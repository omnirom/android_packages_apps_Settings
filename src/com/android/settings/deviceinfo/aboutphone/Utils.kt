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
import android.content.Context
import android.net.wifi.SoftApConfiguration
import android.net.wifi.WifiManager
import android.provider.Settings.Global.DEVICE_NAME
import androidx.lifecycle.ViewModel
import com.android.settings.deviceinfo.DeviceNamePreferenceController.getFilteredBluetoothString
import com.android.settings.wifi.utils.wifiManager
import com.android.settingslib.datastore.SettingsGlobalStore

fun Context.updateDeviceName(deviceName: String) {
    SettingsGlobalStore.get(this).setString(DEVICE_NAME, deviceName)
    BluetoothAdapter.getDefaultAdapter()?.setName(getFilteredBluetoothString(deviceName))
    val manager: WifiManager = wifiManager ?: return
    manager.softApConfiguration =
        SoftApConfiguration.Builder(manager.softApConfiguration).setSsid(deviceName).build()
}

class DeviceInfoViewModel : ViewModel() {
    var deviceName: String? = null

    fun clearDeviceNme() {
        deviceName = null
    }
}
