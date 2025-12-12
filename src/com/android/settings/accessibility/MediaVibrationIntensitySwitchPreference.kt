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
package com.android.settings.accessibility

import android.content.Context
import android.os.VibrationAttributes
import android.provider.Settings.System.MEDIA_VIBRATION_INTENSITY
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceAvailabilityProvider

/** Accessibility settings for media vibration, as a switch toggle. */
// LINT.IfChange
class MediaVibrationIntensitySwitchPreference(
    context: Context,
    key: String,
    mainSwitchPreferenceKey: String,
) :
    VibrationIntensitySwitchPreference(
        context = context,
        key = key,
        settingsProviderKey = MEDIA_VIBRATION_INTENSITY,
        mainSwitchPreferenceKey = mainSwitchPreferenceKey,
        vibrationUsage = VibrationAttributes.USAGE_MEDIA,
        title = R.string.accessibility_media_vibration_title,
    ),
    PreferenceAvailabilityProvider {
    override val keywords: Int
        get() = R.string.keywords_media_vibration

    override fun isAvailable(context: Context) = context.isMediaVibrationPreferenceSupported()
}

/** Returns true is media vibration preference is supported by this device. */
fun Context.isMediaVibrationPreferenceSupported(): Boolean =
    resources.getBoolean(R.bool.config_media_vibration_supported)

// LINT.ThenChange(MediaVibrationTogglePreferenceController.java)
