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

// LINT.IfChange
class RingVibrationIntensitySliderPreferenceTest : VibrationIntensitySliderPreferenceTestCase() {
    override val hasRingerModeDependency = true
    override val preference = RingVibrationIntensitySliderPreference(context)

    @Test
    fun setValue_updatesVibrateWhenRinging() {
        setSupportedLevels(3)
        setDefaultIntensity(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setValue(Vibrator.VIBRATION_INTENSITY_HIGH)
        setVibrateWhenRinging(null)
        val widget = createWidget()

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(getStoredVibrateWhenRinging()).isNull()

        widget.value = Vibrator.VIBRATION_INTENSITY_OFF

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(getStoredVibrateWhenRinging()).isFalse()

        widget.value = Vibrator.VIBRATION_INTENSITY_LOW

        assertThat(widget.value).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(getStoredVibrateWhenRinging()).isTrue()
    }

    @Suppress("DEPRECATION")
    private fun getStoredVibrateWhenRinging() =
        SettingsSystemStore.get(context).getBoolean(Settings.System.VIBRATE_WHEN_RINGING)

    @Suppress("DEPRECATION")
    private fun setVibrateWhenRinging(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(Settings.System.VIBRATE_WHEN_RINGING, value)
}
// LINT.ThenChange(RingVibrationIntensityPreferenceControllerTest.java)
