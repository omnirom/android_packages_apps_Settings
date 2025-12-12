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
import android.util.Log
import com.google.android.appfunctions.schema.common.v1.devicestate.AdjustNumericDeviceStateItemByPercentageParams
import com.google.android.appfunctions.schema.common.v1.devicestate.OffsetNumericDeviceStateItemByValueParams
import com.google.android.appfunctions.schema.common.v1.devicestate.SetDeviceStateItemParams
import com.google.android.appfunctions.schema.common.v1.devicestate.ToggleDeviceStateItemParams
import java.lang.IllegalArgumentException

/**
 * A wrapper class for the different types of device state setter params. It accepts a
 * GenericDocument and parses it according to the provided appFunctionType.
 */
class GenericDeviceStateItemSetterParams(
    private val appFunctionType: DeviceStateAppFunctionType,
    unparsedParams: GenericDocument,
) {

    private val params: Any = parseParams(appFunctionType, unparsedParams)

    private fun parseParams(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): Any {
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> parseSetDeviceStateItemParams(params)
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                parseOffsetNumericDeviceStateItemByValueParams(params)
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                parseAdjustNumericDeviceStateItemByPercentageParams(params)
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE ->
                parseToggleDeviceStateItemParams(params)
            else -> {
                throw IllegalArgumentException("Unrecognised appFunctionType: $appFunctionType")
            }
        }
    }

    /**
     * Returns the parsed SetDeviceStateItemParams if the provided appFunctionType was
     * SET_DEVICE_STATE, otherwise throws an Exception.
     */
    fun getSetDeviceStateItemParams(): SetDeviceStateItemParams {
        check(appFunctionType == DeviceStateAppFunctionType.SET_DEVICE_STATE) {
            "Requesting getSetDeviceStateItemParams for a $appFunctionType params."
        }
        return params as SetDeviceStateItemParams
    }

    /**
     * Returns the parsed OffsetNumericDeviceStateItemByValueParams if the provided appFunctionType
     * was OFFSET_DEVICE_STATE_BY_VALUE, otherwise throws an Exception.
     */
    fun getOffsetNumericDeviceStateItemByValueParams(): OffsetNumericDeviceStateItemByValueParams {
        check(appFunctionType == DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE) {
            "Requesting getOffsetNumericDeviceStateItemByValueParams for a $appFunctionType params."
        }
        return params as OffsetNumericDeviceStateItemByValueParams
    }

    /**
     * Returns the parsed AdjustNumericDeviceStateItemByPercentageParams if the provided
     * appFunctionType was ADJUST_DEVICE_STATE_BY_PERCENTAGE, otherwise throws an Exception.
     */
    fun getAdjustNumericDeviceStateItemByPercentageParams():
        AdjustNumericDeviceStateItemByPercentageParams {
        check(appFunctionType == DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE) {
            "Requesting getAdjustNumericDeviceStateItemByPercentageParams for a $appFunctionType params."
        }
        return params as AdjustNumericDeviceStateItemByPercentageParams
    }

    /**
     * Returns the parsed ToggleDeviceStateItemParams if the provided appFunctionType was
     * TOGGLE_DEVICE_STATE, otherwise throws an Exception.
     */
    fun getToggleDeviceStateItemParams(): ToggleDeviceStateItemParams {
        check(appFunctionType == DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE) {
            "Requesting getToggleDeviceStateItemParams for a $appFunctionType params."
        }
        return params as ToggleDeviceStateItemParams
    }

    /** Returns the non-parametrised preference key. */
    fun getKey(): String {
        return when (appFunctionType) {
            DeviceStateAppFunctionType.SET_DEVICE_STATE -> getSetDeviceStateItemParams().key
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE ->
                getOffsetNumericDeviceStateItemByValueParams().key
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE ->
                getAdjustNumericDeviceStateItemByPercentageParams().key
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE -> getToggleDeviceStateItemParams().key
            else -> {
                throw IllegalStateException("Invalid app function type $appFunctionType")
            }
        }
    }

    private fun parseSetDeviceStateItemParams(params: GenericDocument?): SetDeviceStateItemParams {
        val setDeviceStateItemParams = SetDeviceStateItemParams(key = "key", value = "value")

        val unparsedParams = params?.getPropertyDocument("setDeviceStateItemParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing setDeviceStateItemParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found setDeviceStateItemParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return setDeviceStateItemParams
    }

    private fun parseOffsetNumericDeviceStateItemByValueParams(
        params: GenericDocument?
    ): OffsetNumericDeviceStateItemByValueParams {
        val offsetNumericDeviceStateItemByValueParams =
            OffsetNumericDeviceStateItemByValueParams(key = "key", valueAdjustment = 0.0)

        val unparsedParams =
            params?.getPropertyDocument("offsetNumericDeviceStateItemByValueParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing offsetNumericDeviceStateItemByValueParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found offsetNumericDeviceStateItemByValueParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return offsetNumericDeviceStateItemByValueParams
    }

    private fun parseAdjustNumericDeviceStateItemByPercentageParams(
        params: GenericDocument?
    ): AdjustNumericDeviceStateItemByPercentageParams {
        val adjustNumericDeviceStateItemByPercentageParams =
            AdjustNumericDeviceStateItemByPercentageParams(key = "key", percentageAdjustment = 0)

        val unparsedParams =
            params?.getPropertyDocument("adjustNumericDeviceStateItemByPercentageParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing adjustNumericDeviceStateItemByPercentageParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found adjustNumericDeviceStateItemByPercentageParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return adjustNumericDeviceStateItemByPercentageParams
    }

    private fun parseToggleDeviceStateItemParams(
        params: GenericDocument?
    ): ToggleDeviceStateItemParams {
        val toggleDeviceStateParams = ToggleDeviceStateItemParams(key = "key")

        val unparsedParams = params?.getPropertyDocument("toggleDeviceStateItemParams")
        if (unparsedParams == null) {
            throw IllegalArgumentException(
                "Missing toggleDeviceStateItemParams in the request: $params"
            )
        } else {
            Log.i(TAG, "Found toggleDeviceStateItemParams: $unparsedParams")
            // TODO: need to manually parse and construct the object
        }
        return toggleDeviceStateParams
    }

    companion object {
        private const val TAG = "GenericDeviceStateItemSetterParams"
    }
}
