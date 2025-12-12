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
import android.provider.Settings.System.VIBRATE_ON
import androidx.core.content.getSystemService
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.PreferenceBindingFactory
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

/** Test case for vibration switch preferences. */
// LINT.IfChange
@Config(shadows = [ShadowAudioManager::class])
@RunWith(AndroidJUnit4::class)
abstract class VibrationIntensitySwitchPreferenceTestCase {
    protected abstract val hasRingerModeDependency: Boolean
    protected abstract val preference: VibrationIntensitySwitchPreference
    protected val mainSwitchPreference = VibrationMainSwitchPreference("some_key")

    protected val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    protected val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setUp() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    @Test
    fun state_valueUnset_enabledAndChecked() {
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_valueTrue_enabledAndChecked() {
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_valueFalse_enabledAndUnchecked() {
        setValue(false)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun state_valueTrueAndMainSwitchFalse_disabledAndUnchecked() {
        setMainSwitchValue(false)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun state_ringerModeNormal_enabledAndChecked() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_ringerModeVibrate_enabledAndChecked() {
        setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_ringerModeSilentWithoutDependency_enabledAndChecked() {
        assumeFalse(hasRingerModeDependency)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun state_ringerModeSilentWithDependency_disabledAndUncheckedAndPreservesStoredValue() {
        assumeTrue(hasRingerModeDependency)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        val defaultIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        vibratorSpy.stub { on { getDefaultVibrationIntensity(any()) } doReturn defaultIntensity }
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()
        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
    }

    @Test
    fun summary_preferenceEnabled_isNull() {
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.summary).isNull()
    }

    @Test
    fun summary_preferenceDisabledByMainSwitch_isNull() {
        setMainSwitchValue(false)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.summary).isNull()
    }

    @Test
    fun summary_preferenceDisabledByRingerModeSilent_isSilentModeMessage() {
        assumeTrue(hasRingerModeDependency)
        setRingerMode(AudioManager.RINGER_MODE_SILENT)
        val expectedSummary =
            context.getString(
                R.string.accessibility_vibration_setting_disabled_for_silent_mode_summary
            )
        setMainSwitchValue(true)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.summary).isEqualTo(expectedSummary)
    }

    @Test
    fun click_withDifferentStates_updatesStateCorrectly() {
        setValue(null)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withDefaultIntensity_storesIntensityValues() {
        val defaultIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        vibratorSpy.stub {
            on { getDefaultVibrationIntensity(preference.vibrationUsage) } doReturn defaultIntensity
        }
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(getRawStoredValue()).isEqualTo(Vibrator.VIBRATION_INTENSITY_OFF)
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withVibrator_playsHapticPreviewWhenChecked() {
        setValue(null)
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
    fun click_withMainSwitchFalse_doesNothing() {
        setMainSwitchValue(false)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isChecked).isFalse()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    @Test
    fun mainSwitchClick_withValueTrue_updateCheckedStateAndRestoreOriginal() {
        setMainSwitchValue(true)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun mainSwitchClick_withValueFalse_doesNotChangeCheckedState() {
        setMainSwitchValue(true)
        setValue(false)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isFalse()
    }

    @Test
    fun mainSwitchClick_withDefaultIntensity_doesNotUpdateStoredValue() {
        val defaultIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        vibratorSpy.stub { on { getDefaultVibrationIntensity(any()) } doReturn defaultIntensity }
        setMainSwitchValue(true)
        setValue(true)
        val widget = createWidget()
        val mainSwitchWidget = createMainSwitchWidget()

        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isChecked).isTrue()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
        assertThat(mainSwitchWidget.isChecked).isFalse()
        assertThat(widget.isChecked).isFalse()

        mainSwitchWidget.performClick()
        updatePreferenceBinding(widget)

        assertThat(getRawStoredValue()).isEqualTo(defaultIntensity)
        assertThat(mainSwitchWidget.isChecked).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }

    private fun getRawStoredValue() =
        SettingsSystemStore.get(context).getInt(preference.settingsProviderKey)

    private fun setMainSwitchValue(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(VIBRATE_ON, value)

    protected fun setValue(value: Boolean?) =
        SettingsSystemStore.get(context)
            .setInt(
                preference.settingsProviderKey,
                value?.let {
                    if (it) {
                        vibratorSpy.getDefaultVibrationIntensity(any())
                    } else {
                        Vibrator.VIBRATION_INTENSITY_OFF
                    }
                },
            )

    protected fun createWidget(): SwitchPreferenceCompat = preference.createAndBindWidget(context)

    private fun createMainSwitchWidget(): MainSwitchPreference =
        mainSwitchPreference.createAndBindWidget(context)

    private fun updatePreferenceBinding(widget: SwitchPreferenceCompat) =
        PreferenceBindingFactory.defaultFactory
            .getPreferenceBinding(preference)!!
            .bind(widget, preference)
}
// LINT.ThenChange(VibrationTogglePreferenceControllerTest.java)
