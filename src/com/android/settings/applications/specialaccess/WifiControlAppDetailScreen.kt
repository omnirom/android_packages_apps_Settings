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

package com.android.settings.applications.specialaccess

import android.Manifest.permission.CHANGE_WIFI_STATE
import android.Manifest.permission.NETWORK_SETTINGS
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.Settings.ChangeWifiStateActivity
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

/**
 * The app detail catalyst screen for "Wi-Fi control" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Wi-Fi control > [app name]
 */
@ProvidePreferenceScreen(WifiControlAppDetailScreen.KEY, parameterized = true)
open class WifiControlAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.change_wifi_state_title

    override val op
        get() = AppOpsManager.OP_CHANGE_WIFI_STATE

    override val permission: String?
        get() = PERMISSION

    override val switchPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_switch

    override val footerPreferenceTitle
        get() = R.string.change_wifi_state_app_detail_summary

    // Edge case: what if the app's change wifi state permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) && wifiStateControlFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.CONFIGURE_WIFI

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getAccessChangeActionMetrics(allowed: Boolean): Int =
        when (allowed) {
            true -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_ALLOW
            else -> SettingsEnums.APP_SPECIAL_PERMISSION_SETTINGS_CHANGE_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ChangeWifiStateActivity::class.java, metadata?.key).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_wifi_control_app_detail"
        const val BROADER_PERMISSION = NETWORK_SETTINGS
        const val PERMISSION = CHANGE_WIFI_STATE

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::wifiStateControlFilter)

        private fun wifiStateControlFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            val isChangeable =
                isPermissionRequested(packageInfo, PERMISSION) &&
                    !isPermissionRequested(packageInfo, BROADER_PERMISSION)

            return isChangeable
        }
    }
}
