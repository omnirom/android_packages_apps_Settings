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

package com.android.settings.deviceinfo.firmwareversion

import android.content.Context
import android.content.res.Resources
import android.sysprop.TelephonyProperties
import android.telephony.TelephonyManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.robolectric.RobolectricTestRunner

// LINT.IfChange
@RunWith(RobolectricTestRunner::class)
class BasebandVersionPreferenceTest {
    private val mockTelephonyManager = mock<TelephonyManager>()
    private val mockResources = mock<Resources>()

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(TelephonyManager::class.java) } doReturn mockTelephonyManager
            on { getSystemService(Context.TELEPHONY_SERVICE) } doReturn mockTelephonyManager
            on { resources } doReturn mockResources
        }

    private val basebandVersionPreference = BasebandVersionPreference()

    @Before
    fun setup() {
        // By default, available
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn true
        }
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn true
        }
    }

    @Test
    fun isAvailable_default_available() {
        TelephonyProperties.baseband_version(listOf("test"))
        assertThat(basebandVersionPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noShowSimInfo_unavailable() {
        mockResources.stub {
            on { getBoolean(R.bool.config_show_sim_info) } doReturn false
        }
        assertThat(basebandVersionPreference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_voiceCapable_notDataCapable_available() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn true
        }
        assertThat(basebandVersionPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_notVoiceCapable_dataCapable_available() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn true
            on { isDeviceVoiceCapable } doReturn false
        }
        assertThat(basebandVersionPreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_notVoiceCapable_notDataCapable_unavailable() {
        mockTelephonyManager.stub {
            on { isDataCapable } doReturn false
            on { isDeviceVoiceCapable } doReturn false
        }
        assertThat(basebandVersionPreference.isAvailable(context)).isFalse()
    }
}
// LINT.ThenChange(BasebandVersionPreferenceControllerTest.java)
