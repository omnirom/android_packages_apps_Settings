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
import androidx.annotation.Keep
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

/**
 * Retrieves a specific aspect of the device's current state. The actual state details are
 * encapsulated within the [PerScreenDeviceStates] structure.
 */
@Keep
interface DeviceStateSource {
    /**
     * The app function this device state source belongs to. Each implementation must define its
     * specific app function.
     */
    val appFunctionType: DeviceStateAppFunctionType

    /**
     * Retrieves the [PerScreenDeviceStates] for the current context. If no specific states are
     * applicable or found, it may return an object with an empty list of states or default values.
     *
     * @param context The Android [android.content.Context] which might be needed to access system
     *   services or resources.
     * @param sharedDeviceStateData Data shared by multiple [DeviceStateSource]s, which is computed
     *   lazily.
     * @return A [PerScreenDeviceStates] object. This object might contain an empty list of states
     *   or have specific default values if no relevant states are found or applicable.
     */
    suspend fun get(
        context: Context,
        sharedDeviceStateData: SharedDeviceStateData,
    ): List<PerScreenDeviceStates>
}
