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

package com.android.settings.fuelgauge.batteryusage

import android.content.Context
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.providersources.DeviceStateSource
import com.android.settings.appfunctions.providersources.SharedDeviceStateData
import com.android.settingslib.Utils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

class BatteryUsageStateSource :
    DeviceStateSource, DataProcessManager.OnBatteryDiffDataMapLoadedListener {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_BATTERY

    private val batteryLevelDataFlow = MutableStateFlow<BatteryLevelData?>(null)
    private val batteryDiffDataMapFlow =
        MutableStateFlow<MutableMap<Long?, BatteryDiffData?>?>(null)
    var batteryLevelData: BatteryLevelData? = null
    var batteryDiffDataMap: MutableMap<Long?, BatteryDiffData?>? = null

    override fun onBatteryDiffDataMapLoaded(
        batteryDiffDataMap: MutableMap<Long?, BatteryDiffData?>?
    ) {
        Log.d(TAG, "batteryDiffDataMap：start")
        this.batteryDiffDataMap = batteryDiffDataMap
        batteryDiffDataMapFlow.value = batteryDiffDataMap
    }

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        return listOf(
            PerScreenDeviceStates(
                description = "Battery Usage",
                deviceStateItems = getDeviceStateItems(context),
            )
        )
    }

    private suspend fun getDeviceStateItems(context: Context): List<DeviceStateItem> {
        batteryLevelData =
            DataProcessManager.getBatteryLevelData(
                context,
                null,
                UserIdsSeries(context, /* isNonUIRequest= */ false),
                /* isFromPeriodJob= */ false,
                this,
            )
        batteryLevelDataFlow.value = batteryLevelData

        Log.d(TAG, "Wait for state flow ready...")
        val combinedData =
            batteryLevelDataFlow
                .combine(batteryDiffDataMapFlow) { levelData, diffMap ->
                    if (levelData != null && diffMap != null) {
                        Pair(levelData, diffMap)
                    } else {
                        null
                    }
                }
                .filterNotNull()
                .first()

        val currentBatteryLevelData = combinedData.first
        val currentBatteryDiffDataMap = combinedData.second

        Log.d(TAG, "Got state flows updated.")
        return convertUsageDataToDeviceStateItems(
            context,
            currentBatteryLevelData,
            currentBatteryDiffDataMap,
        )
    }

    private fun convertUsageDataToDeviceStateItems(
        context: Context,
        batteryLevelData: BatteryLevelData?,
        batteryDiffDataMap: MutableMap<Long?, BatteryDiffData?>?,
    ): List<DeviceStateItem> {
        if (batteryLevelData == null || batteryDiffDataMap == null) {
            Log.e(TAG, "Battery level data or diff data map is null")
            return emptyList()
        }
        val batteryUsageMap =
            DataProcessor.generateBatteryUsageMap(context, batteryDiffDataMap, batteryLevelData)
        if (batteryUsageMap == null) {
            Log.e(TAG, "Failed to generate batteryUsageMap")
            return emptyList()
        }
        val totalBatteryUsageMap = batteryUsageMap[-1]?.get(-1)
        val screenOnTimeMs = totalBatteryUsageMap?.screenOnTime ?: 0L

        val deviceStateItems = mutableListOf<DeviceStateItem>()
        deviceStateItems.add(
            DeviceStateItem(
                key = "screen_time_since_last_full_charge",
                purpose = "screen_time_since_last_full_charge",
                jsonValue = screenOnTimeMs.millisToSecondsString(),
            )
        )
        totalBatteryUsageMap?.appDiffEntryList?.forEach { appEntry ->
            val packageName = appEntry.packageName
            // skip "System apps"
            if (packageName.isNullOrEmpty()) {
                return@forEach
            }
            val appName = appEntry.appLabel
            val percentage = appEntry.percentage + appEntry.adjustPercentageOffset
            val screenMs = appEntry.mScreenOnTimeInMs
            val backgroundMs =
                appEntry.mBackgroundUsageTimeInMs + appEntry.mForegroundServiceUsageTimeInMs

            deviceStateItems.add(
                DeviceStateItem(
                    key = "battery_usage_screen_time_package_$packageName",
                    purpose = "battery_usage_screen_time_package_$packageName",
                    jsonValue = screenMs.millisToSecondsString(),
                    hintText = "App: $appName",
                )
            )

            deviceStateItems.add(
                DeviceStateItem(
                    key = "battery_usage_background_time_package_$packageName",
                    purpose = "battery_usage_background_time_package_$packageName",
                    jsonValue = backgroundMs.millisToSecondsString(),
                    hintText = "App: $appName",
                )
            )

            deviceStateItems.add(
                DeviceStateItem(
                    key = "battery_usage_percentage_package_$packageName",
                    purpose = "battery_usage_percentage_package_$packageName",
                    jsonValue = Utils.formatPercentage(percentage, true),
                    hintText = "App: $appName",
                )
            )
        }
        return deviceStateItems
    }

    private fun Long.millisToSecondsString(): String {
        return TimeUnit.MILLISECONDS.toSeconds(this).toString()
    }

    companion object {
        private const val TAG = "BatteryUsageScreenTime"
    }
}
