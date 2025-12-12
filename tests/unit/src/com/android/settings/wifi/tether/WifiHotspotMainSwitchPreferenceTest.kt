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
import android.net.wifi.WifiManager
import androidx.lifecycle.LiveData
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.wifi.factory.WifiFeatureProvider
import com.android.settings.wifi.repository.WifiHotspotRepository
import com.android.settingslib.datastore.KeyValueStore
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class WifiHotspotMainSwitchPreferenceTest {

    private val mockWifiManager = mock<WifiManager>()
    private val mockWifiHotspotStore = mock<KeyValueStore>()
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

    private val preference = WifiHotspotMainSwitchPreference(mockWifiHotspotStore)

    @Before
    fun setUp() {
        provider.stub { on { wifiHotspotRepository } doReturn mockWifiHotspotRepository }
        mockWifiHotspotRepository.stub { on { isRestarting } doReturn false }
    }

    @Test
    fun isAvailable_sapEnabled_returnTrue() {
        mockWifiManager.stub { on { wifiApState } doReturn WifiManager.WIFI_AP_STATE_ENABLED }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isNotRestarting_returnTrue() {
        mockWifiManager.stub { on { wifiApState } doReturn WifiManager.WIFI_AP_STATE_ENABLING }
        mockWifiHotspotRepository.stub { on { isRestarting } doReturn false }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_isRestarting_returnFalse() {
        mockWifiManager.stub { on { wifiApState } doReturn WifiManager.WIFI_AP_STATE_ENABLING }
        mockWifiHotspotRepository.stub { on { isRestarting } doReturn true }

        assertThat(preference.isAvailable(context)).isFalse()
    }
}