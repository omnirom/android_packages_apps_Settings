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

import android.Manifest
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

@ProvidePreferenceScreen(ManageWriteSettingsAppDetailScreen.KEY, parameterized = true)
open class ManageWriteSettingsAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.write_system_settings

    override val op
        get() = AppOpsManager.OP_WRITE_SETTINGS

    override val permission
        get() = PERMISSION

    override val switchPreferenceTitle
        get() = R.string.permit_write_settings

    override val footerPreferenceTitle
        get() = R.string.write_settings_description

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            manageWriteSettingsFilter(context, packageInfo?.applicationInfo)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_manage_write_settings_app_detail"
        const val PERMISSION = Manifest.permission.WRITE_SETTINGS

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::manageWriteSettingsFilter)

        private fun manageWriteSettingsFilter(
            context: Context,
            appInfo: ApplicationInfo?,
        ): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false
            return isPermissionRequested(packageInfo, PERMISSION)
        }
    }
}
