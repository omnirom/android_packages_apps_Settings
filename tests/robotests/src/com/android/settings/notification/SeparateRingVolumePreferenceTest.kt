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

package com.android.settings.notification

import android.content.ContextWrapper
import android.media.AudioManager
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class SeparateRingVolumePreferenceTest {
    private val audioHelper = mock<AudioHelper>()
    private val audioManager = mock<AudioManager>()
    private val vibrator = mock<Vibrator>()
    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(Vibrator::class.java) -> vibrator
                    AUDIO_SERVICE -> audioManager
                    else -> super.getSystemService(name)
                }
        }

    @Test
    fun isAvailable_singleVolume_shouldReturnFalse() {
        audioHelper.stub { on { isSingleVolume } doReturn true }
        var ringVolumePreference = SeparateRingVolumePreference(audioHelper)

        assertThat(ringVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_noSingleVolume_shouldReturnTrue() {
        audioHelper.stub { on { isSingleVolume } doReturn false }
        var ringVolumePreference = SeparateRingVolumePreference(audioHelper)

        assertThat(ringVolumePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun getEffectiveRingerMode_noVibratorAndVibrateMode_shouldReturnSilentMode() {
        vibrator.stub { on { hasVibrator() } doReturn false }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getEffectiveRingerMode()).isEqualTo(RINGER_MODE_SILENT)
    }

    @Test
    fun getEffectiveRingerMode_hasVibratorAndVibrateMode_shouldReturnVibrateMode() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getEffectiveRingerMode()).isEqualTo(RINGER_MODE_VIBRATE)
    }

    @Test
    fun getEffectiveRingerMode_hasVibratorAndNormalMode_shouldReturnNormalMode() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_NORMAL }

        assertThat(context.getEffectiveRingerMode()).isEqualTo(RINGER_MODE_NORMAL)
    }

    @Test
    fun getIconRes_normalMode_shouldReturnRingVolumeIcon() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_NORMAL }

        assertThat(context.getIconRes()).isEqualTo(R.drawable.ic_ring_volume)
    }

    @Test
    fun getMuteIcon_vibrateMode_shouldReturnVibrateIcon() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getIconRes()).isEqualTo(R.drawable.ic_volume_ringer_vibrate)
    }

    @Test
    fun getMuteIcon_silentMode_shouldReturnSilentIcon() {
        vibrator.stub { on { hasVibrator() } doReturn false }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getIconRes()).isEqualTo(R.drawable.ic_ring_volume_off)
    }

    @Test
    fun getContentDescription_normalMode_shouldReturnTitleDescription() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_NORMAL }

        assertThat(context.getContentDescription())
            .isEqualTo(context.getString(R.string.separate_ring_volume_option_title))
    }

    @Test
    fun getContentDescription_vibrateMode_shouldReturnVibrateModeDescription() {
        vibrator.stub { on { hasVibrator() } doReturn true }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getContentDescription())
            .isEqualTo(context.getString(R.string.ringer_content_description_vibrate_mode))
    }

    @Test
    fun getContentDescription_silentMode_shouldReturnSilentModeDescription() {
        vibrator.stub { on { hasVibrator() } doReturn false }
        audioManager.stub { on { ringerModeInternal } doReturn RINGER_MODE_VIBRATE }

        assertThat(context.getContentDescription())
            .isEqualTo(context.getString(R.string.ringer_content_description_silent_mode))
    }
}
// LINT.ThenChange(SeparateRingVolumePreferenceControllerTest.java)
