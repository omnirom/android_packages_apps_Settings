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
import android.provider.Settings.System.NOTIFICATION_VIBRATION_INTENSITY
import com.android.settings.R

/** Accessibility settings for notification vibration, using a switch toggle. */
// LINT.IfChange
class NotificationVibrationIntensitySwitchPreference(
    context: Context,
    key: String,
    mainSwitchPreferenceKey: String,
) :
    VibrationIntensitySwitchPreference(
        context = context,
        key = key,
        settingsProviderKey = NOTIFICATION_VIBRATION_INTENSITY,
        mainSwitchPreferenceKey = mainSwitchPreferenceKey,
        vibrationUsage = VibrationAttributes.USAGE_NOTIFICATION,
        title = R.string.accessibility_notification_vibration_title,
    ) {
    override val keywords: Int
        get() = R.string.keywords_notification_vibration
}
// LINT.ThenChange(NotificationVibrationTogglePreferenceController.java)
