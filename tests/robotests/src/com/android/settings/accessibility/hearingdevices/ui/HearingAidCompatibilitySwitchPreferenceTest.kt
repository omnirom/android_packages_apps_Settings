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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.media.AudioManager
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowAudioManager
import org.robolectric.shadows.ShadowTelephonyManager

@RunWith(RobolectricTestRunner::class)
class HearingAidCompatibilitySwitchPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = HearingAidCompatibilitySwitchPreference(context)
    private val telephonyManager: ShadowTelephonyManager =
        shadowOf(context.getSystemService(TelephonyManager::class.java))
    private val audioManager: ShadowAudioManager =
        shadowOf(context.getSystemService(AudioManager::class.java))

    @Test
    fun isAvailable_hacSupported_returnTrue() {
        telephonyManager.setHearingAidCompatibilitySupported(true)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_hacNotSupported_returnFalse() {
        telephonyManager.setHearingAidCompatibilitySupported(false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun storageSetTrue_shouldEnableHac() {
        preference.storage(context).setBoolean(preference.key, true)

        assertThat(audioManager.getParameter("HACSetting")).isEqualTo("ON")
    }

    @Test
    fun storageSetFalse_shouldEnableHac() {
        preference.storage(context).setBoolean(preference.key, false)

        assertThat(audioManager.getParameter("HACSetting")).isEqualTo("OFF")
    }
}
