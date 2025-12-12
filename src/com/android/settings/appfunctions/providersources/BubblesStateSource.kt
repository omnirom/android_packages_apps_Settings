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
import android.provider.Settings.Global.NOTIFICATION_BUBBLES
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.notification.BubbleHelper
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

class BubblesStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val isEnabled =
            Settings.Global.getInt(
                context.contentResolver,
                NOTIFICATION_BUBBLES,
                BubbleHelper.SYSTEM_WIDE_ON,
            ) == BubbleHelper.SYSTEM_WIDE_ON

        val item =
            DeviceStateItem(
                key = "global_notification_bubbles",
                purpose = "global_notification_bubbles",
                jsonValue = isEnabled.toString(),
            )

        return listOf(
            PerScreenDeviceStates(description = "Bubbles", deviceStateItems = listOf(item))
        )
    }
}
