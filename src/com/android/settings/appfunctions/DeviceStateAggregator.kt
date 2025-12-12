/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions

import android.app.appsearch.GenericDocument
import androidx.annotation.Keep
import com.android.settings.appfunctions.providers.DeviceStateExecutor
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse

/**
 * Orchestrates the collection and transformation of device state information.
 *
 * This class executes from multiple [DeviceStateExecutor]s in parallel, aggregates the results to
 * produce the final [DeviceStateResponse].
 *
 * @property executors The list of [DeviceStateExecutor]s to query for device state.
 */
@Keep
abstract class DeviceStateAggregator(private val executors: List<DeviceStateExecutor>) {
    /**
     * Aggregates device state from all registered providers.
     *
     * This function performs the following steps:
     * 1. Calls all [DeviceStateExecutor]s concurrently to gather device state information.
     * 2. Combines the states and hint text from all provider results.
     * 3. Constructs and returns the final [DeviceStateResponse].
     *
     * @param appFunctionType The device state app function to fetch, passed to each provider.
     * @param deviceLocale The current locale of the device, included in the final response.
     * @return A [DeviceStateResponse] containing the fully aggregated device state.
     */
    abstract suspend fun aggregate(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
        deviceLocale: String,
    ): Any
}
