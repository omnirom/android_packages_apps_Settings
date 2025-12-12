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

package com.android.settings.network.tether

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkPolicyManager
import android.net.TetheringManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.eq
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BluetoothTetherSwitchPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val tetheringManager: TetheringManager =
        spy(appContext.getSystemService(TetheringManager::class.java))
    private val connectivityManager: ConnectivityManager =
        spy(appContext.getSystemService(ConnectivityManager::class.java))
    private val networkPolicyManager: NetworkPolicyManager =
        spy(appContext.getSystemService(NetworkPolicyManager::class.java))
    private val context: Context =
        object : ContextWrapper(appContext) {
            override fun getApplicationContext() = this

            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(TetheringManager::class.java) -> tetheringManager
                    getSystemServiceName(ConnectivityManager::class.java) -> connectivityManager
                    getSystemServiceName(NetworkPolicyManager::class.java) -> networkPolicyManager
                    else -> super.getSystemService(name)
                }
        }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var preference: BluetoothTetherSwitchPreference

    @Before
    fun setUp() {
        bluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter())
        bluetoothAdapter.stub { on { state } doReturn BluetoothAdapter.STATE_ON }
        preference = BluetoothTetherSwitchPreference(bluetoothAdapter)
    }

    @Test
    fun isEnabled_bluetoothTurningOn_returnFalse() {
        bluetoothAdapter.stub { on { state } doReturn BluetoothAdapter.STATE_TURNING_ON }

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_bluetoothTurningOff_returnFalse() {
        bluetoothAdapter.stub { on { state } doReturn BluetoothAdapter.STATE_TURNING_OFF }

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isEnabled_dataSaverDisabled_returnTrue() {
        networkPolicyManager.stub { on { restrictBackground } doReturn false }

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_dataSaverEnabled_returnFalse() {
        networkPolicyManager.stub { on { restrictBackground } doReturn true }

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun isAvailable_noTetherableBluetoothRegexs_returnFalse() {
        doReturn(emptyArray<String>()).whenever(tetheringManager).tetherableBluetoothRegexs

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasTetherableBluetoothRegexs_returnTrue() {
        doReturn(arrayOf("bt-pan")).whenever(tetheringManager).tetherableBluetoothRegexs

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun storageSetOn_bluetoothOff_enableBluetooth() {
        bluetoothAdapter.stub { on { state } doReturn BluetoothAdapter.STATE_OFF }

        preference.storage(context).setBoolean(preference.key, true)

        verify(bluetoothAdapter).enable()
    }

    @Test
    fun storageSetOn_startBtTethering() {
        preference.storage(context).setBoolean(preference.key, true)

        verify(connectivityManager)
            .startTethering(eq(ConnectivityManager.TETHERING_BLUETOOTH), anyBoolean(), any(), any())
    }

    @Test
    fun storageSetOff_stopBtTethering() {
        preference.storage(context).setBoolean(preference.key, false)

        verify(connectivityManager).stopTethering(eq(ConnectivityManager.TETHERING_BLUETOOTH))
    }
}
