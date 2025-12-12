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
import android.content.res.Resources
import android.media.AudioManager
import android.media.AudioManager.STREAM_BLUETOOTH_SCO
import android.media.AudioManager.STREAM_VOICE_CALL
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
@RunWith(AndroidJUnit4::class)
class CallVolumePreferenceTest {
    private val mockResources = mock<Resources>()

    private val audioManager = mock<AudioManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    AUDIO_SERVICE -> audioManager
                    else -> super.getSystemService(name)
                }

            override fun getResources(): Resources = mockResources
        }

    @Test
    fun isAvailable_configTrueAndNoSingleVolume_shouldReturnTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        val audioHelper = mock<AudioHelper> { on { isSingleVolume } doReturn false }
        val callVolumePreference = CallVolumePreference(audioHelper)

        assertThat(callVolumePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_configTrueAndSingleVolume_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        val audioHelper = mock<AudioHelper> { on { isSingleVolume } doReturn true }
        val callVolumePreference = CallVolumePreference(audioHelper)

        assertThat(callVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_configFalse_shouldReturnFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }
        val callVolumePreference = CallVolumePreference(context)

        assertThat(callVolumePreference.isAvailable(context)).isFalse()
    }

    @Test
    @Suppress("DEPRECATION")
    fun getAudioStream_onBluetoothScoOn_shouldEqualToStreamBluetoothSco() {
        audioManager.stub { on { isBluetoothScoOn } doReturn true }
        val callVolumePreference = CallVolumePreference(context)

        assertThat(callVolumePreference.getAudioStream(context)).isEqualTo(STREAM_BLUETOOTH_SCO)
    }

    @Test
    @Suppress("DEPRECATION")
    fun getAudioStream_onBluetoothScoOff_shouldEqualToStreamVoiceCall() {
        audioManager.stub { on { isBluetoothScoOn } doReturn false }
        val callVolumePreference = CallVolumePreference(context)

        assertThat(callVolumePreference.getAudioStream(context)).isEqualTo(STREAM_VOICE_CALL)
    }
}
// LINT.ThenChange(CallVolumePreferenceControllerTest.java)
