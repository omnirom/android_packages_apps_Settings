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

package com.android.settings.accessibility.hearingdevices.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.util.FeatureFlagUtils
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityAudioRoutingFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata

class AudioRoutingPreference : PreferenceMetadata, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.bluetooth_audio_routing_title

    override val summary: Int
        get() = R.string.bluetooth_audio_routing_summary

    override fun intent(context: Context): Intent? =
        SubSettingLauncher(context)
            .setDestination(AccessibilityAudioRoutingFragment::class.java.name)
            .setSourceMetricsCategory(SettingsEnums.ACCESSIBILITY_HEARING_AID_SETTINGS)
            .toIntent()

    override fun isAvailable(context: Context): Boolean =
        FeatureFlagUtils.isEnabled(context, FeatureFlagUtils.SETTINGS_AUDIO_ROUTING)

    companion object {
        const val KEY = "audio_routing"
    }
}
