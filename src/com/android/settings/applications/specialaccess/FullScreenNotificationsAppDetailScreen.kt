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

import android.Manifest.permission.USE_FULL_SCREEN_INTENT
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.core.net.toUri
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithPermissions
import com.android.settings.applications.isPermissionRequested
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

/**
 * The app detail catalyst screen for "Full-screen notifications" special app access.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Full-screen
 * notifications > [app name]
 */
@ProvidePreferenceScreen(FullScreenNotificationsAppDetailScreen.KEY, parameterized = true)
open class FullScreenNotificationsAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle: Int
        get() = R.string.full_screen_intent_title

    override val op
        get() = AppOpsManager.OP_USE_FULL_SCREEN_INTENT

    override val permission: String?
        get() = PERMISSION

    override val setModeByUid: Boolean?
        get() = true

    override val switchPreferenceTitle
        get() = R.string.permit_full_screen_intent

    override val footerPreferenceTitle
        get() = R.string.footer_description_full_screen_intent

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    // Edge case: what if the app's read permission is revoked/granted
    override fun isAvailable(context: Context) =
        super.isAvailable(context) && fullScreenIntentFilter(context, packageInfo?.applicationInfo)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENT").apply {
            // TODO: b/444137482 - Launch Catalyst page when UI is ready
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_full_screen_notifications_app_detail"
        const val PERMISSION = USE_FULL_SCREEN_INTENT

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::fullScreenIntentFilter)

        private fun fullScreenIntentFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithPermissions(appInfo.packageName) ?: return false

            return isPermissionRequested(packageInfo, PERMISSION)
        }
    }
}
