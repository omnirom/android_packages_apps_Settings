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

package com.android.settings.appfunctions.setters

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.GenericDeviceStateItemSetterParams
import com.google.android.appfunctions.schema.common.v1.devicestate.AdjustNumericDeviceStateItemByPercentageParams
import com.google.android.appfunctions.schema.common.v1.devicestate.OffsetNumericDeviceStateItemByValueParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemResponse
import com.google.android.appfunctions.schema.common.v1.devicestate.ToggleDeviceStateItemParams

/**
 * Attempts to set the device state according to the provided params if it's the appropriate setter
 * for it.
 *
 * Implementers should implement the appropriate setter methods out of the provided abstract
 * methods, and return null for the remaining setters.
 */
@Keep
abstract class DeviceStateSetter {
    abstract val preferenceKey: String

    /**
     * Sets the device state according to the provided params and returns the result. Returns null
     * if this setter isn't the appropriate setter for the provided params.
     */
    fun set(
        context: Context,
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDeviceStateItemSetterParams,
    ): SetDeviceStateItemResponse? {
        Log.i(
            TAG,
            "Attempting to execute a setDeviceStateRequest with " +
                "appFunctionType: $appFunctionType",
        )
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE ->
                setDeviceState(context, params.getSetDeviceStateItemParams())
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                offsetNumericDeviceStateByValue(
                    context,
                    params.getOffsetNumericDeviceStateItemByValueParams(),
                )
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                adjustNumericDeviceStateByPercentage(
                    context,
                    params.getAdjustNumericDeviceStateItemByPercentageParams(),
                )
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE ->
                toggleDeviceStateItemParams(context, params.getToggleDeviceStateItemParams())
            else -> {
                Log.i(TAG, "Unrecognised appFunctionType: $appFunctionType")
                return null
            }
        }
    }

    /**
     * Sets the value of a certain preference to an absolute value according to the provided params.
     * e.g. Setting ringtone volume tone to 8.
     */
    abstract fun setDeviceState(
        context: Context,
        params: SetDeviceStateItemParams,
    ): SetDeviceStateItemResponse?

    /**
     * Offsets the value of a preference by a certain value according to the provided params. e.g.
     * Increasing ringtone volume by 2.
     */
    abstract fun offsetNumericDeviceStateByValue(
        context: Context,
        params: OffsetNumericDeviceStateItemByValueParams,
    ): SetDeviceStateItemResponse?

    /**
     * Adjusts the value of a preference by a certain percentage according to the provided params.
     * e.g. Adjusting ringtone volume to 50% of current value.
     */
    abstract fun adjustNumericDeviceStateByPercentage(
        context: Context,
        params: AdjustNumericDeviceStateItemByPercentageParams,
    ): SetDeviceStateItemResponse?

    /** Changes the value of a toggle to the opposite of the current value. e.g. Toggling wifi. */
    abstract fun toggleDeviceStateItemParams(
        context: Context,
        params: ToggleDeviceStateItemParams,
    ): SetDeviceStateItemResponse?

    companion object {
        private const val TAG = "DeviceStateSetter"
    }
}
