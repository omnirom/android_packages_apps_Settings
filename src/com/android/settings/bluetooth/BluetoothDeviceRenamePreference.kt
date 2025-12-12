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

import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.connecteddevice.BluetoothDataStore
import com.android.settings.connecteddevice.BluetoothPreference
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class BluetoothDeviceRenamePreference(private val bluetoothDataStore: BluetoothDataStore) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceClickListener {

    private lateinit var lifeCycleContext: PreferenceLifecycleContext

    private var broadcastReceiver: BroadcastReceiver? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.bluetooth_device_name

    override fun dependencies(context: Context) = arrayOf(BluetoothPreference.KEY)

    override fun onCreate(context: PreferenceLifecycleContext) {
        lifeCycleContext = context
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    context.notifyPreferenceChange(KEY)
                }
            }
        context.registerReceiver(
            receiver,
            IntentFilter().apply {
                addAction(ACTION_LOCAL_NAME_CHANGED)
            },
        )
        broadcastReceiver = receiver
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        broadcastReceiver?.let {
            context.unregisterReceiver(it)
            broadcastReceiver = null
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        FeatureFactory.featureFactory.metricsFeatureProvider
            .action(preference.context, SettingsEnums.ACTION_BLUETOOTH_RENAME)
        LocalDeviceNameDialogFragment().show(
            lifeCycleContext.childFragmentManager,
            LocalDeviceNameDialogFragment.TAG
        )
        return true
    }

    override fun isAvailable(context: Context): Boolean {
        return bluetoothDataStore.bluetoothAdapter?.isEnabled == true
    }

    override fun getSummary(context: Context): CharSequence? {
        return bluetoothDataStore.bluetoothAdapter?.name
    }

    companion object {
        const val KEY = "bluetooth_screen_bt_pair_rename_devices"
    }
}
// LINT.ThenChange(BluetoothDeviceRenamePreferenceController.java)
