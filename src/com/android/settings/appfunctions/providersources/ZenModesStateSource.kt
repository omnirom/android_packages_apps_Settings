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

import android.app.AutomaticZenRule
import android.content.Context
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settingslib.notification.modes.ZenModesBackend
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class ZenModesStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        var isDndActive = false
        var isBedtimeActive = false
        for (mode in ZenModesBackend.getInstance(context).getModes()) {
            if (mode.isActive()) {
                when {
                    mode.isManualDnd() -> isDndActive = true
                    mode.type == AutomaticZenRule.TYPE_BEDTIME -> isBedtimeActive = true
                }
            }
        }

        val dndItem =
            DeviceStateItem(
                key = "zen_mode_dnd_active",
                purpose = "zen_mode_dnd_active",
                jsonValue = isDndActive.toString(),
            )
        val bedtimeItem =
            DeviceStateItem(
                key = "zen_mode_bedtime_active",
                purpose = "zen_mode_bedtime_active",
                jsonValue = isBedtimeActive.toString(),
            )

        return listOf(
            PerScreenDeviceStates(
                description = "Modes",
                deviceStateItems = listOf(dndItem, bedtimeItem),
            )
        )
    }
}
