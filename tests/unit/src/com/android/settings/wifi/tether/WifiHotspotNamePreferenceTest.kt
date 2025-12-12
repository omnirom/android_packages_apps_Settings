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
import android.net.TetheringManager
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.wifi.WifiUtils
import com.android.settingslib.datastore.KeyValueStore
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class WifiHotspotNamePreferenceTest {

    private val mockWifiManager = mock<WifiManager>()
    private val mockTetheringManager = mock<TetheringManager>()
    private val mockWifiHotspotStore = mock<KeyValueStore>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    getSystemServiceName(TetheringManager::class.java) -> mockTetheringManager
                    else -> super.getSystemService(name)
                }
        }

    private var testScope = TestScope(StandardTestDispatcher())
    private val sapConfig = SoftApConfiguration.Builder().setSsid(SAP_SSID).build()

    private lateinit var preference: WifiHotspotNamePreference

    @Before
    fun setUp() {
        mockWifiManager.stub { on { softApConfiguration } doReturn sapConfig }
        preference = WifiHotspotNamePreference(context, testScope, mockWifiHotspotStore)
    }

    @Test
    fun isAvailable_canNotShowWifiHotspot_returnFalse() {
        WifiUtils.setCanShowWifiHotspotCached(false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_canShowWifiHotspot_returnTrue() {
        WifiUtils.setCanShowWifiHotspotCached(true)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun getSummary_returnSapSsid() {
        assertThat(preference.getSummary(context)).isEqualTo(SAP_SSID)
    }

    @Test
    fun onPreferenceChange_valueNoChange_returnFalse() {
        assertThat(preference.onPreferenceChange(Preference(context), SAP_SSID)).isFalse()
    }

    @Test
    fun onPreferenceChange_valueChange_returnTrue() {
        assertThat(preference.onPreferenceChange(Preference(context), "new_$SAP_SSID")).isTrue()
    }

    companion object {
        private const val SAP_SSID = "sap_ssid"
    }
}