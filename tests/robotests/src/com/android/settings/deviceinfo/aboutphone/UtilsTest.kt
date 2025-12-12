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

package com.android.settings.deviceinfo.aboutphone

import android.bluetooth.BluetoothAdapter
import android.content.ContextWrapper
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.provider.Settings.Global.DEVICE_NAME
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settingslib.datastore.SettingsGlobalStore
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class UtilsTest {

    private val mockWifiManager = mock<WifiManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(WifiManager::class.java) -> mockWifiManager
                    else -> super.getSystemService(name)
                }
        }

    private val configuration = SoftApConfiguration.Builder().setSsid(TEST_SSID).build()

    @Test
    fun updateDeviceName_assginTestDeviceName_appliedSettedName() {
        mockWifiManager.stub { on { getSoftApConfiguration() } doReturn configuration }

        context.updateDeviceName(TEST_DEVICE_NAME)

        assertThat(SettingsGlobalStore.get(context).getString(DEVICE_NAME))
            .isEqualTo(TEST_DEVICE_NAME)
        assertThat(BluetoothAdapter.getDefaultAdapter()?.name).isEqualTo(TEST_DEVICE_NAME)
    }

    companion object {
        private val TEST_SSID = "test-ssid"
        private val TEST_DEVICE_NAME = "Terepan_Device"
    }
}
