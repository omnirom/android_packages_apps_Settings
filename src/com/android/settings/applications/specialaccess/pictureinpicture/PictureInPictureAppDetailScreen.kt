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

package com.android.settings.applications.specialaccess.pictureinpicture

import android.app.ActivityManager
import android.app.AppOpsManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings.ACTION_PICTURE_IN_PICTURE_SETTINGS
import androidx.core.net.toUri
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.applications.CatalystAppListFragment.Companion.DEFAULT_SHOW_SYSTEM
import com.android.settings.applications.getPackageInfoWithActivities
import com.android.settings.applications.specialaccess.SpecialAccessAppDetailScreen
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.utils.highlightPreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

@ProvidePreferenceScreen(PictureInPictureAppDetailScreen.KEY, parameterized = true)
open class PictureInPictureAppDetailScreen(context: Context, arguments: Bundle) :
    SpecialAccessAppDetailScreen(context, arguments) {

    override val key
        get() = KEY

    override val bindingKey
        get() = "$KEY-$packageName"

    override val screenTitle
        get() = R.string.picture_in_picture_app_detail_title

    override val op
        get() = AppOpsManager.OP_PICTURE_IN_PICTURE

    override val permission: String?
        get() = null

    override val setModeByUid: Boolean?
        get() = false // set op mode by package

    override val switchPreferenceTitle
        get() = R.string.picture_in_picture_app_detail_switch

    override val footerPreferenceTitle
        get() = R.string.picture_in_picture_app_detail_summary

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun isFlagEnabled(context: Context) = context.isPictureInPictureEnabled()

    override fun isAvailable(context: Context) =
        super.isAvailable(context) && pictureInPictureFilter(context, packageInfo?.applicationInfo)

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_MANAGE_PICTURE_IN_PICTURE_DETAIL

    override fun getAccessChangeActionMetrics(allowed: Boolean) =
        when (allowed) {
            true -> SettingsEnums.APP_PICTURE_IN_PICTURE_ALLOW
            else -> SettingsEnums.APP_PICTURE_IN_PICTURE_DENY
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_PICTURE_IN_PICTURE_SETTINGS).apply {
            data = "package:$packageName".toUri()
            highlightPreference(arguments, metadata?.bindingKey)
        }

    companion object {
        const val KEY = "special_access_picture_in_picture_app_detail"

        @JvmStatic fun parameters(context: Context) = parameters(context, DEFAULT_SHOW_SYSTEM)

        fun parameters(context: Context, showSystemApp: Boolean) =
            parameters(context, showSystemApp, ::pictureInPictureFilter)

        fun pictureInPictureFilter(context: Context, appInfo: ApplicationInfo?): Boolean {
            if (appInfo == null) return false
            val packageInfo =
                context.getPackageInfoWithActivities(appInfo.packageName) ?: return false

            return packageInfo.activities?.any(ActivityInfo::supportsPictureInPicture) == true
        }
    }
}

class PictureInPictureAppDetailActivity :
    CatalystSettingsActivity(PictureInPictureAppDetailScreen.KEY)

internal fun Context.isPictureInPictureEnabled() =
    packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE) &&
        !ActivityManager.isLowRamDeviceStatic()
