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

package com.android.settings.accessibility.detail.a11yservice.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_SHORTCUT_TOGGLE
import com.android.settings.accessibility.ShortcutPreference
import com.android.settings.accessibility.detail.a11yservice.data.ShortcutDataStore
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isServiceWarningRequired
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast
import com.android.settings.accessibility.shared.data.AccessibilityShortcutDataStore
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.RESULT_STATUS_ALLOW
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.accessibility.shared.ui.ShortcutFeatureNameProvider
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceTitleProvider

/**
 * A preference that allows users to configure the accessibility shortcut for an accessibility
 * service.
 *
 * This preference displays the service name and allows users to enable or disable the shortcut. If
 * the service requires a warning dialog, it will be shown before enabling the shortcut.
 */
class A11yServiceShortcutPreference(
    context: Context,
    private val serviceInfo: AccessibilityServiceInfo,
    metricCategory: Int,
) :
    AccessibilityShortcutPreference(
        context = context,
        key = KEY,
        componentName = serviceInfo.componentName,
        metricsCategory = metricCategory,
    ),
    PreferenceTitleProvider,
    ShortcutFeatureNameProvider {

    override val dataStore: AccessibilityShortcutDataStore by lazy {
        ShortcutDataStore(context, serviceInfo)
    }

    override fun getTitle(context: Context): CharSequence? {
        return context.getString(R.string.accessibility_shortcut_title, getFeatureName(context))
    }

    override fun getFeatureName(context: Context): CharSequence {
        return serviceInfo.getFeatureName(context)
    }

    override fun getSettingsEditable(context: Context): Boolean {
        return serviceInfo.targetSdkIsAtLeast(Build.VERSION_CODES.R)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        context.childFragmentManager.setFragmentResultListener(
            /* requestKey= */ SERVICE_WARNING_DIALOG_REQUEST_CODE,
            /* lifecycleOwner= */ context.lifecycleOwner,
        ) { requestKey, result ->
            if (requestKey == SERVICE_WARNING_DIALOG_REQUEST_CODE) {
                val shortcutPreference =
                    context.findPreference<ShortcutPreference>(key)
                        ?: return@setFragmentResultListener
                val allow: Boolean =
                    AccessibilityServiceWarningDialogFragment.getResultStatus(result) ==
                        RESULT_STATUS_ALLOW
                val dialogEnum =
                    AccessibilityServiceWarningDialogFragment.getResultDialogContext(result)
                handleServiceWarningResponse(context, shortcutPreference, allow, dialogEnum)
            }
        }
    }

    override fun onSettingsClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        val serviceWarningRequired = serviceInfo.isServiceWarningRequired(preference.context)
        if (serviceWarningRequired) {
            showServiceWarning(context.childFragmentManager, ENABLE_WARNING_FROM_SHORTCUT)
        } else {
            handleServiceWarningResponse(
                context = context,
                shortcutPreference = preference,
                allow = true,
                dialogEnum = ENABLE_WARNING_FROM_SHORTCUT,
            )
        }
        // log here since calling super.onPreferenceTreeClick will be skipped
        featureFactory.metricsFeatureProvider.logClickedPreference(preference, metricsCategory)
    }

    override fun onToggleClicked(
        preference: ShortcutPreference,
        context: PreferenceLifecycleContext,
    ) {
        val isChecked = preference.isChecked
        if (!isChecked) {
            dataStore.setBoolean(key, false)
        } else {
            val serviceWarningRequired = serviceInfo.isServiceWarningRequired(preference.context)
            if (serviceWarningRequired) {
                showServiceWarning(
                    context.childFragmentManager,
                    ENABLE_WARNING_FROM_SHORTCUT_TOGGLE,
                )
            } else {
                handleServiceWarningResponse(
                    context = context,
                    shortcutPreference = preference,
                    allow = true,
                    dialogEnum = ENABLE_WARNING_FROM_SHORTCUT_TOGGLE,
                )
            }
        }
    }

    private fun showServiceWarning(fragmentManager: FragmentManager, dialogEnum: Int) {
        AccessibilityServiceWarningDialogFragment.showDialog(
            fragmentManager = fragmentManager,
            componentName = serviceInfo.componentName,
            source = dialogEnum,
            requestKey = SERVICE_WARNING_DIALOG_REQUEST_CODE,
        )
    }

    private fun handleServiceWarningResponse(
        context: PreferenceLifecycleContext,
        shortcutPreference: ShortcutPreference,
        allow: Boolean,
        dialogEnum: Int,
    ) {
        if (allow) {
            when (dialogEnum) {
                ENABLE_WARNING_FROM_SHORTCUT -> {
                    showEditShortcutsScreen(
                        context = shortcutPreference.context,
                        screenTitle = shortcutPreference.title ?: "",
                    )
                }

                ENABLE_WARNING_FROM_SHORTCUT_TOGGLE -> {
                    dataStore.setBoolean(key, true)
                    showShortcutsTutorial(
                        context,
                        context.childFragmentManager,
                        dataStore.getUserShortcutTypes(),
                        shortcutPreference.context.isInSetupWizard(),
                    )
                }
            }
        } else {
            dataStore.setBoolean(key, false)
            // Since the dataStore aren't updated,
            // we need to notify the preference change so that the toggle would accurately
            // reflect the state.
            context.notifyPreferenceChange(key)
        }
    }

    companion object {
        const val KEY = "service_shortcut"
        private const val SERVICE_WARNING_DIALOG_REQUEST_CODE =
            "serviceWarningRequestFromA11yServiceShortcut"
    }
}
