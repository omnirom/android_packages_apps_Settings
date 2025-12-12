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
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

// LINT.IfChange
class TouchVibrationIntensitySwitchPreferenceTest : VibrationIntensitySwitchPreferenceTestCase() {
    override val hasRingerModeDependency = false
    override val preference = TouchVibrationIntensitySwitchPreference(context, "key", "main_key")

    @Test
    fun state_valueTrueAndHapticFeedbackEnabledFalse_enabledAndUnchecked() {
        setValue(true)
        setHapticFeedbackEnabled(false)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun state_valueFalseAndHapticFeedbackEnabledTrue_enabledAndUnchecked() {
        setValue(false)
        setHapticFeedbackEnabled(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun click_withHapticFeedbackEnabled_updatesDeprecatedSetting() {
        setValue(null)
        setHapticFeedbackEnabled(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()
        assertThat(getStoredHapticFeedbackEnabled()).isNull()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        assertThat(getStoredHapticFeedbackEnabled()).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        assertThat(getStoredHapticFeedbackEnabled()).isTrue()
    }

    @Test
    fun click_withHardwareFeedback_updatesDependentSetting() {
        val defaultIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        vibratorSpy.stub { on { getDefaultVibrationIntensity(any()) } doReturn defaultIntensity }
        setValue(null)
        setHardwareFeedbackIntensity(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()
        assertThat(getStoredHardwareFeedbackIntensity()).isNull()

        widget.performClick()

        // Hardware feedback intensity is not turned off by this preference
        assertThat(widget.isChecked).isFalse()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(defaultIntensity)

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(defaultIntensity)
    }

    private fun getStoredHardwareFeedbackIntensity() =
        SettingsSystemStore.get(context).getInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY)

    @Suppress("DEPRECATION")
    private fun getStoredHapticFeedbackEnabled() =
        SettingsSystemStore.get(context).getBoolean(Settings.System.HAPTIC_FEEDBACK_ENABLED)

    @Suppress("DEPRECATION")
    private fun setHapticFeedbackEnabled(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(Settings.System.HAPTIC_FEEDBACK_ENABLED, value)

    private fun setHardwareFeedbackIntensity(value: Int?) =
        SettingsSystemStore.get(context)
            .setInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY, value)
}
// LINT.ThenChange(HapticFeedbackTogglePreferenceControllerTest.java)
