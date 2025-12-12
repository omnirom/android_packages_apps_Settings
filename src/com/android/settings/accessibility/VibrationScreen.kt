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
package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Vibrator
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.VibrationSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

/** Accessibility settings for vibration. */
// LINT.IfChange
@ProvidePreferenceScreen(VibrationScreen.KEY)
open class VibrationScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_vibration_settings_title

    override val keywords: Int
        get() = R.string.keywords_vibration

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_VIBRATION

    override fun isAvailable(context: Context) =
        context.hasVibrator && context.getSupportedVibrationIntensityLevels() == 1

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystVibrationIntensityScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = VibrationSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +VibrationMainSwitchPreference(MAIN_SWITCH_KEY)
            // The preferences below are migrated behind a different flag from the screen migration.
            // They should only be declared in this screen hierarchy if their migration is enabled.
            if (Flags.catalystVibrationIntensityScreen25q4()) {
                +CallVibrationPreferenceCategory("toggle_vibration_category_call") += {
                    +RingVibrationIntensitySwitchPreference(
                        context,
                        "toggle_ring_vibration_intensity",
                        MAIN_SWITCH_KEY,
                    )
                    +RampingRingerVibrationSwitchPreference(
                        context,
                        key = "toggle_apply_ramping_ringer",
                        ringPreferenceKey = "toggle_ring_vibration_intensity",
                    )
                }
                +NotificationAlarmVibrationPreferenceCategory(
                    "toggle_vibration_category_notification_alarm"
                ) +=
                    {
                        +NotificationVibrationIntensitySwitchPreference(
                            context,
                            "toggle_notification_vibration_intensity",
                            MAIN_SWITCH_KEY,
                        )
                        +AlarmVibrationIntensitySwitchPreference(
                            context,
                            "toggle_alarm_vibration_intensity",
                            MAIN_SWITCH_KEY,
                        )
                    }
                +InteractiveHapticsPreferenceCategory("toggle_vibration_category_haptics") += {
                    +TouchVibrationIntensitySwitchPreference(
                        context,
                        "toggle_haptic_feedback_intensity",
                        MAIN_SWITCH_KEY,
                    )
                    +MediaVibrationIntensitySwitchPreference(
                        context,
                        "toggle_media_vibration_intensity",
                        MAIN_SWITCH_KEY,
                    )
                    +KeyboardVibrationSwitchPreference(
                        context,
                        "toggle_keyboard_vibration_enabled",
                        MAIN_SWITCH_KEY,
                    )
                }
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        if (Flags.deeplinkSoundAndVibration25q4()) {
            makeLaunchIntent(context, VibrationSettingsActivity::class.java, metadata?.key)
        } else {
            super.getLaunchIntent(context, metadata)
        }

    companion object {
        const val KEY = "vibration_screen"
        const val MAIN_SWITCH_KEY = "toggle_vibrate_on"
    }
}

/** Call vibration preferences (e.g. ringtone, ramping ringer, etc). */
class CallVibrationPreferenceCategory(
    key: String = "vibration_category_call",
    title: Int = R.string.accessibility_call_vibration_category_title,
) : PreferenceCategory(key, title)

/** Notification and alarm vibration preferences. */
class NotificationAlarmVibrationPreferenceCategory(
    key: String = "vibration_category_notification_alarm",
    title: Int = R.string.accessibility_notification_alarm_vibration_category_title,
) : PreferenceCategory(key, title)

/** Interactive haptics preferences (e.g. touch feedback, media, keyboard, etc). */
class InteractiveHapticsPreferenceCategory(
    key: String = "vibration_category_haptics",
    title: Int = R.string.accessibility_interactive_haptics_category_title,
) : PreferenceCategory(key, title)

/** Returns true if the device has a system vibrator, false otherwise. */
val Context.hasVibrator: Boolean
    get() = getSystemService(Vibrator::class.java)?.hasVibrator() == true

// LINT.ThenChange(VibrationPreferenceController.java)
