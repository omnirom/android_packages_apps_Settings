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
import com.android.settings.appfunctions.providers.DeviceStateExecutorResult
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates the collection and transformation of the device state setter results.
 *
 * This class executes from multiple [DeviceStateExecutor]s in parallel, aggregates the results to
 * produce the final [SetDeviceStateItemResponse].
 *
 * @property executors The list of [DeviceStateExecutor]s to call for setting device state.
 */
@Keep
class DeviceStateSetterAggregator(private val executors: List<DeviceStateExecutor>) :
    DeviceStateAggregator(executors) {
    /**
     * Aggregates device state setting results from all registered providers.
     *
     * @param appFunctionType The device state app function to fetch, passed to each provider.
     * @param deviceLocale The current locale of the device, included in the final response.
     * @return A [DeviceStateResponse] containing the fully aggregated device state.
     */
    override suspend fun aggregate(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
        deviceLocale: String,
    ): SetDeviceStateItemResponse {
        val executorResults = coroutineScope {
            executors
                .map { executor ->
                    async {
                        executor.execute(appFunctionType, params) as DeviceStateSetterExecutorResult
                    }
                }
                .map { it.await() }
        }

        val validResults = executorResults.mapNotNull { it.result }

        return when (validResults.size) {
            0 -> throw IllegalStateException("No valid executor found for $appFunctionType")
            // TODO: use commented logic once we properly implement setters in catalyst, at the
            //  moment we're always returning a dummy result from catalyst so will cause an
            //  exception if combined with any result from AndroidApiStateSetterExecutor implemented
            //  by OEMs.
            else -> validResults.first()
        //            1 -> validResults.first()
        //            else -> throw IllegalStateException("Multiple executors found for
        // $appFunctionType" +
        //                    "with params $params")
        }
    }
}

/**
 * Represents the self-contained result from a single [DeviceStateExecutor]. This is an immutable
 * data class.
 */
@Keep
data class DeviceStateSetterExecutorResult(val result: SetDeviceStateItemResponse?) :
    DeviceStateExecutorResult()
