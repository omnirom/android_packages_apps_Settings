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

package com.android.settings.sound

import android.app.settings.SettingsEnums.ACTION_SHOW_MEDIA_ON_LOCK_SCREEN
import android.content.Context
import android.provider.Settings.Secure.MEDIA_CONTROLS_LOCK_SCREEN
import com.android.settings.R
import com.android.settings.contract.KEY_SHOW_MEDIA_ON_LOCK_SCREEN
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class MediaControlsLockscreenSwitchPreference :
    SwitchPreference(
        KEY,
        R.string.media_controls_lockscreen_title,
        R.string.media_controls_lockscreen_description,
    ),
    PreferenceActionMetricsProvider {

    override val preferenceActionMetrics: Int
        get() = ACTION_SHOW_MEDIA_ON_LOCK_SCREEN

    override fun tags(context: Context) = arrayOf(KEY_SHOW_MEDIA_ON_LOCK_SCREEN)

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun storage(context: Context) = context.dataStore

    companion object {
        const val KEY = MEDIA_CONTROLS_LOCK_SCREEN

        private val Context.dataStore: KeyValueStore
            get() = SettingsSecureStore.get(this).apply { setDefaultValue(KEY, true) }
    }
}
// LINT.ThenChange(MediaControlsLockScreenPreferenceController.java)
