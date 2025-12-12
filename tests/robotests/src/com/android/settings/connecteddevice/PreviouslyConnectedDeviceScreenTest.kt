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

package com.android.settings.connecteddevice

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager.FEATURE_BLUETOOTH
import com.android.settings.R
import com.android.settings.Settings.PreviouslyConnectedDeviceActivity
import com.android.settings.bluetooth.SavedBluetoothDeviceUpdater
import com.android.settings.connecteddevice.dock.DockUpdater
import com.android.settings.flags.Flags
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.whenever
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowBluetoothAdapter

@Config(shadows = [ShadowBluetoothAdapter::class])
class PreviouslyConnectedDeviceScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = PreviouslyConnectedDeviceScreen()

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_CONNECTED_DEVICES_25Q4

    private val bluetoothManager = appContext.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: ShadowBluetoothAdapter = shadowOf(bluetoothManager?.adapter)

    @Mock private val mConnectedDockUpdater: DockUpdater? = null

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(PreviouslyConnectedDeviceActivity::class.java.getName())
    }

    @Test
    fun isAvailable_bluetoothNotSupported_returnsFalse() {
        setSavedDockUpdater(null)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_BLUETOOTH, false)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(false)
    }

    @Test
    fun isAvailable_bluetoothSupported_returnsTrue() {
        setSavedDockUpdater(null)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_BLUETOOTH, true)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(true)
    }

    @Test
    fun isAvailable_updaterNotNull_returnsTrue() {
        val updater = object : DockUpdater {}
        setSavedDockUpdater(updater)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_BLUETOOTH, false)

        assertThat(preferenceScreenCreator.isAvailable(appContext)).isEqualTo(true)
    }

    @Test
    fun getSummary_bluetoothEnabled_isEmpty() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_ON)

        assertThat(preferenceScreenCreator.getSummary(appContext)).isEqualTo("")
    }

    @Test
    fun getSummary_bluetoothDisabled_showTurnOnAction() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_OFF)

        assertThat(preferenceScreenCreator.getSummary(appContext))
            .isEqualTo(appContext.getString(R.string.connected_device_see_all_summary))
    }

    @Test
    @Config(shadows = [ShadowSavedBluetoothDeviceUpdater::class])
    override fun migration() {
        bluetoothAdapter.setState(BluetoothAdapter.STATE_ON)
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_BLUETOOTH, true)
        super.migration()
    }

    private fun setSavedDockUpdater(updater: DockUpdater? = null) {
        val fakeFeatureFactory = FakeFeatureFactory.setupForTest()
        val mockFeatureProvider = fakeFeatureFactory.dockUpdaterFeatureProvider
        whenever(mockFeatureProvider.getSavedDockUpdater(appContext, preferenceScreenCreator))
            .thenReturn(updater)
    }
}

@Implements(SavedBluetoothDeviceUpdater::class)
class ShadowSavedBluetoothDeviceUpdater {
    @Implementation fun forceUpdate() {}
}
