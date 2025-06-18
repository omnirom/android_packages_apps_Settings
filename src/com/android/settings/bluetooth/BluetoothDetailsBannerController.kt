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

import android.content.Context
import android.widget.TextView
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settingslib.bluetooth.BluetoothUtils
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.core.lifecycle.Lifecycle
import com.android.settingslib.widget.LayoutPreference

class BluetoothDetailsBannerController(
    private val context: Context,
    fragment: PreferenceFragmentCompat,
    private val cachedDevice: CachedBluetoothDevice,
    lifecycle: Lifecycle,
) : BluetoothDetailsController(context, fragment, cachedDevice, lifecycle) {
    private lateinit var pref: LayoutPreference

    override fun getPreferenceKey(): String = KEY_BLUETOOTH_DETAILS_BANNER

    override fun init(screen: PreferenceScreen) {
        pref = screen.findPreference(KEY_BLUETOOTH_DETAILS_BANNER) ?: return
    }

    override fun refresh() {
        pref.findViewById<TextView>(R.id.bluetooth_details_banner_message).text =
            context.getString(R.string.device_details_key_missing_title, cachedDevice.name)
    }

    override fun isAvailable(): Boolean =
        BluetoothUtils.getKeyMissingCount(cachedDevice.device)?.let { it > 0 } ?: false

    private companion object {
        const val KEY_BLUETOOTH_DETAILS_BANNER: String = "bluetooth_details_banner"
    }
}
