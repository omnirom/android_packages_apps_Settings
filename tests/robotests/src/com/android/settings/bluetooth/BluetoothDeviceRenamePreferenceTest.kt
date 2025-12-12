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

package com.android.settings.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.connecteddevice.BluetoothPreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(AndroidJUnit4::class)
class BluetoothDeviceRenamePreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothDeviceRenamePreference: BluetoothDeviceRenamePreference
    private val mockLifecycleContext = mock<PreferenceLifecycleContext>()

    @Before
    fun setUp() {
        bluetoothAdapter = spy(BluetoothAdapter.getDefaultAdapter())
        whenever(bluetoothAdapter.isEnabled).thenReturn(true)
        bluetoothDeviceRenamePreference =
            BluetoothDeviceRenamePreference(
                BluetoothPreference.createDataStore(
                    context,
                    bluetoothAdapter
                )
            )
    }

    @Test
    fun isAvailable_bluetoothEnabled_returnTrue() {
        assertThat(bluetoothDeviceRenamePreference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_bluetoothDisabled_returnFalse() {
        whenever(bluetoothAdapter.isEnabled).thenReturn(false)

        assertThat(bluetoothDeviceRenamePreference.isAvailable(context)).isFalse()
    }

    @Test
    fun onStart_registerReceiver() {
        bluetoothDeviceRenamePreference.onStart(mockLifecycleContext)

        verify(mockLifecycleContext).registerReceiver(any(), any())
    }

    @Test
    fun onStop_unregisterReceiver() {
        bluetoothDeviceRenamePreference.onStart(mockLifecycleContext)
        bluetoothDeviceRenamePreference.onStop(mockLifecycleContext)

        verify(mockLifecycleContext).unregisterReceiver(any())
    }

    @Test
    fun getSummary_bluetoothName() {
        whenever(bluetoothAdapter.name).thenReturn(BLUETOOTH_NAME)
        val summary = bluetoothDeviceRenamePreference.getSummary(context)

        assertThat(summary).isEqualTo(BLUETOOTH_NAME)
    }

    private companion object {
        const val BLUETOOTH_NAME = "name"
    }
}
