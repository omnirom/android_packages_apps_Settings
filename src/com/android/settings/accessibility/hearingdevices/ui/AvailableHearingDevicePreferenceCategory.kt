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

import android.bluetooth.BluetoothProfile
import android.content.Context
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.accessibility.AvailableHearingDeviceUpdater
import com.android.settings.accessibility.HearingAidUtils
import com.android.settings.bluetooth.BluetoothDeviceUpdater
import com.android.settings.bluetooth.Utils
import com.android.settingslib.bluetooth.BluetoothCallback
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.metadata.PreferenceLifecycleContext

class AvailableHearingDevicePreferenceCategory(
    context: Context,
    val metricsCategory: Int,
    key: String = "available_hearing_devices",
    title: Int = R.string.accessibility_hearing_device_connected_title,
) : HearingDevicePreferenceCategory(key, title), BluetoothCallback {
    private val localBluetoothManager: LocalBluetoothManager? by lazy {
        Utils.getLocalBluetoothManager(context)
    }
    private var fragmentManager: FragmentManager? = null

    override fun createDeviceUpdater(context: Context): BluetoothDeviceUpdater? =
        AvailableHearingDeviceUpdater(context, this, metricsCategory)

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        fragmentManager = context.childFragmentManager
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        localBluetoothManager?.eventManager?.registerCallback(this)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        localBluetoothManager?.eventManager?.unregisterCallback(this)
    }

    override fun onActiveDeviceChanged(
        activeDevice: CachedBluetoothDevice?,
        bluetoothProfile: Int,
    ) {
        if (activeDevice == null) {
            return
        }

        if (bluetoothProfile == BluetoothProfile.HEARING_AID) {
            HearingAidUtils.launchHearingAidPairingDialog(
                fragmentManager,
                activeDevice,
                metricsCategory,
            )
        }
    }
}
