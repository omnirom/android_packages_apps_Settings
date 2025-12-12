/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums.ACTION_DIAL_PAD_TONE
import android.content.Context
import android.provider.Settings.System.DTMF_TONE_WHEN_DIALING
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.contract.KEY_DIAL_PAD_TONE
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class DialPadTonePreference :
    SwitchPreference(DTMF_TONE_WHEN_DIALING, R.string.dial_pad_tones_title),
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {
    override val preferenceActionMetrics: Int
        get() = ACTION_DIAL_PAD_TONE

    override fun tags(context: Context) = arrayOf(KEY_DIAL_PAD_TONE)

    override fun storage(context: Context) = SettingsSystemStore.get(context)

    override fun isAvailable(context: Context) = Utils.isVoiceCapable(context)

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY
}
// LINT.ThenChange(DialPadTonePreferenceController.java)
