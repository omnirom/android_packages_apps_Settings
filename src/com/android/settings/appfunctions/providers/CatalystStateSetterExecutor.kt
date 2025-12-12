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
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateSetterExecutorResult
import com.android.settings.appfunctions.GenericDeviceStateItemSetterParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse

/**
 * A [DeviceStateExecutor] that sets device state for Settings that are exposed using Catalyst
 * framework. Configured in [CatalystStateProviderConfig].
 */
class CatalystStateSetterExecutor() : DeviceStateExecutor {

    /**
     * Asynchronously executes the device state set request.
     *
     * @param appFunctionType The app function type requested by the caller. The executor will only
     *   execute if it matches the requested type.
     * @param params The required params to execute the set request.
     * @return A [DeviceStateSetterExecutorResult] containing the outcome of the set operation.
     */
    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateSetterExecutorResult {
        try {
            if (params == null) {
                throw IllegalArgumentException("Provided params are null.")
            }
            var result = executeSetDeviceStateRequest(appFunctionType, params)

            // TODO: replace with actual result
            var dummyResult =
                SetDeviceStateItemResponse(isSuccessful = true, currentValue = "currentValue")
            return DeviceStateSetterExecutorResult(result = dummyResult)
        } catch (e: Exception) {
            Log.e(TAG, "error executing $appFunctionType", e)
            return DeviceStateSetterExecutorResult(result = null)
        }
    }

    private fun executeSetDeviceStateRequest(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument,
    ): SetDeviceStateItemResponse? {
        Log.i(TAG, "Executing a setDeviceStateRequest with appFunctionType: $appFunctionType")
        val parsedParams = GenericDeviceStateItemSetterParams(appFunctionType, params)
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> setDeviceState(parsedParams)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                offsetNumericDeviceStateByValue(parsedParams)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                adjustNumericDeviceStateByPercentage(parsedParams)
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE ->
                toggleDeviceStateItemParams(parsedParams)
            else -> {
                Log.i(TAG, "Unrecognised appFunctionType: $appFunctionType")
                return null
            }
        }
    }

    private fun setDeviceState(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getSetDeviceStateItemParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun offsetNumericDeviceStateByValue(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getOffsetNumericDeviceStateItemByValueParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun adjustNumericDeviceStateByPercentage(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getAdjustNumericDeviceStateItemByPercentageParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    private fun toggleDeviceStateItemParams(
        genericParams: GenericDeviceStateItemSetterParams
    ): SetDeviceStateItemResponse? {
        val params = genericParams.getToggleDeviceStateItemParams()
        // TODO: call into appropriate setter APIs

        return null
    }

    companion object {
        private const val TAG = "CatalystStateSetterExecutor"
    }
}
