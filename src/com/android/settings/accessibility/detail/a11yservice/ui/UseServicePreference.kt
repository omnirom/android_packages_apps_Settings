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
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums.ENABLE_WARNING_FROM_TOGGLE
import com.android.settings.accessibility.detail.a11yservice.data.UseServiceDataStore
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceWarningRequired
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast
import com.android.settings.accessibility.shared.dialogs.AccessibilityServiceWarningDialogFragment
import com.android.settings.accessibility.shared.dialogs.DisableAccessibilityServiceDialogFragment
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.MainSwitchPreferenceBinding

class UseServicePreference(
    private val context: Context,
    private val serviceInfo: AccessibilityServiceInfo,
    private val sourceMetricsCategory: Int,
) :
    BooleanValuePreference,
    MainSwitchPreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceLifecycleProvider {

    override val key: String
        get() = KEY

    private val dataStore by lazy { UseServiceDataStore(context, serviceInfo) }

    override fun storage(context: Context): KeyValueStore {
        return dataStore
    }

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) =
        when (value) {
            true ->
                if (serviceInfo.isServiceWarningRequired(context)) ReadWritePermit.DISALLOW
                else ReadWritePermit.ALLOW

            else -> {
                // We show a warning dialog before the user wants to turn the service off.
                ReadWritePermit.DISALLOW
            }
        }

    override fun isAvailable(context: Context): Boolean {
        return serviceInfo.run {
            !targetSdkIsAtLeast(Build.VERSION_CODES.R) ||
                flags and AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON == 0
        }
    }

    override fun getTitle(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_service_primary_switch_title,
            serviceInfo.getFeatureName(context),
        )
    }

    override fun createWidget(context: Context): Preference {
        return super.createWidget(context).apply {
            onPreferenceClickListener =
                Preference.OnPreferenceClickListener { _ ->
                    // We handled the preference click in #onPreferenceChange
                    true
                }
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        context.childFragmentManager.setFragmentResultListener(
            SERVICE_WARNING_DIALOG_REQUEST_CODE,
            context.lifecycleOwner,
        ) { requestKey, result ->
            if (requestKey == SERVICE_WARNING_DIALOG_REQUEST_CODE) {
                val allow: Boolean =
                    AccessibilityServiceWarningDialogFragment.getResultStatus(result) ==
                        AccessibilityServiceWarningDialogFragment.RESULT_STATUS_ALLOW
                storage(context).setBoolean(key, allow)
            }
        }

        context.findPreference<Preference>(key)?.onPreferenceChangeListener =
            Preference.OnPreferenceChangeListener { preference, newValue ->
                if (newValue == false) {
                    DisableAccessibilityServiceDialogFragment.showDialog(
                        fragmentManager = context.childFragmentManager,
                        componentName = serviceInfo.componentName,
                    )
                } else {
                    if (serviceInfo.isServiceWarningRequired(preference.context)) {
                        AccessibilityServiceWarningDialogFragment.showDialog(
                            fragmentManager = context.childFragmentManager,
                            componentName = serviceInfo.componentName,
                            source = ENABLE_WARNING_FROM_TOGGLE,
                            requestKey = SERVICE_WARNING_DIALOG_REQUEST_CODE,
                        )
                    } else {
                        storage(preference.context).setBoolean(key, true)
                    }
                }
                // log here since calling super.onPreferenceTreeClick will be skipped
                featureFactory.metricsFeatureProvider.logClickedPreference(
                    preference,
                    sourceMetricsCategory,
                )

                false
            }
    }

    companion object {
        const val KEY = "use_service"
        private const val SERVICE_WARNING_DIALOG_REQUEST_CODE =
            "serviceWarningRequestFromUseServiceToggle"
    }
}
