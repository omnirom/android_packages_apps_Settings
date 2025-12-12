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
import java.util.concurrent.TimeUnit

class ScreenTimeoutStateSource : DeviceStateSource {
    override val appFunctionType: DeviceStateAppFunctionType =
        DeviceStateAppFunctionType.GET_UNCATEGORIZED

    override suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates> {
        val screenTimeoutMilliseconds =
            Settings.System.getLong(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE,
            )
        val screenTimeoutSeconds = TimeUnit.MILLISECONDS.toSeconds(screenTimeoutMilliseconds)

        val item =
            DeviceStateItem(
                key = "screen_timeout",
                purpose = "screen_timeout",
                jsonValue = "$screenTimeoutSeconds s",
            )

        return listOf(
            PerScreenDeviceStates(description = "Screen timeout", deviceStateItems = listOf(item))
        )
    }

    private companion object {
        /**
         * This value comes from
         * [com.android.settings.display.ScreenTimeoutSettings.FALLBACK_SCREEN_TIMEOUT_VALUE].
         */
        const val FALLBACK_SCREEN_TIMEOUT_VALUE = 30000L
    }
}
