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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

// LINT.IfChange
class MediaVibrationIntensitySwitchPreferenceTest : VibrationIntensitySwitchPreferenceTestCase() {
    override val hasRingerModeDependency = false
    override val preference = MediaVibrationIntensitySwitchPreference(context, "key", "main_key")

    private val resourcesSpy: Resources =
        spy(ApplicationProvider.getApplicationContext<Context>().resources)

    private val contextWrapper: Context =
        object : ContextWrapper(context) {
            override fun getResources(): Resources = resourcesSpy
        }

    @Test
    fun isAvailable_mediaPreferenceNotSupported_unavailable() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_media_vibration_supported) } doReturn false
        }
        assertThat(preference.isAvailable(contextWrapper)).isFalse()
    }

    @Test
    fun isAvailable_mediaPreferenceSupported_available() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_media_vibration_supported) } doReturn true
        }
        assertThat(preference.isAvailable(contextWrapper)).isTrue()
    }
}
// LINT.ThenChange(MediaVibrationTogglePreferenceControllerTest.java)
