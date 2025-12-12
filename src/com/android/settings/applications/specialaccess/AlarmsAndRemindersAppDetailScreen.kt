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

import android.Manifest.permission.SCHEDULE_EXACT_ALARM
import android.Manifest.permission.USE_EXACT_ALARM
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.compat.CompatChanges
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.os.PowerExemptionManager
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import androidx.core.net.toUri
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.spaprivileged.model.app.userHandle

/**
 * The app detail catalyst screen for "Alarms & reminders" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Alarms &
 * reminders > [app name]
 */
@ProvidePreferenceScreen(AlarmsAndRemindersAppDetailScreen.KEY, parameterized = true)
open class AlarmsAndRemindersAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.alarms_and_reminders_title

    override val op
        get() = AppOpsManager.OP_SCHEDULE_EXACT_ALARM

    override val permission: String?
        get() = PERMISSION

    override val setModeByUid: Boolean?
        get() = true

    override val switchPreferenceTitle
        get() = R.string.alarms_and_reminders_switch_title

    override val footerPreferenceTitle
        get() = R.string.alarms_and_reminders_footer_title

    // Edge case: what if the app's read permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) &&
            alarmsAndRemindersFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.ALARMS_AND_REMINDERS

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_alarms_and_reminders_app_detail"
        const val BROADER_PERMISSION = USE_EXACT_ALARM
        const val PERMISSION = SCHEDULE_EXACT_ALARM

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::alarmsAndRemindersFilter)

        private fun alarmsAndRemindersFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            val hasRequestScheduleExactAlarmPermission =
                isPermissionRequested(packageInfo, PERMISSION) &&
                    CompatChanges.isChangeEnabled(
                        AlarmManager.REQUIRE_EXACT_ALARM_PERMISSION,
                        appInfo.packageName,
                        appInfo.userHandle,
                    )
            val hasRequestUseExactAlarm =
                isPermissionRequested(packageInfo, BROADER_PERMISSION) &&
                    CompatChanges.isChangeEnabled(
                        AlarmManager.ENABLE_USE_EXACT_ALARM,
                        appInfo.packageName,
                        appInfo.userHandle,
                    )
            val isPowerAllowListed =
                context
                    .getSystemService(PowerExemptionManager::class.java)
                    ?.isAllowListed(appInfo.packageName, true) ?: false
            val isTrumped =
                hasRequestScheduleExactAlarmPermission &&
                    (hasRequestUseExactAlarm || isPowerAllowListed)

            return hasRequestScheduleExactAlarmPermission && !isTrumped
        }
    }
}
