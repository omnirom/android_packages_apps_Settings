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
package com.android.settings.connecteddevice

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.BluetoothDashboardActivity
import com.android.settings.bluetooth.BluetoothDeviceRenamePreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(BluetoothDashboardScreen.KEY)
open class BluetoothDashboardScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.bluetooth_settings_title

    override val icon: Int
        get() = R.drawable.ic_settings_bluetooth

    override fun getMetricsCategory() = SettingsEnums.BLUETOOTH_FRAGMENT

    override val highlightMenuKey: Int
        get() = R.string.menu_key_connected_devices

    override fun isFlagEnabled(context: Context) = Flags.catalystBluetoothSwitchbarScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = BluetoothDashboardFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, BluetoothDashboardActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val bluetoothDataStore = BluetoothPreference.createDataStore(context)
            +BluetoothPreference(bluetoothDataStore)
            if (Flags.deeplinkConnectedDevices25q4()) {
                +BluetoothDeviceRenamePreference(bluetoothDataStore)
            }
            +BluetoothFooterPreference(bluetoothDataStore)
        }

    companion object {
        const val KEY = "bluetooth_switchbar_screen"
    }
}
