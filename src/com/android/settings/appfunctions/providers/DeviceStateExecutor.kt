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

package com.android.settings.appfunctions.providers

import android.app.appsearch.GenericDocument
import androidx.annotation.Keep
import com.android.settings.appfunctions.DeviceStateAppFunctionType

/**
 * Defines the contract for any class that provides or changes device state information.
 *
 * Implementations are responsible for fetching/setting data from a single, well-defined source,
 * such as the Catalyst graph, Android system settings, or a specific OEM API. These providers are
 * designed to be stateless and fully testable in isolation.
 */
@Keep
interface DeviceStateExecutor {
    /**
     * Asynchronously executes the device state request.
     *
     * @param appFunctionType The app function type requested by the caller. The executor should
     *   only execute if it matches the requested id.
     * @return A [DeviceStateExecutorResult] containing the required information.
     */
    suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument? = null,
    ): DeviceStateExecutorResult
}

/**
 * Represents the self-contained result from a single [DeviceStateExecutor]. This is an immutable
 * data class.
 */
@Keep abstract class DeviceStateExecutorResult
