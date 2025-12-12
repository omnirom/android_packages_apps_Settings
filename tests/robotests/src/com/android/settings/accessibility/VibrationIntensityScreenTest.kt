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
import android.content.res.Resources
import android.media.AudioManager
import android.os.Vibrator
import androidx.core.content.getSystemService
import com.android.settings.R.integer.config_vibration_supported_intensity_levels
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils.shadow.ShadowAudioManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// LINT.IfChange
@Config(shadows = [ShadowAudioManager::class, SettingsShadowResources::class])
class VibrationIntensityScreenTest : SettingsCatalystTestCase() {
    private lateinit var vibratorMock: Vibrator

    private val resourcesSpy: Resources = spy(appContext.resources)

    private val context: Context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any? =
                when {
                    name == VIBRATOR_SERVICE -> vibratorMock
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = resourcesSpy
        }

    override val preferenceScreenCreator = VibrationIntensityScreen()

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
            on { getInteger(config_vibration_supported_intensity_levels) } doReturn 3
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndSingleIntensityLevel_unavailable() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(config_vibration_supported_intensity_levels) } doReturn 1
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasVibratorAndMultipleIntensityLevels_available() {
        vibratorMock = mock { on { hasVibrator() } doReturn true }
        resourcesSpy.stub {
            on { getInteger(config_vibration_supported_intensity_levels) } doReturn 2
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    override fun migration() {
        // make screen available
        shadowOf(appContext.getSystemService(Vibrator::class.java)).setHasVibrator(true)
        SettingsShadowResources.overrideResource(config_vibration_supported_intensity_levels, 2)
        super.migration()
    }

    private fun setRingerMode(ringerMode: Int) {
        val audioManager = context.getSystemService<AudioManager>()
        audioManager?.ringerModeInternal = ringerMode
        assertThat(audioManager?.ringerModeInternal).isEqualTo(ringerMode)
    }
}
// LINT.ThenChange(VibrationPreferenceControllerTest.java)
