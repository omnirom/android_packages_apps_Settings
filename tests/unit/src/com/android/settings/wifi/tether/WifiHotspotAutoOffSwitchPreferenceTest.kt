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

package com.android.settings.wifi.tether

import android.content.ContextWrapper
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settings.wifi.repository.WifiHotspotRepository
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

class WifiHotspotAutoOffSwitchPreferenceTest {
    private val mockWifiManager = mock<WifiManager>()
    private val mockWifiHotspotRepository = mock<WifiHotspotRepository>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    else -> super.getSystemService(name)
                }
        }

    private var provider: WifiFeatureProvider =
        FakeFeatureFactory.setupForTest().wifiFeatureProvider

    private val preference = WifiHotspotAutoOffSwitchPreference()

    @Before
    fun setUp() {
        provider.stub { on { wifiHotspotRepository } doReturn mockWifiHotspotRepository }
        mockWifiHotspotRepository.stub {
            on { isSpeedFeatureAvailable } doReturn true
            on { isDualBand } doReturn true
        }
    }

    @Test
    fun getValue_configIsEnabled_returnTrue() {
        val sapConfig = SoftApConfiguration.Builder().setAutoShutdownEnabled(true).build()
        mockWifiManager.stub { on { softApConfiguration } doReturn sapConfig }

        assertThat(preference.storage(context).getBoolean(KEY)).isTrue()
    }

    @Test
    fun getValue_configIsDisabled_returnFalse() {
        val sapConfig = SoftApConfiguration.Builder().setAutoShutdownEnabled(false).build()
        mockWifiManager.stub { on { softApConfiguration } doReturn sapConfig }

        assertThat(preference.storage(context).getBoolean(KEY)).isFalse()
    }

    @Test
    fun setValue_valueTrue_setConfigTrue() {
        val sapConfig = SoftApConfiguration.Builder().setAutoShutdownEnabled(false).build()
        mockWifiManager.stub { on { softApConfiguration } doReturn sapConfig }

        preference.storage(context).setBoolean(KEY, true)

        val captor = argumentCaptor<SoftApConfiguration>()
        verify(mockWifiManager).softApConfiguration = captor.capture()
        assertThat(captor.firstValue.isAutoShutdownEnabled).isTrue()
    }

    @Test
    fun setValue_valueFalse_setConfigFalse() {
        val sapConfig = SoftApConfiguration.Builder().setAutoShutdownEnabled(true).build()
        mockWifiManager.stub { on { softApConfiguration } doReturn sapConfig }

        preference.storage(context).setBoolean(KEY, false)

        val captor = argumentCaptor<SoftApConfiguration>()
        verify(mockWifiManager).softApConfiguration = captor.capture()
        assertThat(captor.firstValue.isAutoShutdownEnabled).isFalse()
    }

    companion object {
        const val KEY = WifiHotspotAutoOffSwitchPreference.KEY
    }
}