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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

/**
 * Catalyst screen to display the list of special apps with "Full-screen notifications" permission.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Full-screen notifications
 */
@ProvidePreferenceScreen(FullScreenNotificationsAppListScreen.KEY)
open class FullScreenNotificationsAppListScreen : SpecialAccessAppListScreen() {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.full_screen_intent_title

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent =
        Intent().apply {
            // TODO: b/444137482 - Launch Catalyst page when UI is ready
            component =
                ComponentName("com.android.settings", "com.android.settings.ManageFullScreenIntent")
        }

    override val appDetailScreenKey: String
        get() = FullScreenNotificationsAppDetailScreen.KEY

    override fun appDetailParameters(context: Context, hierarchyType: Boolean) =
        FullScreenNotificationsAppDetailScreen.parameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_full_screen_notifications_app_list"
    }
}
