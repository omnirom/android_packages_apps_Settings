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
import android.util.FeatureFlagUtils
import android.util.FeatureFlagUtils.SETTINGS_AUDIO_ROUTING
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioRoutingPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = AudioRoutingPreference()

    @Test
    fun isAvailable_audioRoutingNotSupported_returnFalse() {
        FeatureFlagUtils.setEnabled(context, SETTINGS_AUDIO_ROUTING, false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getAvailabilityStatus_audioRoutingNotSupported_available() {
        FeatureFlagUtils.setEnabled(context, SETTINGS_AUDIO_ROUTING, true)

        assertThat(preference.isAvailable(context)).isTrue()
    }
}
