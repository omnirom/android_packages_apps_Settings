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
import android.content.ContextWrapper
import android.media.AudioManager
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.System.APPLY_RAMPING_RINGER
import android.provider.Settings.System.RING_VIBRATION_INTENSITY
import androidx.core.content.getSystemService
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.RampingRingerVibrationSwitchPreference.TelephonyConfigProvider
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.PreferenceBindingFactory
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
@Config(shadows = [ShadowAudioManager::class])
class RampingRingerVibrationSwitchPreferenceTest {
    private val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    private val preference = RampingRingerVibrationSwitchPreference(context, TEST_KEY, RING_KEY)

    @Before
    fun setUp() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    @Test
    fun isAvailable_enabledInTelephony_unavailable() {
        val newPreference =
            RampingRingerVibrationSwitchPreference(
                context,
                key = TEST_KEY,
                ringPreferenceKey = RING_KEY,
                deviceConfig =
                    object : TelephonyConfigProvider {
                        override fun isTelephonyRampingRingerEnabled() = true

                        override fun isVoiceCapable(context: Context) = true
                    },
            )

        assertThat(newPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_notVoiceCapable_unavailable() {
        val newPreference =
            RampingRingerVibrationSwitchPreference(
                context,
                key = TEST_KEY,
                ringPreferenceKey = RING_KEY,
                deviceConfig =
                    object : TelephonyConfigProvider {
                        override fun isTelephonyRampingRingerEnabled() = false

                        override fun isVoiceCapable(context: Context) = false
                    },
            )

        assertThat(newPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_voiceCapableAndDisabledInTelephony_available() {
        val newPreference =
            RampingRingerVibrationSwitchPreference(
                context,
                key = TEST_KEY,
                ringPreferenceKey = RING_KEY,
                deviceConfig =
                    object : TelephonyConfigProvider {
                        override fun isTelephonyRampingRingerEnabled() = false

                        override fun isVoiceCapable(context: Context) = true
                    },
            )

        assertThat(newPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun state_valueUnset_enabledAndUnchecked() {
        setRingIntensityValue(true)
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun state_valueTrue_enabledAndChecked() {
        setRingIntensityValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_valueFalse_enabledAndUnchecked() {
        setRingIntensityValue(true)
        setValue(false)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun state_valueTrueAndRingVibrationOff_disabledAndUnchecked() {
        setRingIntensityValue(false)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun click_withDifferentStates_updatesStateCorrectly() {
        setRingIntensityValue(true)
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun click_withStorage_storesBooleanValues() {
        setRingIntensityValue(true)
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(getRawStoredValue()).isFalse()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun click_withVibrator_playsHapticPreviewWhenChecked() {
        setRingIntensityValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        verify(vibratorSpy).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    @Test
    fun click_withRingOff_doesNothing() {
        setRingIntensityValue(false)
        setValue(true)
        val widget = createWidget()
        bindWidget(widget)

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    @Test
    fun ringStateChange_withValueTrue_updateCheckedStateAndRestoreOriginal() {
        setRingIntensityValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()

        setRingIntensityValue(false)
        bindWidget(widget)

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        setRingIntensityValue(true)
        bindWidget(widget)

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun ringStateChange_withValueTrue_doesNotUpdateStoredValue() {
        setRingIntensityValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()

        setRingIntensityValue(false)
        bindWidget(widget)

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isFalse()

        setRingIntensityValue(true)
        bindWidget(widget)

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun ringStateChange_withVibrator_doesNotPlayPreviewWhenCheckStateRestored() {
        setRingIntensityValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        setRingIntensityValue(false)
        bindWidget(widget)

        assertThat(widget.isChecked).isFalse()

        setRingIntensityValue(true)
        bindWidget(widget)

        assertThat(widget.isChecked).isTrue()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    private fun getRawStoredValue() =
        SettingsSystemStore.get(context).getBoolean(APPLY_RAMPING_RINGER)

    private fun setValue(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(APPLY_RAMPING_RINGER, value)

    private fun setRingIntensityValue(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(RING_VIBRATION_INTENSITY, value)

    private fun createWidget(): SwitchPreferenceCompat = preference.createAndBindWidget(context)

    private fun bindWidget(widget: SwitchPreferenceCompat) {
        PreferenceBindingFactory.defaultFactory
            .getPreferenceBinding(preference)!!
            .bind(widget, preference)
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }

    companion object {
        private const val TEST_KEY = "some_key"
        private const val RING_KEY = "ring_key"
    }
}
// LINT.ThenChange(VibrationRampingRingerTogglePreferenceControllerTest.java)
