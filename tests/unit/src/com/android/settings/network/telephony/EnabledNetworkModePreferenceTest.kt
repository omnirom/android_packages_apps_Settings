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

package com.android.settings.network.telephony

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test

class EnabledNetworkModePreferenceTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private var mobileNetworkData = MobileNetworkData(context, null, 0)

    private lateinit var preference: EnabledNetworkModePreference

    @Before
    fun setUp() {
        mobileNetworkData.enabledNetworkModeFlow.value =
            EnabledNetworkModeData(isAvailable = true, summary = NETWORK_MODE)
        preference = EnabledNetworkModePreference(mobileNetworkData)
    }

    @Test
    fun isAvailable_returnTrue() {
        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_returnFalse() {
        mobileNetworkData.enabledNetworkModeFlow.value =
            EnabledNetworkModeData(isAvailable = false, summary = NETWORK_MODE)
        preference = EnabledNetworkModePreference(mobileNetworkData)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getSummary_returnNetworkMode() {
        assertThat(preference.getSummary(context)).isEqualTo(NETWORK_MODE)
    }

    companion object {
        private const val NETWORK_MODE = "5G"
    }
}
