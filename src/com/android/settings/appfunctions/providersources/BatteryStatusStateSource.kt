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
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.fuelgauge.BatteryUtils
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class BatteryStatusStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_BATTERY

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val batteryUtils = BatteryUtils.getInstance(context)
        val batteryInfo = batteryUtils.getBatteryInfo(TAG)

        val statusLabel = batteryInfo.statusLabel
        val isCharging = !batteryInfo.discharging
        val timeRemainingLabel = if (!isCharging) batteryInfo.remainingLabel else null
        val chargedByLabel =
            if (isCharging) batteryInfo.remainingLabel ?: batteryInfo.statusLabel else null

        val batteryStatusItem =
            DeviceStateItem(
                key = "battery_status",
                purpose = "battery_status",
                jsonValue = statusLabel,
            )
        val batteryTimeRemainingItem =
            DeviceStateItem(
                key = "battery_time_remaining",
                purpose = "battery_time_remaining",
                jsonValue = timeRemainingLabel.toString(),
            )
        val batteryChargedByItem =
            DeviceStateItem(
                key = "battery_charged_by",
                purpose = "battery_charged_by",
                jsonValue = chargedByLabel.toString(),
            )

        return listOf(
            PerScreenDeviceStates(
                description = "Battery status",
                deviceStateItems =
                    listOf(batteryStatusItem, batteryTimeRemainingItem, batteryChargedByItem),
            )
        )
    }

    companion object {
        private const val TAG = "BatteryChargingStateSource"
    }
}
