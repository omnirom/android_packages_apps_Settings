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

package com.android.settings.wifi.tether

import android.content.ContextWrapper
import android.net.TetheringManager
import android.net.TetheringManager.TETHERING_WIFI
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WIFI_AP_STATE_DISABLED
import android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED
import androidx.test.core.app.ApplicationProvider
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

class WifiHotspotScreenTest : SettingsCatalystTestCase() {
    private val mockWifiManager = mock<WifiManager>()
    private val mockTetheringManager = mock<TetheringManager>()

    override val preferenceScreenCreator
        get() = WifiHotspotScreen(context)

    override val flagName: String
        get() = Flags.FLAG_CATALYST_TETHER_SETTINGS

    @Test
    override fun migration() {}

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

    @Test
    fun getValue_defaultOn_returnOn() {
        mockWifiManager.stub { on { wifiApState } doReturn WIFI_AP_STATE_ENABLED }

        val getValue = preferenceScreenCreator.storage(context).getBoolean(WifiHotspotScreen.KEY)

        assertThat(getValue).isTrue()
    }

    @Test
    fun getValue_defaultOff_returnOff() {
        mockWifiManager.stub { on { wifiApState } doReturn WIFI_AP_STATE_DISABLED }

        val getValue = preferenceScreenCreator.storage(context).getBoolean(WifiHotspotScreen.KEY)

        assertThat(getValue).isFalse()
    }

    @Test
    fun setValue_valueOn_startTethering() {
        preferenceScreenCreator.storage(context).setBoolean(WifiHotspotScreen.KEY, true)

        verify(mockTetheringManager).startTethering(eq(TETHERING_WIFI), any(), any())
    }

    @Test
    fun setValue_valueOff_stopTethering() {
        preferenceScreenCreator.storage(context).setBoolean(WifiHotspotScreen.KEY, false)

        verify(mockTetheringManager).stopTethering(eq(TETHERING_WIFI))
    }
}
