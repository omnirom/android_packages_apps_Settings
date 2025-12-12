/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.bluetooth

import android.content.Context
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.KEY_HEARING_DEVICE_GROUP
import com.android.settings.bluetooth.BluetoothDetailsHearingDeviceController.ORDER_HEARING_AIDS_PRESETS
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.HapClientProfile
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.hearingdevices.ui.PresetUiController
import com.android.settingslib.core.instrumentation.Instrumentable
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.utils.ThreadUtils

/**
 * A controller for managing preset preference for Bluetooth hearing devices.
 *
 * This class handles the creation and management of a [PresetPreference] within the Bluetooth
 * device details screen. It coordinates with a [PresetUiController] to handle the business logic
 * for presets and ensures the UI reflects the device's state. It only shows the preset controls if
 * the device supports the HAP Client profile.
 */
class BluetoothDetailsPresetPreferenceController(
    context: Context,
    private val bluetoothManager: LocalBluetoothManager,
    fragment: PreferenceFragmentCompat,
    cachedDevice: CachedBluetoothDevice,
    lifecycle: Lifecycle,
) : BluetoothDetailsController(context, fragment, cachedDevice, lifecycle) {

    private var presetUiController: PresetUiController? = null

    public override fun init(screen: PreferenceScreen) {
        val deviceControls = screen.findPreference<PreferenceCategory>(KEY_HEARING_DEVICE_GROUP)
        if (deviceControls == null) {
            return
        }
        val presetControls =
            PresetPreference(deviceControls.context).apply {
                key = KEY_HEARING_AIDS_PRESETS
                order = ORDER_HEARING_AIDS_PRESETS
                if (mFragment is Instrumentable) {
                    setMetricsCategory(mFragment.metricsCategory)
                }
            }
        deviceControls.addPreference(presetControls)
        presetUiController =
            PresetUiController(mContext, bluetoothManager, presetControls).apply {
                loadDevice(mCachedDevice)
            }
    }

    override fun refresh() {
        presetUiController?.let { controller ->
            if (isAvailable) {
                controller.refresh()
            }
        }
    }

    override fun getPreferenceKey(): String {
        return KEY_HEARING_AIDS_PRESETS
    }

    override fun onResume() {
        presetUiController?.let { controller ->
            ThreadUtils.postOnBackgroundThread { controller.start() }
        }
    }

    override fun onPause() {
        presetUiController?.let { controller ->
            ThreadUtils.postOnBackgroundThread { controller.stop() }
        }
    }

    override fun isAvailable(): Boolean {
        return mCachedDevice.profiles.any { it is HapClientProfile }
    }

    companion object {
        const val KEY_HEARING_AIDS_PRESETS = "hearing_aids_presets"
    }
}
