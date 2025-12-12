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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.settings.flags.Flags
import kotlinx.coroutines.DelicateCoroutinesApi

/** Receiver to enable/disable LE Audio profile. */
@OptIn(DelicateCoroutinesApi::class)
class LeAudioProfileEnablementRequestReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!Flags.enableChangeLeProfileBroadcastReceiver()) {
            return
        }
        val action: String = intent.action ?: return
        if (action != ACTION) {
            return
        }
        val bluetoothDevice: BluetoothDevice =
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                ?: return
        val newState: Boolean = intent.getBooleanExtra(EXTRA_NEW_STATE, false)
        val requester: String? = intent.getStringExtra(EXTRA_REQUESTER)

        Log.d(
            TAG,
            "Set ${bluetoothDevice.anonymizedAddress} LeAudio enabled to $newState, requester: $requester",
        )
        val localBtManager = Utils.getLocalBtManager(context)
        val cachedDevice = localBtManager.cachedDeviceManager.findDevice(bluetoothDevice) ?: return
        Utils.setLeAudioEnabled(localBtManager, cachedDevice, newState)
    }

    companion object {
        private const val TAG = "LeAudioEnableReceiver"
        private const val ACTION = "com.android.settings.bluetooth.UPDATE_LE_AUDIO_PROFILE"
        private const val EXTRA_NEW_STATE = "NEW_STATE"
        private const val EXTRA_REQUESTER = "REQUESTER"
    }
}
