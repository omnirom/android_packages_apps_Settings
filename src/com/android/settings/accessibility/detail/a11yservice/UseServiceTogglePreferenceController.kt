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
import android.content.Context
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_TOGGLE
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceWarningRequired
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast
import com.android.settings.accessibility.extensions.useService
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment.Companion.RESULT_STATUS_ALLOW
import com.android.settings.accessibility.shared.dialogs.DisableAccessibilityServiceDialogFragment
import com.android.settings.core.BasePreferenceController
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.accessibility.AccessibilityUtils

class UseServiceTogglePreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey),
    DefaultLifecycleObserver,
    Preference.OnPreferenceChangeListener {

    private val settingsKey = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    private var serviceInfo: AccessibilityServiceInfo? = null
    private var fragmentManager: FragmentManager? = null
    private var sourceMetricsCategory: Int = 0
    private var preference: TwoStatePreference? = null
    private var contentObserver: ContentObserver =
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ false) }) {
            override fun onChange(selfChange: Boolean) {
                preference?.let { updateState(it) }
            }
        }

    fun initialize(
        serviceInfo: AccessibilityServiceInfo,
        fragmentManager: FragmentManager,
        sourceMetricsCategory: Int,
    ) {
        this.serviceInfo = serviceInfo
        this.fragmentManager = fragmentManager
        this.sourceMetricsCategory = sourceMetricsCategory
    }

    override fun getAvailabilityStatus(): Int {
        serviceInfo?.apply {
            if (
                !targetSdkIsAtLeast(Build.VERSION_CODES.R) ||
                    flags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON == 0
            ) {
                return AVAILABLE
            }
        }
        return CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        if (serviceInfo == null) return

        val preference: TwoStatePreference? = screen?.findPreference(preferenceKey)

        preference?.apply {
            title =
                mContext.getString(
                    R.string.accessibility_service_primary_switch_title,
                    serviceInfo!!.getFeatureName(context),
                )
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        // We handled the preference click in #onPreferenceChange
                        return true
                    }
                }
        }
        this.preference = preference
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference is TwoStatePreference) {
            preference.isChecked = isChecked()
        }
    }

    fun setChecked(preference: TwoStatePreference, checked: Boolean) {
        serviceInfo?.let {
            it.useService(mContext, checked)
            preference.isChecked = checked
        }
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (newValue == false) {
            DisableAccessibilityServiceDialogFragment.showDialog(
                fragmentManager = requireNotNull(fragmentManager),
                componentName = requireNotNull(serviceInfo?.componentName),
            )
        } else {
            if (serviceInfo?.isServiceWarningRequired(preference.context) != false) {
                AccessibilityServiceWarningDialogFragment.showDialog(
                    fragmentManager = requireNotNull(fragmentManager),
                    componentName = requireNotNull(serviceInfo).componentName,
                    source = ENABLE_WARNING_FROM_TOGGLE,
                    requestKey = SERVICE_WARNING_DIALOG_REQUEST_CODE,
                )
            } else {
                serviceInfo?.useService(mContext, enabled = true)
            }
        }

        // log here since calling super.onPreferenceTreeClick will be skipped
        featureFactory.metricsFeatureProvider.logClickedPreference(
            preference,
            sourceMetricsCategory,
        )
        return false
    }

    private fun isChecked(): Boolean {
        return serviceInfo?.componentName.let {
            AccessibilityUtils.getEnabledServicesFromSettings(mContext).contains(it)
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(settingsKey),
            /* notifyForDescendants= */ false,
            contentObserver,
        )

        fragmentManager?.setFragmentResultListener(
            /* requestKey= */ SERVICE_WARNING_DIALOG_REQUEST_CODE,
            /* lifecycleOwner= */ owner,
        ) { requestKey, result ->
            if (requestKey == SERVICE_WARNING_DIALOG_REQUEST_CODE) {
                val allow: Boolean =
                    AccessibilityServiceWarningDialogFragment.getResultStatus(result) ==
                        RESULT_STATUS_ALLOW
                serviceInfo?.useService(mContext, enabled = allow)
            }
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        mContext.contentResolver.unregisterContentObserver(contentObserver)
    }

    companion object {
        private const val SERVICE_WARNING_DIALOG_REQUEST_CODE =
            "serviceWarningRequestFromUseServiceToggle"
    }
}
