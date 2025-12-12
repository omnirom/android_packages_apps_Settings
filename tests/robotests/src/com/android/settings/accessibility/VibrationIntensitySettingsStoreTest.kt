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
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.Vibrator
import android.provider.Settings
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.AccessibilityUtil.State
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settingslib.datastore.SettingsSystemStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAudioManager::class])
class VibrationIntensitySettingsStoreTest {
    private companion object {
        const val TOUCH_KEY: String = Settings.System.HAPTIC_FEEDBACK_INTENSITY
        const val TOUCH_USAGE: Int = VibrationAttributes.USAGE_TOUCH
        const val RINGTONE_KEY: String = Settings.System.RING_VIBRATION_INTENSITY
        const val RINGTONE_USAGE: Int = VibrationAttributes.USAGE_RINGTONE
        const val DEFAULT_INTENSITY: Int = Vibrator.VIBRATION_INTENSITY_MEDIUM
        const val SUPPORTED_INTENSITIES: Int = Vibrator.VIBRATION_INTENSITY_HIGH
    }

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val settingsStore = SettingsSystemStore.get(context)
    private val touchStore =
        VibrationIntensitySettingsStore(
            context = context,
            preferenceKey = "some_key",
            settingsProviderKey = TOUCH_KEY,
            vibrationUsage = TOUCH_USAGE,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )
    private val ringStore =
        VibrationIntensitySettingsStore(
            context = context,
            preferenceKey = "other_key",
            settingsProviderKey = RINGTONE_KEY,
            vibrationUsage = RINGTONE_USAGE,
            keyValueStoreDelegate = settingsStore,
            defaultIntensity = DEFAULT_INTENSITY,
            supportedIntensityLevels = SUPPORTED_INTENSITIES,
        )

    @Test
    fun isPreferenceEnabled_returnsVibrateOnSettingOrTrue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, null)
        assertThat(touchStore.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        assertThat(touchStore.isPreferenceEnabled()).isTrue()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        assertThat(touchStore.isPreferenceEnabled()).isFalse()
    }

    @Test
    fun isPreferenceEnabled_noRingerModeDependency_ignoresRingerMode() {
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        settingsStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(touchStore.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun isPreferenceEnabled_withRingerModeDependency_returnsDisabledWhenRingerModeSilent() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        settingsStore.setInt(RINGTONE_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(ringStore.isPreferenceEnabled()).isTrue()
        assertThat(settingsStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(ringStore.getBoolean(RINGTONE_KEY)).isTrue()
        assertThat(ringStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)

        setRingerMode(AudioManager.RINGER_MODE_SILENT)

        assertThat(ringStore.isPreferenceEnabled()).isFalse()
        assertThat(settingsStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(ringStore.getBoolean(RINGTONE_KEY)).isFalse()
        assertThat(ringStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun isDisabledByRingerMode_returnsWhenRingerModeSilentAndMainSwitchOn() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(ringStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(ringStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        assertThat(ringStore.isDisabledByRingerMode()).isFalse()

        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        assertThat(ringStore.isDisabledByRingerMode()).isTrue()
    }

    @Test
    fun getValue_badKey_returnsOriginalKey() {
        settingsStore.setBoolean(TOUCH_KEY, true)
        settingsStore.setBoolean("bad_key", false)

        assertThat(settingsStore.getBoolean("bad_key")).isFalse()
        assertThat(touchStore.getBoolean("bad_key")).isTrue()
    }

    @Test
    fun getValue_preferenceDisabledByMainSwitch_returnsIntensityOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        settingsStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_preferenceDisabledByRingerMode_returnsIntensityOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, true)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        settingsStore.setInt(RINGTONE_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(ringStore.getBoolean(RINGTONE_KEY)).isFalse()
        assertThat(ringStore.getInt(RINGTONE_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_valueNull_returnDefaultIntensity() {
        touchStore.setInt(TOUCH_KEY, null)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isNull()
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun getValue_valueTrue_returnDefaultIntensity() {
        touchStore.setBoolean(TOUCH_KEY, true)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun getValue_valueFalse_returnIntensityOff() {
        touchStore.setBoolean(TOUCH_KEY, false)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_valueIntensityIntSupported_returnValueSet() {
        settingsStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun getValue_valueIntensityIntUnsupported_returnMaxSupported() {
        settingsStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH + 1)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH + 1)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun getValue_valueIntensityOff_returnIntensityOff() {
        settingsStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_OFF)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_preferenceDisabled_returnOffAndPreservesValue() {
        settingsStore.setBoolean(Settings.System.VIBRATE_ON, false)
        touchStore.setBoolean(TOUCH_KEY, true)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_hapticFeedbackEnabled_returnsIntensityValue() {
        settingsStore.setInt(
            Settings.System.HAPTIC_FEEDBACK_INTENSITY,
            Vibrator.VIBRATION_INTENSITY_LOW,
        )
        setHapticFeedbackEnabled(true)

        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)

        touchStore.setBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY, false)
        setHapticFeedbackEnabled(true)

        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun getValue_hapticFeedbackDisabledIntensityOn_returnsIntensityOff() {
        settingsStore.setInt(
            Settings.System.HAPTIC_FEEDBACK_INTENSITY,
            Vibrator.VIBRATION_INTENSITY_HIGH,
        )
        setHapticFeedbackEnabled(false)

        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
    }

    @Test
    fun setValue_updatesCorrectly() {
        touchStore.setBoolean(TOUCH_KEY, null)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isNull()
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)

        touchStore.setBoolean(TOUCH_KEY, false)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isFalse()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)

        touchStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)

        touchStore.setBoolean(TOUCH_KEY, true)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(DEFAULT_INTENSITY)
    }

    @Test
    fun setUnsupportedIntValue_updatesWithinSupportedLevels() {
        touchStore.setInt(TOUCH_KEY, Vibrator.VIBRATION_INTENSITY_HIGH + 1)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(touchStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(touchStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
    }

    @Test
    fun supportsOneLevel_usesDefaultIntensity() {
        val testStore =
            VibrationIntensitySettingsStore(
                context = context,
                preferenceKey = "some_key",
                settingsProviderKey = TOUCH_KEY,
                vibrationUsage = TOUCH_USAGE,
                keyValueStoreDelegate = settingsStore,
                defaultIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM,
                supportedIntensityLevels = 1,
            )

        testStore.setInt(TOUCH_KEY, null)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isNull()
        assertThat(testStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(testStore.getInt(TOUCH_KEY)).isEqualTo(1)

        testStore.setInt(TOUCH_KEY, 1)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_MEDIUM)
        assertThat(testStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(testStore.getInt(TOUCH_KEY)).isEqualTo(1)
    }

    @Test
    fun supportsTwoLevels_usesLowAndHighIntensities() {
        val testStore =
            VibrationIntensitySettingsStore(
                context = context,
                preferenceKey = "some_key",
                settingsProviderKey = TOUCH_KEY,
                vibrationUsage = TOUCH_USAGE,
                keyValueStoreDelegate = settingsStore,
                defaultIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM,
                supportedIntensityLevels = 2,
            )

        testStore.setInt(TOUCH_KEY, null)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isNull()
        assertThat(testStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(testStore.getInt(TOUCH_KEY)).isEqualTo(2)

        testStore.setInt(TOUCH_KEY, 1)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(testStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(testStore.getInt(TOUCH_KEY)).isEqualTo(1)

        testStore.setInt(TOUCH_KEY, 2)

        assertThat(settingsStore.getInt(TOUCH_KEY)).isEqualTo(Vibrator.VIBRATION_INTENSITY_HIGH)
        assertThat(testStore.getBoolean(TOUCH_KEY)).isTrue()
        assertThat(testStore.getInt(TOUCH_KEY)).isEqualTo(2)
    }

    @Test
    fun setValue_ringIntensity_updatesVibrateWhenRinging() {
        setVibrateWhenRinging(null)

        ringStore.setInt(Settings.System.RING_VIBRATION_INTENSITY, null)
        assertThat(getStoredVibrateWhenRinging()).isNull()

        ringStore.setBoolean(Settings.System.RING_VIBRATION_INTENSITY, false)
        assertThat(getStoredVibrateWhenRinging()).isFalse()

        ringStore.setInt(Settings.System.RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_LOW)
        assertThat(getStoredVibrateWhenRinging()).isTrue()
    }

    @Test
    fun setValue_hapticFeedbackIntensity_updateHapticFeedbackEnabledAndHardwareFeedbackIntensity() {
        setHapticFeedbackEnabled(null)
        setHardwareFeedbackIntensity(null)

        touchStore.setInt(Settings.System.HAPTIC_FEEDBACK_INTENSITY, null)
        assertThat(getStoredHapticFeedbackEnabled()).isNull()
        assertThat(getStoredHardwareFeedbackIntensity()).isNull()

        touchStore.setBoolean(Settings.System.HAPTIC_FEEDBACK_INTENSITY, false)
        assertThat(getStoredHapticFeedbackEnabled()).isFalse()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(DEFAULT_INTENSITY)

        touchStore.setInt(
            Settings.System.HAPTIC_FEEDBACK_INTENSITY,
            Vibrator.VIBRATION_INTENSITY_LOW,
        )
        assertThat(getStoredHapticFeedbackEnabled()).isTrue()
        assertThat(getStoredHardwareFeedbackIntensity()).isEqualTo(Vibrator.VIBRATION_INTENSITY_LOW)
    }

    @Suppress("DEPRECATION")
    private fun getStoredVibrateWhenRinging() =
        SettingsSystemStore.get(context).getBoolean(Settings.System.VIBRATE_WHEN_RINGING)

    @Suppress("DEPRECATION")
    private fun setVibrateWhenRinging(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(Settings.System.VIBRATE_WHEN_RINGING, value)

    @Suppress("DEPRECATION")
    private fun getStoredHapticFeedbackEnabled() =
        SettingsSystemStore.get(context).getInt(Settings.System.HAPTIC_FEEDBACK_ENABLED)?.let {
            it == State.ON
        }

    @Suppress("DEPRECATION")
    private fun setHapticFeedbackEnabled(value: Boolean?) =
        SettingsSystemStore.get(context)
            .setInt(
                Settings.System.HAPTIC_FEEDBACK_ENABLED,
                value?.let { if (it) State.ON else State.OFF },
            )

    private fun getStoredHardwareFeedbackIntensity() =
        SettingsSystemStore.get(context).getInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY)

    private fun setHardwareFeedbackIntensity(value: Int?) =
        SettingsSystemStore.get(context)
            .setInt(Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY, value)

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }
}
