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

import android.Manifest.permission.READ_SYSTEM_PREFERENCES
import android.Manifest.permission.WRITE_SYSTEM_PREFERENCES
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Bundle
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionGranted
import com.android.settings.applications.isPermissionRequested
import com.android.settingslib.metadata.ProvidePreferenceScreen

@ProvidePreferenceScreen(WriteSystemPreferencesAppDetailScreen.KEY, parameterized = true)
open class WriteSystemPreferencesAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.write_system_preferences_page_title

    override val op
        get() = AppOpsManager.OP_WRITE_SYSTEM_PREFERENCES

    override val permission: String?
        get() = PERMISSION

    override val switchPreferenceTitle
        get() = R.string.write_system_preferences_switch_title

    override val footerPreferenceTitle
        get() = R.string.write_system_preferences_footer_description

    // Edge case: what if the app's read permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            writeSystemPreferencesFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN

    companion object {
        const val KEY = "special_access_write_system_preferences_app_detail"
        const val PERMISSION = WRITE_SYSTEM_PREFERENCES

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::writeSystemPreferencesFilter)

        private fun writeSystemPreferencesFilter(
            context: Context,
            appInfo: ApplicationInfo?,
        ): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            return isPermissionGranted(packageInfo, READ_SYSTEM_PREFERENCES) &&
                isPermissionRequested(packageInfo, PERMISSION)
        }
    }
}
