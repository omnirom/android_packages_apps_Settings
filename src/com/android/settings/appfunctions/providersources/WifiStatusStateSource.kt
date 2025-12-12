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

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Provides device state information related to saved Wi-Fi networks. */
class WifiStatusStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_MOBILE_DATA

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        return listOf(
            PerScreenDeviceStates(
                description = "Saved Networks",
                deviceStateItems = getDeviceStateItems(context),
            )
        )
    }

    suspend fun getDeviceStateItems(context: Context): List<DeviceStateItem> {
        return withContext(Dispatchers.IO) {
            try {
                val wifiManager =
                    context.getSystemService(WifiManager::class.java)
                        ?: run {
                            Log.e(TAG, "WifiManager is not available.")
                            return@withContext emptyList()
                        }

                val configuredNetworks: List<WifiConfiguration> =
                    wifiManager.configuredNetworks ?: emptyList()
                val currentWifiInfo: WifiInfo? = wifiManager.connectionInfo

                if (configuredNetworks.isEmpty()) {
                    Log.d(TAG, "No saved Wi-Fi networks found.")
                }

                configuredNetworks
                    .filter { !it.SSID.isNullOrBlank() }
                    .distinctBy { it.SSID }
                    .flatMap { network ->
                        val networkId = network.networkId
                        val ssid = network.SSID?.removeSurrounding("\"") ?: "Unknown SSID"
                        val isConnected =
                            currentWifiInfo?.networkId == networkId &&
                                currentWifiInfo.ssid?.removeSurrounding("\"") == ssid

                        listOf(
                            DeviceStateItem(
                                key = "wifi_name_$networkId",
                                jsonValue = ssid,
                                hintText = "Wifi name of saved network",
                            ),
                            DeviceStateItem(
                                key = "is_connected_$networkId",
                                jsonValue = isConnected.toString(),
                                hintText = "Whether currently connected to $ssid",
                            ),
                            DeviceStateItem(
                                key = "auto_connect_$networkId",
                                jsonValue = network.allowAutojoin.toString(),
                                hintText = "Whether $ssid is set to auto-connect",
                            ),
                        )
                    }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException while getting configured networks", e)
                emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error getting saved Wi-Fi networks", e)
                emptyList()
            }
        }
    }

    companion object {
        private const val TAG = "WifiStatusStateSource"
    }
}
