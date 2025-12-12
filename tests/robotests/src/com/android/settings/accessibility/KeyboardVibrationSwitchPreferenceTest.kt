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
import android.content.res.Resources
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.provider.Settings.System.KEYBOARD_VIBRATION_ENABLED
import androidx.core.content.getSystemService
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.internal.R
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.preference.PreferenceBindingFactory
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class KeyboardVibrationSwitchPreferenceTest {
    private val resourcesSpy: Resources =
        spy(ApplicationProvider.getApplicationContext<Context>().resources)

    private val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getResources(): Resources = resourcesSpy

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    private val preference = KeyboardVibrationSwitchPreference(context, "some_key")

    @Before
    fun setUp() {
        setMainVibrationValue(true)
    }

    @Test
    fun isAvailable_keyboardVibrationSettingsNotSupport_unavailable() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_keyboardVibrationSettingsSupported) } doReturn false
        }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_keyboardVibrationSettingsSupport_available() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_keyboardVibrationSettingsSupported) } doReturn true
        }

        assertThat(preference.isAvailable(context)).isTrue()
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
    fun state_valueTrueAndMainVibrationOff_disabledAndUnchecked() {
        setMainVibrationValue(false)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()
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
    fun click_withStorage_storesBooleanValues() {
        setValue(null)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isNull()
        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(getRawStoredValue()).isFalse()
        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withVibrator_playsHapticPreviewWhenChecked() {
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
    fun click_withMainStateOff_doesNothing() {
        setMainVibrationValue(false)
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
    fun mainStateChange_withValueTrue_updateCheckedStateAndRestoreOriginal() {
        setMainVibrationValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()

        setMainVibrationValue(false)
        bindWidget(widget)

        assertThat(widget.isEnabled).isFalse()
        assertThat(widget.isChecked).isFalse()

        setMainVibrationValue(true)
        bindWidget(widget)

        assertThat(widget.isEnabled).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun mainStateChange_withValueTrue_doesNotUpdateStoredValue() {
        setMainVibrationValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()

        setMainVibrationValue(false)
        bindWidget(widget)

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isFalse()

        setMainVibrationValue(true)
        bindWidget(widget)

        assertThat(getRawStoredValue()).isTrue()
        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun mainStateChange_withVibrator_doesNotPlayPreviewWhenCheckStateRestored() {
        setMainVibrationValue(true)
        setValue(true)
        val widget = createWidget()

        assertThat(widget.isChecked).isTrue()

        setMainVibrationValue(false)
        bindWidget(widget)

        assertThat(widget.isChecked).isFalse()

        setMainVibrationValue(true)
        bindWidget(widget)

        assertThat(widget.isChecked).isTrue()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    private fun getRawStoredValue() =
        SettingsSystemStore.get(context).getBoolean(KEYBOARD_VIBRATION_ENABLED)

    private fun setValue(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(KEYBOARD_VIBRATION_ENABLED, value)

    private fun setMainVibrationValue(value: Boolean?) =
        SettingsSystemStore.get(context).setBoolean(Settings.System.VIBRATE_ON, value)

    private fun createWidget(): SwitchPreferenceCompat = preference.createAndBindWidget(context)

    private fun bindWidget(widget: SwitchPreferenceCompat) {
        PreferenceBindingFactory.defaultFactory
            .getPreferenceBinding(preference)!!
            .bind(widget, preference)
    }
}
// LINT.ThenChange(keyboardVibrationTogglePreferenceControllerTest.java)
