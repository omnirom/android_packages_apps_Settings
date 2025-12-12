/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.accessibility.AccessibilitySettings
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.BaseSupportFragment
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleShortcutPreferenceController
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.detail.a11yservice.ui.A11yServiceScreen
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceEnabled
import com.android.settings.accessibility.shared.LaunchAppInfoPreferenceController
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.EXTRA_BINDING_SCREEN_ARGS

/** Fragment that shows the detail screen of an AccessibilityService */
open class A11yServicePreferenceFragment : BaseSupportFragment() {
    private val tag = A11yServicePreferenceFragment::class.simpleName
    private var disabledStateLogged = false
    private var startTimeMillisForLogging = 0L
    private var serviceInfo: AccessibilityServiceInfo? = null

    private val packageRemovedReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent) {
                val packageName = intent.data?.schemeSpecificPart
                if (serviceInfo?.componentName?.packageName == packageName) {
                    finish()
                }
            }
        }
    }

    private val contentObserver: ContentObserver by lazy {
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ false) }) {
            override fun onChange(selfChange: Boolean) {
                context?.run {
                    if (serviceInfo?.isServiceEnabled(this) == false) {
                        logDisabledState(serviceInfo?.componentName?.packageName)
                    }
                }
            }
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val componentName = getFeatureComponentName()
        serviceInfo =
            AccessibilityRepositoryProvider.get(context).getAccessibilityServiceInfo(componentName)
        if (serviceInfo == null) {
            finish()
            return
        }

        serviceInfo?.let {
            writeConfigDefaultAccessibilityServiceShortcutTargetIfNeeded(it.componentName)
            if (!Flags.catalystA11yServiceDetail()) {
                initializePreferenceControllers(it)
            }
        }
    }

    private fun initializePreferenceControllers(a11yServiceInfo: AccessibilityServiceInfo) {
        use(TopIntroPreferenceController::class.java).initialize(a11yServiceInfo)
        use(AccessibilityServiceIllustrationPreferenceController::class.java)
            .initialize(a11yServiceInfo)
        use(UseServiceTogglePreferenceController::class.java)
            .initialize(a11yServiceInfo, childFragmentManager, metricsCategory)
        use(ShortcutPreferenceController::class.java)
            .initialize(a11yServiceInfo, childFragmentManager, getFeatureName(), metricsCategory)
        use(SettingsPreferenceController::class.java).initialize(a11yServiceInfo)
        use(LaunchAppInfoPreferenceController::class.java).initialize(a11yServiceInfo.componentName)
        use(AccessibilityServiceHtmlFooterPreferenceController::class.java)
            .initialize(a11yServiceInfo)
        use(AccessibilityServiceFooterPreferenceController::class.java).initialize(a11yServiceInfo)
    }

    private fun writeConfigDefaultAccessibilityServiceShortcutTargetIfNeeded(name: ComponentName) {
        // It might be shortened form (with a leading '.'). Need to unflatten back to ComponentName
        // first, or it will encounter errors when getting service from
        // `ACCESSIBILITY_SHORTCUT_TARGET_SERVICE`.
        val defaultService =
            ComponentName.unflattenFromString(
                getString(com.android.internal.R.string.config_defaultAccessibilityService)
            )

        if (defaultService == null || name != defaultService) {
            return
        }

        val targetKey = Settings.Secure.ACCESSIBILITY_SHORTCUT_TARGET_SERVICE
        val targetString = Settings.Secure.getString(requireContext().contentResolver, targetKey)

        // By intentional, we only need to write the config string when the Settings key has never
        // been set (== null). Empty string also means someone already wrote it before, so we need
        // to respect the value.
        if (targetString == null) {
            Settings.Secure.putString(
                requireContext().contentResolver,
                targetKey,
                defaultService.flattenToString(),
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startTimeMillisForLogging =
            arguments?.getLong(AccessibilitySettings.EXTRA_TIME_FOR_LOGGING) ?: 0L
        if (savedInstanceState?.containsKey(KEY_HAS_LOGGED) == true) {
            disabledStateLogged = savedInstanceState.getBoolean(KEY_HAS_LOGGED)
        }

        if (!Flags.catalystA11yServiceDetail()) {
            requireActivity().setTitle(getFeatureName())
        }
        registerPackageRemoveReceiver()

        requireContext()
            .contentResolver
            .registerContentObserver(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_ENABLED),
                false,
                contentObserver,
            )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterPackageRemoveReceiver()
        requireContext().contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (startTimeMillisForLogging > 0) {
            outState.putBoolean(KEY_HAS_LOGGED, disabledStateLogged)
        }
    }

    private fun getFeatureName(): CharSequence {
        return serviceInfo?.getFeatureName(requireContext()) ?: ""
    }

    private fun getFeatureComponentName(): ComponentName {
        return requireNotNull(
            getFragmentArguments()
                .getParcelable(
                    AccessibilitySettings.EXTRA_COMPONENT_NAME,
                    ComponentName::class.java,
                )
        )
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.accessibility_service_detail_screen
    }

    override fun getLogTag(): String? {
        return tag
    }

    override fun getMetricsCategory(): Int {
        return featureFactory.accessibilityPageIdFeatureProvider.getCategory(
            getFeatureComponentName()
        )
    }

    protected fun getShortcutPreferenceController(): ToggleShortcutPreferenceController? {
        return use(ShortcutPreferenceController::class.java)
    }

    private fun registerPackageRemoveReceiver() {
        val filter = IntentFilter(Intent.ACTION_PACKAGE_REMOVED)
        filter.addDataScheme("package")
        context?.registerReceiver(packageRemovedReceiver, filter)
    }

    private fun unregisterPackageRemoveReceiver() {
        context?.unregisterReceiver(packageRemovedReceiver)
    }

    private fun logDisabledState(packageName: String?) {
        if (startTimeMillisForLogging > 0 && !disabledStateLogged) {
            AccessibilityStatsLogUtils.logDisableNonA11yCategoryService(
                packageName,
                SystemClock.elapsedRealtime() - startTimeMillisForLogging,
            )
            disabledStateLogged = true
        }
    }

    override fun getPreferenceScreenBindingKey(context: Context): String? {
        return A11yServiceScreen.KEY
    }

    override fun getPreferenceScreenBindingArgs(context: Context): Bundle? {
        return getFragmentArguments()
    }

    /**
     * Retrieves the fragment arguments.
     *
     * If the arguments contain PreferenceScreenBindingKeyProviderKt#EXTRA_BINDING_SCREEN_ARGS the
     * nested bundle associated with that key will be returned. Otherwise, the original arguments
     * are returned.
     *
     * @return The fragment arguments, or the nested arguments if
     *   PreferenceScreenBindingKeyProviderKt#EXTRA_BINDING_SCREEN_ARGS is present.
     * @throws NullPointerException if the initial arguments are null or if the nested argument are
     *   null
     */
    private fun getFragmentArguments(): Bundle {
        var arguments: Bundle = requireArguments()
        if (arguments.containsKey(EXTRA_BINDING_SCREEN_ARGS)) {
            arguments = requireNotNull(arguments.getBundle(EXTRA_BINDING_SCREEN_ARGS))
        }
        return arguments
    }

    companion object {
        @VisibleForTesting const val KEY_HAS_LOGGED = "has_logged"
    }
}
