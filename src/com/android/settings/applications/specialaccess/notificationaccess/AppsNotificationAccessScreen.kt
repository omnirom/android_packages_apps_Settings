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

package com.android.settings.applications.specialaccess.notificationaccess

import android.Manifest
import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Bundle
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import com.android.settings.R
import com.android.settings.Settings.NotificationAccessSettingsActivity
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** "Apps" -> "Special app access" -> "Notification read, reply & control" */
@ProvidePreferenceScreen(AppsNotificationAccessScreen.KEY)
open class AppsNotificationAccessScreen : PreferenceScreenMixin {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.manage_notification_access_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.NOTIFICATION_ACCESS

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.catalystAppList()

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, NotificationAccessSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val services = loadNotificationListenerServices(context)
            for (service in services) {
                val arguments =
                    Bundle(1).apply {
                        putString("app", service.packageName)
                        putString("serviceName", service.name)
                    }
                +(AppInfoNotificationAccessScreen.KEY args arguments)
            }
        }

    companion object {
        const val KEY = "notification_access"

        // TODO: b/416239475 - unify this code with ServiceListing.java
        @JvmStatic
        private fun loadEnabledServices(context: Context, setting: String): List<ComponentName> {
            val enabledServices = mutableListOf<ComponentName>()
            enabledServices.clear()

            val contentResolver = context.getContentResolver()
            val flat = Settings.Secure.getString(contentResolver, setting)
            if (!flat.isNullOrEmpty()) {
                val names = flat.split(":")
                for (name in names) {
                    ComponentName.unflattenFromString(name)?.let { enabledServices.add(it) }
                }
            }

            return enabledServices
        }

        /**
         * Loads services matching the given intent and permission, filtered to the apps that
         * request the service via a setting.
         *
         * This is based on com.android.settingslib.applications.ServiceListing#load().
         *
         * @param context The context to use.
         * @param intentAction The intent action to match.
         * @param permission The permission to require.
         * @param setting The setting to use to store enabled services.
         * @return A list of services that match the given intent and permission.
         */
        // TODO: b/416239475 - unify this code with ServiceListing.java
        @JvmStatic
        private fun loadServices(
            context: Context,
            intentAction: String,
            permission: String,
            setting: String,
        ): List<ServiceInfo> {
            val enabledServices = loadEnabledServices(context, setting)
            val services = mutableListOf<ServiceInfo>()
            val user = ActivityManager.getCurrentUser()

            var flags = PackageManager.GET_SERVICES or PackageManager.GET_META_DATA

            // Add requesting apps, with full validation
            val installedServices =
                context.packageManager.queryIntentServicesAsUser(Intent(intentAction), flags, user)
            for (resolveInfo in installedServices) {
                val info = resolveInfo.serviceInfo

                if (info.componentName !in enabledServices) {
                    if (permission != info.permission) {
                        Log.w(
                            TAG_DEVICE_STATE_SCREEN,
                            "Skipping service ${info.packageName}/${info.name}: " +
                                "it does not require the permission $permission",
                        )
                        continue
                    }
                    services.add(info)
                }
            }

            // Add all apps with access, in case prior approval was granted without full validation
            for (componentName in enabledServices) {
                val enabledServicesResolveInfo =
                    context.packageManager.queryIntentServicesAsUser(
                        Intent().setComponent(componentName),
                        flags,
                        user,
                    )
                for (resolveInfo in enabledServicesResolveInfo) {
                    val info = resolveInfo.serviceInfo
                    services.add(info)
                }
            }

            return services
        }

        @JvmStatic
        fun loadNotificationListenerServices(context: Context): List<ServiceInfo> {
            return loadServices(
                context,
                NotificationListenerService.SERVICE_INTERFACE,
                Manifest.permission.BIND_NOTIFICATION_LISTENER_SERVICE,
                Settings.Secure.ENABLED_NOTIFICATION_LISTENERS,
            )
        }
    }
}
