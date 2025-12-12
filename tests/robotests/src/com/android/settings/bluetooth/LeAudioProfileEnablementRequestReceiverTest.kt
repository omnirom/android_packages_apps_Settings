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

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.content.ContextCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.flags.Flags
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper.shadowMainLooper

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(shadows = [ShadowBluetoothUtils::class])
class LeAudioProfileEnablementRequestReceiverTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()
    @get:Rule val setFlagsRule: SetFlagsRule = SetFlagsRule()
    private lateinit var testScope: TestScope
    private lateinit var dispatcher: CoroutineDispatcher

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()
    @Mock private lateinit var localBtManager: LocalBluetoothManager
    @Mock private lateinit var deviceManager: CachedBluetoothDeviceManager
    @Mock private lateinit var device: BluetoothDevice
    @Mock private lateinit var cachedDevice: CachedBluetoothDevice

    private val underTest = LeAudioProfileEnablementRequestReceiver()

    @Before
    fun setUp() {
        dispatcher = UnconfinedTestDispatcher()
        testScope = TestScope(dispatcher)
        ShadowBluetoothUtils.sLocalBluetoothManager = localBtManager
        whenever(localBtManager.cachedDeviceManager).thenReturn(deviceManager)
        ContextCompat.registerReceiver(
            context,
            underTest,
            IntentFilter(CHANGE_LE_AUDIO_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CHANGE_LE_PROFILE_BROADCAST_RECEIVER)
    fun testEnableLeAudio() =
        testScope.runTest {
            whenever(localBtManager.cachedDeviceManager.findDevice(device)).thenReturn(cachedDevice)
            ShadowBluetoothUtils.setLeAudioEnabled(localBtManager, cachedDevice, false)

            context.sendBroadcast(
                Intent(CHANGE_LE_AUDIO_ACTION).apply {
                    putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    putExtra("NEW_STATE", true)
                }
            )
            shadowMainLooper().idle()

            assertThat(ShadowBluetoothUtils.isLeAudioEnabled(cachedDevice)).isTrue()
        }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_CHANGE_LE_PROFILE_BROADCAST_RECEIVER)
    fun testDisableLeAudio() =
        testScope.runTest {
            whenever(localBtManager.cachedDeviceManager.findDevice(device)).thenReturn(cachedDevice)
            ShadowBluetoothUtils.setLeAudioEnabled(localBtManager, cachedDevice, true)

            context.sendBroadcast(
                Intent(CHANGE_LE_AUDIO_ACTION).apply {
                    putExtra(BluetoothDevice.EXTRA_DEVICE, device)
                    putExtra("NEW_STATE", false)
                }
            )
            shadowMainLooper().idle()

            assertThat(ShadowBluetoothUtils.isLeAudioEnabled(cachedDevice)).isFalse()
        }

    companion object {
        private const val CHANGE_LE_AUDIO_ACTION =
            "com.android.settings.bluetooth.UPDATE_LE_AUDIO_PROFILE"
    }
}
