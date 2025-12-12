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

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.os.Vibrator
import android.platform.test.annotations.EnableFlags
import android.provider.Settings.System.APPLY_RAMPING_RINGER
import android.provider.Settings.System.KEYBOARD_VIBRATION_ENABLED
import android.provider.Settings.System.NOTIFICATION_VIBRATION_INTENSITY
import android.provider.Settings.System.RING_VIBRATION_INTENSITY
import androidx.core.content.getSystemService
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

// LINT.IfChange
@Config(shadows = [ShadowAudioManager::class])
class VibrationScreenTest : SettingsCatalystTestCase() {
    private val testScope = TestScope()
    private lateinit var vibratorMock: Vibrator

    private val resourcesSpy: Resources =
        spy((ApplicationProvider.getApplicationContext() as Context).resources)

    private val context: Context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == VIBRATOR_SERVICE -> vibratorMock
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = resourcesSpy
        }

    override val preferenceScreenCreator = VibrationScreen()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN

    @Before
    fun setUp() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    @Test
    fun isAvailable_noVibrator_unavailable() {
        vibratorMock = mock { on { hasVibrator() } doReturn false }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndMultipleIntensityLevels_unavailable() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 3
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndSingleIntensityLevel_available() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(R.integer.config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun mainSwitchClick_withIntensitiesSet_disablesAndUnchecksAllIntensitiesAndPreservesStorage() {
        val intensityKeys = findVibrationIntensitySwitchPreferences()
        assertThat(intensityKeys).isNotEmpty()

        // Setup initial vibration intensities.
        val originalIntensity = Vibrator.VIBRATION_INTENSITY_HIGH
        intensityKeys.values.forEach { key -> setStoredIntensity(key, originalIntensity) }
        // Setup other switches.
        setStoredBoolean(APPLY_RAMPING_RINGER, true)
        setStoredBoolean(KEYBOARD_VIBRATION_ENABLED, true)

        testOnFragment { fragment ->
            val intensitySwitches =
                intensityKeys.keys
                    .stream()
                    .map { key -> fragment.findPreference<SwitchPreferenceCompat>(key)!! }
                    .toList()
            val rampingRingerSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_apply_ramping_ringer")!!
            val keyboardSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_keyboard_vibration_enabled")!!
            val mainSwitch: MainSwitchPreference = fragment.findPreference("toggle_vibrate_on")!!

            // Check all intensity switches are enabled and checked.
            assertThat(mainSwitch.isChecked).isTrue()
            intensitySwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)
            assertSwitchCheckedAndEnabled(keyboardSwitch)

            // Turn main switch off.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper()

            // Check all intensities are disabled and unchecked, and stored value is preserved.
            assertThat(mainSwitch.isChecked).isFalse()
            intensitySwitches.forEach { switch -> assertSwitchUncheckedAndDisabled(switch) }
            intensityKeys.values.forEach { settingKey ->
                assertExpectedStoredIntensity(settingKey, originalIntensity)
            }
            assertSwitchUncheckedAndDisabled(rampingRingerSwitch)
            assertExpectedStoredBoolean(APPLY_RAMPING_RINGER, true)
            assertSwitchUncheckedAndDisabled(keyboardSwitch)
            assertExpectedStoredBoolean(KEYBOARD_VIBRATION_ENABLED, true)

            // Turn main switch back on.
            mainSwitch.performClick()
            ShadowLooper.idleMainLooper()

            // Check all intensity switches restored.
            assertThat(mainSwitch.isChecked).isTrue()
            intensitySwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)
            assertSwitchCheckedAndEnabled(keyboardSwitch)
        }
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun ringerModeChange_disablesOnlyRingAndNotificationOnSilentMode() {
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
        val intensityKeys = findVibrationIntensitySwitchPreferences()
        assertThat(intensityKeys).isNotEmpty()

        // Setup initial vibration intensities.
        val originalIntensity = Vibrator.VIBRATION_INTENSITY_MEDIUM
        intensityKeys.values.forEach { key -> setStoredIntensity(key, originalIntensity) }
        // Setup ramping ringer switch.
        setStoredBoolean(APPLY_RAMPING_RINGER, true)

        testOnFragment { fragment ->
            val allIntensitySwitches =
                intensityKeys.keys
                    .stream()
                    .map { key -> fragment.findPreference<SwitchPreferenceCompat>(key)!! }
                    .toList()
            val independentIntensitySwitches =
                allIntensitySwitches
                    .stream()
                    .filter { switch ->
                        switch.key != "toggle_ring_vibration_intensity" &&
                            switch.key != "toggle_notification_vibration_intensity"
                    }
                    .toList()
            val ringSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_ring_vibration_intensity")!!
            val notificationSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_notification_vibration_intensity")!!
            val rampingRingerSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_apply_ramping_ringer")!!

            // Check all intensity switches are enabled and checked.
            allIntensitySwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)

            // Turn ringer mode silent.
            setRingerMode(AudioManager.RINGER_MODE_SILENT)
            context.sendBroadcast(Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION))
            ShadowLooper.idleMainLooper()

            // Check only ring and notification are disabled and unchecked.
            assertSwitchUncheckedAndDisabled(ringSwitch)
            assertExpectedStoredIntensity(RING_VIBRATION_INTENSITY, originalIntensity)
            assertSwitchUncheckedAndDisabled(notificationSwitch)
            assertExpectedStoredIntensity(NOTIFICATION_VIBRATION_INTENSITY, originalIntensity)
            assertSwitchUncheckedAndDisabled(rampingRingerSwitch)
            assertExpectedStoredBoolean(APPLY_RAMPING_RINGER, true)
            independentIntensitySwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }

            // Turn ringer mode vibrate-only.
            setRingerMode(AudioManager.RINGER_MODE_VIBRATE)
            context.sendBroadcast(Intent(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION))
            ShadowLooper.idleMainLooper()

            // Check all intensity switches restored.
            allIntensitySwitches.forEach { switch -> assertSwitchCheckedAndEnabled(switch) }
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)
        }
    }

    @EnableFlags(Flags.FLAG_CATALYST_VIBRATION_INTENSITY_SCREEN_25Q4)
    @Test
    fun ringVibrationChange_disablesRampingRingerAndPreservesStorage() {
        // Setup initial ring vibration and ramping ringer.
        setStoredIntensity(RING_VIBRATION_INTENSITY, Vibrator.VIBRATION_INTENSITY_MEDIUM)
        setStoredBoolean(APPLY_RAMPING_RINGER, true)

        testOnFragment { fragment ->
            val ringSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_ring_vibration_intensity")!!
            val rampingRingerSwitch: SwitchPreferenceCompat =
                fragment.findPreference("toggle_apply_ramping_ringer")!!

            // Check ramping ringer enabled and checked.
            assertThat(ringSwitch.isChecked).isTrue()
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)

            // Turn ring vibrations off.
            ringSwitch.performClick()
            ShadowLooper.idleMainLooper()

            // Check ramping ringer disabled and unchecked.
            assertThat(ringSwitch.isChecked).isFalse()
            assertSwitchUncheckedAndDisabled(rampingRingerSwitch)
            assertExpectedStoredBoolean(APPLY_RAMPING_RINGER, true)

            // Turn ring vibrations back on.
            ringSwitch.performClick()
            ShadowLooper.idleMainLooper()

            // Check ramping ringer restored.
            assertThat(ringSwitch.isChecked).isTrue()
            assertSwitchCheckedAndEnabled(rampingRingerSwitch)
        }
    }

    private fun assertSwitchUncheckedAndDisabled(switch: SwitchPreferenceCompat) {
        assertWithSwitch(switch).that(switch.isEnabled).isFalse()
        assertWithSwitch(switch).that(switch.isChecked).isFalse()
    }

    private fun assertSwitchCheckedAndEnabled(switch: SwitchPreferenceCompat) {
        assertWithSwitch(switch).that(switch.isEnabled).isTrue()
        assertWithSwitch(switch).that(switch.isChecked).isTrue()
    }

    private fun assertExpectedStoredIntensity(settingKey: String, expectedIntensity: Int?) {
        assertWithMessage("On setting key %s", settingKey)
            .that(getStoredIntensity(settingKey))
            .isEqualTo(expectedIntensity)
    }

    private fun assertExpectedStoredBoolean(settingKey: String, expectedStoredValue: Boolean?) {
        assertWithMessage("On setting key %s", settingKey)
            .that(getStoredBoolean(settingKey))
            .isEqualTo(expectedStoredValue)
    }

    private fun assertWithSwitch(switch: SwitchPreferenceCompat) =
        assertWithMessage("On switch with key %s", switch.key)

    private fun setStoredBoolean(settingsKey: String, value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(settingsKey, value)

    private fun getStoredBoolean(settingsKey: String) =
        SettingsSystemStore.get(context).getBoolean(settingsKey)

    private fun setStoredIntensity(settingsKey: String, value: Int?) =
        SettingsSystemStore.get(context).setInt(settingsKey, value)

    private fun getStoredIntensity(settingsKey: String) =
        SettingsSystemStore.get(context).getInt(settingsKey)

    private fun testOnFragment(action: (PreferenceFragmentCompat) -> Unit) {
        @Suppress("UNCHECKED_CAST")
        val clazz = preferenceScreenCreator.fragmentClass() as Class<PreferenceFragmentCompat>
        launchFragment(clazz) { fragment -> action.invoke(fragment) }
    }

    private fun findVibrationIntensitySwitchPreferences(): Map<String, String> {
        val switches = HashMap<String, String>()
        preferenceScreenCreator.getPreferenceHierarchy(context, testScope).forEachRecursively {
            child ->
            if (child.metadata is VibrationIntensitySwitchPreference) {
                val switch = child.metadata as VibrationIntensitySwitchPreference
                switches.put(switch.key, switch.settingsProviderKey)
            }
        }
        return switches
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }
}
// LINT.ThenChange(VibrationPreferenceControllerTest.java)
