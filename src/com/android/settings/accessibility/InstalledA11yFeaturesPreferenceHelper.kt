/*
 * Copyright (C) 2022 The Android Open Source Project
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
package com.android.settings.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.permission.flags.Flags
import com.android.internal.R
import com.android.settingslib.RestrictedLockUtilsInternal
import com.android.settingslib.RestrictedPreference
import com.android.settingslib.accessibility.AccessibilityUtils
import java.util.stream.Collectors

/** This class helps setup Preferences for installed accessibility features. */
class InstalledA11yFeaturesPreferenceHelper(private val context: Context) {
    private val devicePolicyManager: DevicePolicyManager? =
        context.getSystemService(DevicePolicyManager::class.java)
    private val appOpsManager: AppOpsManager? = context.getSystemService(AppOpsManager::class.java)

    /**
     * Creates the list of [RestrictedPreference] with the installedServices arguments.
     *
     * @param installedServices The list of [AccessibilityServiceInfo]s of the installed
     *   accessibility services
     * @return The list of [RestrictedPreference]
     */
    fun createAccessibilityServicePreferenceList(
        installedServices: List<AccessibilityServiceInfo>
    ): List<RestrictedPreference> {
        val enabledServices = AccessibilityUtils.getEnabledServicesFromSettings(context)
        val permittedServices =
            devicePolicyManager!!.getPermittedAccessibilityServices(UserHandle.myUserId())

        return installedServices
            .stream()
            .map { info: AccessibilityServiceInfo ->
                val resolveInfo = info.resolveInfo
                val packageName = resolveInfo.serviceInfo.packageName
                val componentName = ComponentName(packageName, resolveInfo.serviceInfo.name)
                val serviceEnabled = enabledServices.contains(componentName)
                val preference: RestrictedPreference =
                    AccessibilityServicePreference(
                        context,
                        packageName,
                        resolveInfo.serviceInfo.applicationInfo.uid,
                        info,
                        serviceEnabled,
                    )
                setRestrictedPreferenceEnabled(preference, permittedServices, serviceEnabled)
                preference
            }
            .collect(Collectors.toList())
    }

    /**
     * Creates the list of [AccessibilityActivityPreference] with the installedShortcuts arguments.
     *
     * @param installedShortcuts The list of [AccessibilityShortcutInfo]s of the installed
     *   accessibility shortcuts
     * @return The list of [AccessibilityActivityPreference]
     */
    fun createAccessibilityActivityPreferenceList(
        installedShortcuts: List<AccessibilityShortcutInfo>
    ): List<AccessibilityActivityPreference> {
        return installedShortcuts
            .stream()
            .map { info: AccessibilityShortcutInfo ->
                val preference = AccessibilityActivityPreference(context, info)
                // Accessibility Activities do not have elevated privileges so restricting
                // them based on ECM or device admin does not give any value.
                preference.isEnabled = true
                preference
            }
            .collect(Collectors.toList())
    }

    private fun setRestrictedPreferenceEnabled(
        preference: RestrictedPreference,
        permittedServices: MutableList<String?>?,
        serviceEnabled: Boolean,
    ) {
        // permittedServices null means all accessibility services are allowed.
        var serviceAllowed =
            permittedServices == null || permittedServices.contains(preference.getPackageName())

        if (
            Flags.enhancedConfirmationModeApisEnabled() &&
                android.security.Flags.extendEcmToAllSettings()
        ) {
            preference.checkEcmRestrictionAndSetDisabled(
                AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE,
                preference.getPackageName(),
                serviceEnabled,
            )
            if (preference.isDisabledByEcm()) {
                serviceAllowed = false
            }

            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true)
            } else {
                // Disable accessibility service that are not permitted.
                val admin =
                    RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                        context,
                        preference.getPackageName(),
                        UserHandle.myUserId(),
                    )

                if (admin != null) {
                    preference.setDisabledByAdmin(admin)
                } else if (!preference.isDisabledByEcm()) {
                    preference.setEnabled(false)
                }
            }
        } else {
            var appOpsAllowed: Boolean
            if (serviceAllowed) {
                try {
                    val mode: Int =
                        appOpsManager!!.noteOpNoThrow(
                            AppOpsManager.OP_ACCESS_RESTRICTED_SETTINGS,
                            preference.getUid(),
                            preference.getPackageName(),
                        )
                    val ecmEnabled: Boolean =
                        context
                            .getResources()
                            .getBoolean(R.bool.config_enhancedConfirmationModeEnabled)
                    appOpsAllowed =
                        !ecmEnabled ||
                            mode == AppOpsManager.MODE_ALLOWED ||
                            mode == AppOpsManager.MODE_DEFAULT
                    serviceAllowed = appOpsAllowed
                } catch (e: Exception) {
                    // Allow service in case if app ops is not available in testing.
                    appOpsAllowed = true
                }
            } else {
                appOpsAllowed = false
            }
            if (serviceAllowed || serviceEnabled) {
                preference.setEnabled(true)
            } else {
                // Disable accessibility service that are not permitted.
                val admin =
                    RestrictedLockUtilsInternal.checkIfAccessibilityServiceDisallowed(
                        context,
                        preference.getPackageName(),
                        UserHandle.myUserId(),
                    )

                if (admin != null) {
                    preference.setDisabledByAdmin(admin)
                } else if (!appOpsAllowed) {
                    preference.setDisabledByAppOps(true)
                } else {
                    preference.setEnabled(false)
                }
            }
        }
    }
}
