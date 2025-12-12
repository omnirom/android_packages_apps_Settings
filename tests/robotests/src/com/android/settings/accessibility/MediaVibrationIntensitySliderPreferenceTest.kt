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

import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub

// LINT.IfChange
class MediaVibrationIntensitySliderPreferenceTest : VibrationIntensitySliderPreferenceTestCase() {
    override val hasRingerModeDependency = false
    override val preference = MediaVibrationIntensitySliderPreference(context)

    @Test
    fun isAvailable_mediaPreferenceNotSupported_unavailable() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_media_vibration_supported) } doReturn false
        }
        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_mediaPreferenceSupported_available() {
        resourcesSpy.stub {
            on { getBoolean(R.bool.config_media_vibration_supported) } doReturn true
        }
        assertThat(preference.isAvailable(context)).isTrue()
    }
}
// LINT.ThenChange(MediaVibrationIntensityPreferenceControllerTest.java)
