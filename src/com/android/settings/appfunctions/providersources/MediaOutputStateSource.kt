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

package com.android.settings.appfunctions.providersources

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.media.AudioManager
import com.android.settings.R
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.bluetooth.Utils
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.LocalBluetoothProfileManager
import com.android.settingslib.media.LocalMediaManager
import com.android.settingslib.media.PhoneMediaDevice
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class MediaOutputStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val audioManager = context.getSystemService(AudioManager::class.java)
        val localMediaManager = LocalMediaManager(context, /* packageName= */ null)

        val audioMode: Int = audioManager!!.mode

        val summary: String =
            if (isOngoingCall(audioMode, context)) {
                context.getString(R.string.media_out_summary_ongoing_call_state)
            } else {
                val connectedDeviceName = localMediaManager.currentConnectedDevice?.name
                val activeBluetoothDeviceName = findActiveBluetoothDevice(context, audioMode)?.alias
                val defaultSummary = context.getString(R.string.media_output_default_summary)

                connectedDeviceName ?: activeBluetoothDeviceName ?: defaultSummary
            }

        val item =
            DeviceStateItem(key = "media_output", purpose = "media_output", jsonValue = summary)
        return listOf(
            PerScreenDeviceStates(
                description = "Sound & vibration",
                deviceStateItems = listOf(item),
            )
        )
    }

    private fun isOngoingCall(audioMode: Int, context: Context): Boolean {
        return (audioMode == AudioManager.MODE_RINGTONE ||
            audioMode == AudioManager.MODE_IN_CALL ||
            audioMode == AudioManager.MODE_IN_COMMUNICATION) &&
            !PhoneMediaDevice.inputRoutingEnabledAndIsDesktop(context)
    }

    private fun findActiveBluetoothDevice(context: Context, audioMode: Int): BluetoothDevice? {
        val localBluetoothManager = Utils.getLocalBtManager(context)
        val profileManager = localBluetoothManager?.profileManager

        if (audioMode == AudioManager.MODE_NORMAL) {
            val connectedA2dpDevices: List<BluetoothDevice> =
                getConnectedA2dpDevices(profileManager)
            val connectedHADevices: List<BluetoothDevice> =
                getConnectedHearingAidDevices(profileManager)
            val connectedLeAudioDevices: List<BluetoothDevice> =
                getConnectedLeAudioDevices(localBluetoothManager, profileManager)

            if (
                connectedA2dpDevices.isNotEmpty() ||
                    connectedHADevices.isNotEmpty() ||
                    connectedLeAudioDevices.isNotEmpty()
            ) {
                return findActiveDevice(profileManager)
            }
        }
        return null
    }

    /**
     * get A2dp devices on all states (STATE_DISCONNECTED, STATE_CONNECTING, STATE_CONNECTED,
     * STATE_DISCONNECTING)
     */
    private fun getConnectedA2dpDevices(
        profileManager: LocalBluetoothProfileManager?
    ): List<BluetoothDevice> = profileManager?.a2dpProfile?.connectedDevices ?: emptyList()

    /** get hearing aid profile connected device, exclude other devices with same hiSyncId. */
    private fun getConnectedHearingAidDevices(
        profileManager: LocalBluetoothProfileManager?
    ): List<BluetoothDevice> {
        val hapProfile = profileManager?.hearingAidProfile ?: return emptyList()

        return hapProfile.connectedDevices
            .filter { it.isConnected } // Ensure device is actually connected
            .distinctBy { hapProfile.getHiSyncId(it) }
    }

    /** Get LE Audio profile connected devices */
    private fun getConnectedLeAudioDevices(
        localBluetoothManager: LocalBluetoothManager?,
        profileManager: LocalBluetoothProfileManager?,
    ): List<BluetoothDevice> {
        val leAudioProfile = profileManager?.leAudioProfile ?: return emptyList()
        val devicesFromProfile = leAudioProfile.connectedDevices ?: return emptyList()

        return devicesFromProfile.filter { device ->
            device != null &&
                device.isConnected() &&
                isDeviceInCachedList(device, localBluetoothManager)
        }
    }

    private fun isDeviceInCachedList(
        device: BluetoothDevice?,
        localBluetoothManager: LocalBluetoothManager?,
    ): Boolean {
        if (device == null || localBluetoothManager == null) {
            return false
        }
        val cachedDevices = localBluetoothManager.cachedDeviceManager.cachedDevicesCopy
        return cachedDevices.any { it.device == device }
    }

    private fun findActiveDevice(profileManager: LocalBluetoothProfileManager?): BluetoothDevice? {
        return findActiveHearingAidDevice(profileManager)
            ?: findActiveLeAudioDevice(profileManager)
            ?: profileManager?.a2dpProfile?.activeDevice // or .getActiveDevice()
    }

    private fun findActiveHearingAidDevice(
        profileManager: LocalBluetoothProfileManager?
    ): BluetoothDevice? {
        val hearingAidProfile = profileManager?.hearingAidProfile ?: return null
        val activeDevices = hearingAidProfile.getActiveDevices()
        return activeDevices?.firstOrNull { it != null }
    }

    private fun findActiveLeAudioDevice(
        profileManager: LocalBluetoothProfileManager?
    ): BluetoothDevice? {
        return profileManager?.leAudioProfile?.getActiveDevices()?.firstOrNull()
    }
}
