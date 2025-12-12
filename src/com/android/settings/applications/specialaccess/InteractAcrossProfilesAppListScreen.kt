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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.content.pm.CrossProfileApps
import android.os.Bundle
import android.os.UserManager
import android.provider.Settings.ACTION_MANAGE_CROSS_PROFILE_ACCESS
import com.android.settings.R
import com.android.settings.applications.specialaccess.interactacrossprofiles.InteractAcrossProfilesSettings
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(InteractAcrossProfilesAppListScreen.KEY)
open class InteractAcrossProfilesAppListScreen : PreferenceScreenMixin {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.interact_across_profiles_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = false

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        // TODO: highlight the app from the metadata when highlighting parameterized screens is
        // supported.
        Intent(ACTION_MANAGE_CROSS_PROFILE_ACCESS)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val packageManager = context.packageManager
            val userManager = context.getSystemService(UserManager::class.java)
            val crossProfileApps = context.getSystemService(CrossProfileApps::class.java)

            InteractAcrossProfilesSettings.collectConfigurableApps(
                    packageManager,
                    userManager,
                    crossProfileApps,
                )
                .forEach { app_user ->
                    val arguments = Bundle(1).apply { putString("app", app_user.first.packageName) }
                    +(InteractAcrossProfilesAppDetailScreen.KEY args arguments)
                }
        }

    companion object {
        const val KEY = "special_access_interact_across_profiles_app_list"
    }
}
