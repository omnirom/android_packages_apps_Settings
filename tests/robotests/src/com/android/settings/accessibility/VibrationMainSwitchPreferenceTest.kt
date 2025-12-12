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
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings.System.VIBRATE_ON
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class VibrationMainSwitchPreferenceTest {
    private val vibratorSpy: Vibrator =
        spy(ApplicationProvider.getApplicationContext<Context>().getSystemService<Vibrator>()!!)

    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == getSystemServiceName(Vibrator::class.java) -> vibratorSpy
                    else -> super.getSystemService(name)
                }
        }

    private val preference = VibrationMainSwitchPreference("some_key")

    @Test
    fun checked_valueUnset_returnDefaultTrue() {
        setVibrateOn(null)

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun checked_valueEnabled_returnTrue() {
        setVibrateOn(true)

        assertThat(getMainSwitchPreference().isChecked).isTrue()
    }

    @Test
    fun checked_valueDisabled_returnFalse() {
        setVibrateOn(false)

        assertThat(getMainSwitchPreference().isChecked).isFalse()
    }

    @Test
    fun click_updatesCorrectly() {
        setVibrateOn(null)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
    }

    @Test
    fun click_withVibrator_playsHapticPreviewWhenChecked() {
        setVibrateOn(null)
        val widget = getMainSwitchPreference()

        assertThat(widget.isChecked).isTrue()

        widget.performClick()

        assertThat(widget.isChecked).isFalse()
        verify(vibratorSpy, never()).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())

        widget.performClick()

        assertThat(widget.isChecked).isTrue()
        verify(vibratorSpy).vibrate(any<VibrationEffect>(), any<VibrationAttributes>())
    }

    private fun getMainSwitchPreference(): MainSwitchPreference =
        preference.createAndBindWidget(context)

    private fun setVibrateOn(enabled: Boolean?) =
        preference.storage(context).setBoolean(VIBRATE_ON, enabled)
}
// LINT.ThenChange(VibrationMainSwitchPreferenceControllerTest.java)
