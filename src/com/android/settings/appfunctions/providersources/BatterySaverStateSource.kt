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
import android.provider.Settings
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class BatterySaverStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_BATTERY

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val areRemindersEnabled =
            Settings.Global.getInt(
                context.getContentResolver(),
                Settings.Global.LOW_POWER_MODE_REMINDER_ENABLED,
                1,
            ) == 1

        val item =
            DeviceStateItem(
                key = "battery_saver_reminders",
                purpose = "battery_saver_reminders",
                jsonValue = areRemindersEnabled.toString(),
            )

        return listOf(
            PerScreenDeviceStates(description = "Battery Saver", deviceStateItems = listOf(item))
        )
    }
}
