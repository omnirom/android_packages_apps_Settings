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
import android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
import com.android.settings.flags.Flags
import com.android.settingslib.R
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen

/**
 * Catalyst screen to display the list of special apps with "Alarms & reminders" permission.
 *
 * This screen is accessible from: Settings > Apps > Special app access > Alarms & reminders
 */
@ProvidePreferenceScreen(AlarmsAndRemindersAppListScreen.KEY)
open class AlarmsAndRemindersAppListScreen : SpecialAccessAppListScreen() {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.alarms_and_reminders_title

    override fun getMetricsCategory() = SettingsEnums.PAGE_UNKNOWN // TODO: correct page id

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (metadata == null) Intent(ACTION_REQUEST_SCHEDULE_EXACT_ALARM) else null

    override val appDetailScreenKey: String
        get() = AlarmsAndRemindersAppDetailScreen.KEY

    override fun appDetailParameters(context: Context, hierarchyType: Boolean) =
        AlarmsAndRemindersAppDetailScreen.parameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_alarms_and_reminders_app_list"
    }
}
