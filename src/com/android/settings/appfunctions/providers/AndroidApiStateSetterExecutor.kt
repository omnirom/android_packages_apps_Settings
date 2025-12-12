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
import android.content.Context
import android.util.Log
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateSetterExecutorResult
import com.android.settings.appfunctions.GenericDeviceStateItemSetterParams
import com.android.settings.appfunctions.setters.DeviceStateSetter
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse

/**
 * A [DeviceStateExecutor] that sets device state for Settings that are exposed directly via Android
 * APIs rather than catalyst.
 */
class AndroidApiStateSetterExecutor(private val context: Context) : DeviceStateExecutor {

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
        if (params == null) {
            throw IllegalArgumentException("Provided params are null.")
        }
        val genericParams = GenericDeviceStateItemSetterParams(appFunctionType, params)
        Log.v(TAG, "Attempting to set device state for " + "$appFunctionType using params $params ")
        val setter = settingStateSettersMap[genericParams.getKey()]
        var setterResult: SetDeviceStateItemResponse? = null
        try {
            setterResult =
                setter?.let {
                    Log.v(TAG, "Attempting to set device state using $it")
                    it.set(context, appFunctionType, genericParams)
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting device state using $setter", e)
            null
        }

        return DeviceStateSetterExecutorResult(result = setterResult)
    }

    companion object {
        private val settingStateSetters: List<DeviceStateSetter> =
            listOf(
                // Add setter instances here
            )
        private val settingStateSettersMap = mutableMapOf<String, DeviceStateSetter>()

        private const val TAG = "AndroidApiStateSetterExecutor"

        init {
            for (setter in settingStateSetters) {
                settingStateSettersMap[setter.preferenceKey] = setter
            }
        }
    }
}
