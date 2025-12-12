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

package com.android.settings.development

import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.provider.Settings
import android.view.View
import androidx.preference.PreferenceScreen
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.accessibility.TextCursorBlinkRateSliderPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class TextCursorBlinkRatePreferenceControllerTest {
    @get:Rule
    val setFlagsRule = SetFlagsRule()
    val context: Context = ApplicationProvider.getApplicationContext()

    val controller = TextCursorBlinkRatePreferenceController(context)

    // Spy on the preference to verify method calls on it
    val preference = spy(TextCursorBlinkRateSliderPreference(context))
    val preferenceScreen = mock<PreferenceScreen>()

    private val noBlinkDurationMs = 0
    private val minSliderValue = 0
    private val noBlinkLabel = "Don\'t blink"

    private val slowBlinkDurationMs = 1000
    private val slowBlinkSliderValue = 1
    private val slowBlinkLabel = "50%"

    private val fastBlinkDurationMs = 333
    private val maxSliderValue = 11
    private val fastBlinkLabel = "150%"

    private val defaultDurationMs = 500
    private val defaultSliderValue = 6
    private val defaultBlinkLabel = "100% (default)"

    @Before
    fun setup() {
        whenever(preferenceScreen.findPreference<TextCursorBlinkRateSliderPreference>(
            controller.getPreferenceKey())).thenReturn(preference)
        controller.displayPreference(preferenceScreen)

        val rootView =
            View.inflate(context, preference.layoutResource, null /* parent */)
        val holder = PreferenceViewHolder.createInstanceForTests(rootView)
        preference.onBindViewHolder(holder)
    }

    @Test
    @DisableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun getAvailabilityStatus_unavailableWhenFlagDisabled() {
        assertThat(controller.isAvailable()).isFalse()
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun getAvailabilityStatus_availableWhenFlagEnabled() {
        assertThat(controller.isAvailable()).isTrue()
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_zeroValue_noBlink() {
        controller.onPreferenceChange(preference, minSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(noBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_minNonZeroValue_slowBlink() {
        controller.onPreferenceChange(preference, slowBlinkSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(slowBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_maxValue_fastBlink() {
        controller.onPreferenceChange(preference, maxSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(fastBlinkDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_defaultValue_defaultBlink() {
        controller.onPreferenceChange(preference, defaultSliderValue)

        val value = getSecureSettingsValue()
        assertThat(value).isEqualTo(defaultDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_customStateDescription_default() {
        controller.onPreferenceChange(preference, defaultSliderValue)

        assertThat(getSliderStateDescription()).isEqualTo(defaultBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_customStateDescription_noBlink() {
        controller.onPreferenceChange(preference, minSliderValue)

        assertThat(getSliderStateDescription()).isEqualTo(noBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_customStateDescription_slowBlink() {
        controller.onPreferenceChange(preference, slowBlinkSliderValue)

        assertThat(getSliderStateDescription()).isEqualTo(slowBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onPreferenceChange_customStateDescription_fastBlink() {
        controller.onPreferenceChange(preference, maxSliderValue)

        assertThat(getSliderStateDescription()).isEqualTo(fastBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_noBlink_zeroValue() {
        setSecureSettingsValue(noBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(minSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_slowBlink_minNonZeroValue() {
        setSecureSettingsValue(slowBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(slowBlinkSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_fastBlink_maxValue() {
        setSecureSettingsValue(fastBlinkDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(maxSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_defaultBlink_defaultValue() {
        setSecureSettingsValue(defaultDurationMs)
        controller.updateState(preference)

        assertThat(preference.value).isEqualTo(defaultSliderValue)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_customStateDescription_default() {
        setSecureSettingsValue(defaultDurationMs)
        controller.updateState(preference)

        assertThat(getSliderStateDescription()).isEqualTo(defaultBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_customStateDescription_noBlink() {
        setSecureSettingsValue(noBlinkDurationMs)
        controller.updateState(preference)

        assertThat(getSliderStateDescription()).isEqualTo(noBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_customStateDescription_slowBlink() {
        setSecureSettingsValue(slowBlinkDurationMs)
        controller.updateState(preference)

        assertThat(getSliderStateDescription()).isEqualTo(slowBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun updateState_customStateDescription_fastBlink() {
        setSecureSettingsValue(fastBlinkDurationMs)
        controller.updateState(preference)

        assertThat(getSliderStateDescription()).isEqualTo(fastBlinkLabel)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun onDeveloperOptionsSwitchDisabled_resetsToDefault() {
        // Set a non-default value first to ensure it changes
        setSecureSettingsValue(fastBlinkDurationMs)
        assertThat(getSecureSettingsValue()).isEqualTo(fastBlinkDurationMs)

        // Action: Disable developer options
        controller.onDeveloperOptionsSwitchDisabled()

        // Verify: The setting is reset to the default value
        assertThat(getSecureSettingsValue()).isEqualTo(defaultDurationMs)
    }

    @Test
    @EnableFlags(android.view.accessibility.Flags.FLAG_TEXT_CURSOR_BLINK_INTERVAL)
    fun displayPreference_resetClickListener_resetsToDefault() {
        // In setup(), displayPreference() is called, which sets a reset click listener.
        // We capture the listener to invoke it and verify its behavior.
        val listenerCaptor = argumentCaptor<View.OnClickListener>()
        // Note: setResetClickListener may be called multiple times during setup. We verify at
        // least one call happened and grab the first listener, which is the one set by our
        // controller.
        verify(preference, atLeastOnce()).setResetClickListener(listenerCaptor.capture())
        val resetClickListener = listenerCaptor.firstValue

        // Set a non-default value to ensure it changes
        setSecureSettingsValue(fastBlinkDurationMs)
        // Update the preference to reflect the non-default value
        controller.updateState(preference)
        assertThat(preference.value).isEqualTo(maxSliderValue)
        assertThat(getSecureSettingsValue()).isEqualTo(fastBlinkDurationMs)

        // Action: Simulate a click on the reset button
        resetClickListener.onClick(mock())

        // Verify: The setting in Settings.Secure is reset to the default value
        assertThat(getSecureSettingsValue()).isEqualTo(defaultDurationMs)

        // Verify: The preference UI is also updated to the default value,
        // because the listener calls updateState().
        assertThat(preference.value).isEqualTo(defaultSliderValue)
    }

    private fun getSecureSettingsValue(): Int {
        return Settings.Secure.getInt(
            context.contentResolver,
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            defaultDurationMs
        )
    }

    private fun setSecureSettingsValue(value: Int) {
        Settings.Secure.putInt(context.getContentResolver(),
            Settings.Secure.ACCESSIBILITY_TEXT_CURSOR_BLINK_INTERVAL_MS,
            value
        )
    }

    private fun getSliderStateDescription(): CharSequence? {
        return preference.getSlider()?.stateDescription
    }
}