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

package com.android.settings.spa.app.catalyst

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.applications.appinfo.AppInfoDashboardFragment
import com.android.settings.applications.packageName
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen
import com.android.settings.applications.specialaccess.DisplayOverOtherAppsAppDetailScreen.Companion.displayOverOtherAppsFilter
import com.android.settings.applications.specialaccess.SpecialAccessAppDetailScreen.Companion.hasSpecialAccessPermission
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen
import com.android.settings.applications.specialaccess.pictureinpicture.PictureInPictureAppDetailScreen.Companion.pictureInPictureFilter
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ProvidePreferenceScreen(AppInfoScreen.KEY, parameterized = true)
open class AppInfoScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceTitleProvider {
    private val packageName = arguments.packageName

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    override val key: String
        get() = KEY

    override fun getMetricsCategory() = SettingsEnums.APPLICATIONS_INSTALLED_APP_DETAILS

    override val screenTitle: Int
        get() = R.string.application_info_label

    override fun getTitle(context: Context): CharSequence =
        appInfo.loadLabel(context.packageManager)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
            data = "package:${appInfo.packageName}".toUri()
            // TODO: create highlight intent for SpaActivity.
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = AppInfoDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory("advanced_app_info", R.string.advanced_apps) += {
                arguments.putString("source", SOURCE)
                if (hasSpecialAccessPermission(context, appInfo, ::pictureInPictureFilter)) {
                    +(PictureInPictureAppDetailScreen.KEY args arguments)
                }
                if (hasSpecialAccessPermission(context, appInfo, ::displayOverOtherAppsFilter)) {
                    +(DisplayOverOtherAppsAppDetailScreen.KEY args arguments)
                }
            }
        }

    companion object {
        const val KEY = "installed_app_detail_settings_screen"
        const val SOURCE = "appinfo"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            AppListRepositoryImpl(context).loadAndFilterApps(context.userId, true).forEach {
                emit(Bundle(1).apply { putString("pkg", it.packageName) })
            }
        }
    }
}
