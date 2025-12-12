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
import android.provider.Settings
import com.android.settings.R

/** Accessibility settings for alarm vibration, as a slider. */
// LINT.IfChange
class AlarmVibrationIntensitySliderPreference(context: Context) :
    VibrationIntensitySliderPreference(
        context = context,
        key = KEY,
        vibrationUsage = VibrationAttributes.USAGE_ALARM,
        title = R.string.accessibility_alarm_vibration_title,
    ) {
    override val keywords: Int
        get() = R.string.keywords_alarm_vibration

    companion object {
        const val KEY = Settings.System.ALARM_VIBRATION_INTENSITY
    }
}
// LINT.ThenChange(AlarmVibrationIntensityPreferenceController.java)
