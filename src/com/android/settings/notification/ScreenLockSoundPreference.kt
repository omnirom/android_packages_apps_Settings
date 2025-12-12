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
package com.android.settings.notification

import android.app.settings.SettingsEnums.ACTION_SCREEN_LOCKING_SOUND
import android.content.Context
import android.provider.Settings.System.LOCKSCREEN_SOUNDS_ENABLED
import com.android.settings.R
import com.android.settings.contract.KEY_SCREEN_LOCKING_SOUND
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class ScreenLockSoundPreference :
    SwitchPreference(KEY, R.string.screen_locking_sounds_title),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {
    override val preferenceActionMetrics: Int
        get() = ACTION_SCREEN_LOCKING_SOUND

    override fun tags(context: Context) = arrayOf(KEY_SCREEN_LOCKING_SOUND)

    override fun storage(context: Context) = context.dataStore

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_screen_locking_sounds)

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = LOCKSCREEN_SOUNDS_ENABLED

        private val Context.dataStore: KeyValueStore
            get() = SettingsSystemStore.get(this).apply { setDefaultValue(KEY, true) }
    }
}
// LINT.ThenChange(ScreenLockSoundPreferenceController.java)
