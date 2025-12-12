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

package com.android.settings.bluetooth.ui.view

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.view.View
import com.android.settings.R
import com.android.settings.bluetooth.BluetoothDetailsAudioDeviceTypeController
import com.android.settings.bluetooth.BluetoothDetailsConfigurableFragment
import com.android.settings.bluetooth.BluetoothDetailsProfilesController
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settingslib.core.AbstractPreferenceController

class DeviceDetailsMoreSettingsFragment : BluetoothDetailsConfigurableFragment() {

    override fun getMetricsCategory(): Int = SettingsEnums.BLUETOOTH_DEVICE_DETAILS_MORE_SETTINGS

    override fun getPreferenceScreenResId(): Int {
        return R.xml.bluetooth_device_more_settings_fragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requestUpdateLayout(listOf(LOADING_PREF))
        requestUpdateLayout(FragmentTypeModel.DeviceDetailsMoreSettingsFragment)
    }

    override fun createPreferenceControllers(context: Context): List<AbstractPreferenceController> {
        return if (isCachedDeviceInitialized()) {
            listOf(
                BluetoothDetailsProfilesController(
                    context,
                    this,
                    localBluetoothManager,
                    cachedDevice,
                    settingsLifecycle,
                ),
                BluetoothDetailsAudioDeviceTypeController(
                    context,
                    this,
                    localBluetoothManager,
                    cachedDevice,
                    settingsLifecycle,
                ),
            )
        } else {
            emptyList()
        }
    }

    override fun getLogTag(): String = TAG

    companion object {
        const val TAG: String = "DeviceMoreSettingsFrg"
        const val KEY_DEVICE_ADDRESS: String = "device_address"
    }
}
