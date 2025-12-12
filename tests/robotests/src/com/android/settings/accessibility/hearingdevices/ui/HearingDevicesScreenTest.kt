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

package com.android.settings.accessibility.hearingdevices.ui

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import com.android.settings.R
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.shadow.ShadowBluetoothAdapter
import com.android.settings.testutils.shadow.ShadowBluetoothUtils
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.CachedBluetoothDeviceManager
import com.android.settingslib.bluetooth.HapClientProfile
import com.android.settingslib.bluetooth.HearingAidInfo
import com.android.settingslib.bluetooth.HearingAidProfile
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowLog

@Config(shadows = [ShadowBluetoothAdapter::class, ShadowBluetoothUtils::class])
class HearingDevicesScreenTest : SettingsCatalystTestCase() {

    val mockAshaProfile = mock<HearingAidProfile>()
    val mockHapClientProfile = mock<HapClientProfile>()
    val mockProfileManager =
        mock<LocalBluetoothProfileManager> {
            on { hearingAidProfile } doReturn mockAshaProfile
            on { hapClientProfile } doReturn mockHapClientProfile
        }
    val mockDeviceLeft = mock<BluetoothDevice> { on { isConnected } doReturn true }
    val mockDeviceRight = mock<BluetoothDevice> { on { isConnected } doReturn true }
    val mockCachedDeviceLeft =
        mock<CachedBluetoothDevice> {
            on { name } doReturn TEST_DEVICE_NAME
            on { deviceSide } doReturn HearingAidInfo.DeviceSide.SIDE_LEFT
            on { device } doReturn mockDeviceLeft
        }
    val mockCachedDeviceRight =
        mock<CachedBluetoothDevice> {
            on { name } doReturn TEST_DEVICE_NAME
            on { deviceSide } doReturn HearingAidInfo.DeviceSide.SIDE_RIGHT
            on { device } doReturn mockDeviceRight
        }
    val mockDeviceManager =
        mock<CachedBluetoothDeviceManager> {
            on { findDevice(mockDeviceLeft) } doReturn mockCachedDeviceLeft
            on { findDevice(mockDeviceRight) } doReturn mockCachedDeviceRight
        }
    val mockLocalBluetoothManager =
        mock<LocalBluetoothManager> {
            on { profileManager } doReturn mockProfileManager
            on { cachedDeviceManager } doReturn mockDeviceManager
        }
    val mockPreferenceLifecycleContext = mock<PreferenceLifecycleContext>()
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    init {
        ShadowBluetoothUtils.sLocalBluetoothManager = mockLocalBluetoothManager
        val shadowAdapter: ShadowBluetoothAdapter = Shadow.extract(bluetoothAdapter)
        shadowAdapter.apply {
            addSupportedProfiles(BluetoothProfile.HEARING_AID)
            addSupportedProfiles(BluetoothProfile.HAP_CLIENT)
        }
    }

    override val preferenceScreenCreator = HearingDevicesScreen(appContext)
    override val flagName: String = Flags.FLAG_CATALYST_HEARING_DEVICES

    @Before
    fun setUp() {
        bluetoothAdapter?.enable()
        ShadowLog.stream = System.out
    }

    @Test override fun migration() {}

    @Test
    fun getSummary_connectedAshaHearingAidRightSide_connectedRightSideSummary() {
        mockAshaProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceRight) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Right only")
    }

    @Test
    fun getSummary_connectedAshaHearingAidBothSide_connectedBothSideSummary() {
        mockCachedDeviceLeft.stub { on { subDevice } doReturn mockCachedDeviceRight }
        mockAshaProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceLeft) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Left and right")
    }

    @Test
    fun getSummary_connectedLeHearingAidRightSide_connectedRightSideSummary() {
        mockHapClientProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceRight) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Right only")
    }

    @Test
    fun getSummary_connectedLeHearingAidBothSide_connectedBothSideSummary() {
        mockCachedDeviceLeft.stub { on { memberDevice } doReturn setOf(mockCachedDeviceRight) }
        mockHapClientProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceLeft) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Left and right")
    }

    @Test
    fun getSummary_connectedLeHearingAidMonoSide_connectedBothSideSummary() {
        mockCachedDeviceLeft.stub {
            on { deviceSide } doReturn HearingAidInfo.DeviceSide.SIDE_MONO
            on { memberDevice } doReturn setOf()
        }
        mockHapClientProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceLeft) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME active")
    }

    @Test
    fun getSummary_connectedMultipleHearingAids_connectedMultipleDevicesSummary() {
        mockHapClientProfile.stub {
            on { connectedDevices } doReturn listOf(mockDeviceLeft, mockDeviceRight)
        }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME +1 more")
    }

    @Test
    fun getSummary_bluetoothOff_disconnectedSummary() {
        bluetoothAdapter?.disable()

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getText(R.string.accessibility_hearingaid_not_connected_summary))
    }

    @Test
    fun onServiceConnected_onHearingAidProfileConnected_updateSummary() {
        mockAshaProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceLeft) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onServiceConnected()

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Left only")
    }

    @Test
    fun onServiceConnected_onHapClientProfileConnected_updateSummary() {
        mockHapClientProfile.stub { on { connectedDevices } doReturn listOf(mockDeviceRight) }

        preferenceScreenCreator.onCreate(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onStart(mockPreferenceLifecycleContext)
        preferenceScreenCreator.onServiceConnected()

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo("$TEST_DEVICE_NAME / Right only")
    }

    companion object {
        const val TEST_DEVICE_NAME = "TEST_HEARING_AID_BT_DEVICE_NAME"
    }
}
