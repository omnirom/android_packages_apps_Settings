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
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import kotlinx.coroutines.flow.Flow

@ProvidePreferenceScreen(ManageWriteSettingsAppListScreen.KEY)
open class ManageWriteSettingsAppListScreen : SpecialAccessAppListScreen() {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.write_system_settings

    override val keywords: Int
        get() = R.string.keywords_write_settings

    override fun getMetricsCategory() = SettingsEnums.SCREEN_UNKNOWN

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_SCREEN)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (metadata == null) Intent(ACTION_MANAGE_WRITE_SETTINGS) else null

    override val appDetailScreenKey
        get() = ManageWriteSettingsAppDetailScreen.KEY

    override fun appDetailParameters(context: Context, hierarchyType: Boolean): Flow<Bundle> =
        ManageWriteSettingsAppDetailScreen.parameters(context, hierarchyType)

    companion object {
        const val KEY = "special_access_manage_write_settings_app_list"
    }
}
