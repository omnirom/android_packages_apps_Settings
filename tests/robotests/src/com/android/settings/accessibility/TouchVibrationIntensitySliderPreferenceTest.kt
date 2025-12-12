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

import android.os.Vibrator
import android.provider.Settings
import com.android.settings.accessibility.AccessibilityUtil.State
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test

// LINT.IfChange
class TouchVibrationIntensitySliderPreferenceTest : VibrationIntensitySliderPreferenceTestCase() {
    override val hasRingerModeDependency = false
    override val preference = TouchVibrationIntensitySliderPreference(context)

    @Test
    fun state_valueTrueAndHapticFeedbackEnabledFalse_enabledAndUnchecked() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(Vibrator.VIBRATION_INTENSITY_HIGH)
        setHapticFeedbackEnabled(false)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun state_valueFalseAndHapticFeedbackEnabledTrue_enabledAndUnchecked() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(Vibrator.VIBRATION_INTENSITY_OFF)
        setHapticFeedbackEnabled(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun setValue_withHapticFeedbackEnabled_updatesDeprecatedSetting() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(null)
        setHapticFeedbackEnabled(null)
        val widget = createWidget()

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        assertThat(getStoredHapticFeedbackEnabled()).isNull()

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(getStoredHapticFeedbackEnabled()).isFalse()

        widget.value = Vibrator.VIBRATION_INTENSITY_HIGH

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(getStoredHapticFeedbackEnabled()).isTrue()
    }

    @Test
    fun setValue_withHardwareFeedback_updatesDependentSetting() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(null)
        setHardwareFeedbackIntensity(null)
        val widget = createWidget()

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        assertThat(getStoredHardwareFeedbackIntensity()).isNull()

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        // Hardware feedback intensity is not turned off by this preference
        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(getStoredHardwareFeedbackIntensity())
            .isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)

        widget.value = Vibrator.VIBRATION_INTENSITY_LOW

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(getStoredHardwareFeedbackIntensity())
            .isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
    }

    private fun getStoredHardwareFeedbackIntensity() =
        SettingsSystemStore.get(context).getInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY)

    @Suppress("DEPRECATION")
    private fun getStoredHapticFeedbackEnabled() =
        SettingsSystemStore.get(context).getInt(Settings.System.HAPTIC_FEEDBACK_ENABLED)?.let {
            it == State.ON
        }

    @Suppress("DEPRECATION")
    private fun setHapticFeedbackEnabled(value: Boolean?) =
        SettingsSystemStore.get(context).setInt(
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            value?.let { if (it) State.ON else State.OFF },
        )

    private fun setHardwareFeedbackIntensity(value: Int?) =
        SettingsSystemStore.get(context).setInt(
            Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
            value,
        )
}
// LINT.ThenChange(HapticFeedbackIntensityPreferenceControllerTest.java)
