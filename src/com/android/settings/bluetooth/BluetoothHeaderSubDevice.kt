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
package com.android.settings.bluetooth

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.android.settings.R
import com.android.settings.Utils
import com.android.settingslib.bluetooth.BluetoothUtils

/** Represents a BluetoothSubDevice, could be a left, case, right, or main device */
class BluetoothHeaderSubDevice : LinearLayout {
    /** Battery level, 0 ~ 100. If less than zero, the battery indicator will be hidden. */
    var batteryLevel = BluetoothUtils.META_INT_ERROR
        set(value) {
            field = value
            updateBatteryUi()
        }
    /** Sub-device type, can be left, case, right or main. */
    var subDeviceType: SubDeviceType = SubDeviceType.Main
        set(value) {
            field = value
            updateBatteryUi()
        }

    /** Whether shows the battery summary text. */
    var showText: Boolean = true
        set(value) {
            field = value
            updateBatteryUi()
        }

    var image: Drawable? = null
        set(value) {
            field = value
            findViewById<BluetoothBatteryIndicator>(R.id.battery_ring).apply {
                deviceIcon = value
            }
        }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        LayoutInflater.from(context).inflate(R.layout.bluetooth_header_sub_device, this, true)
    }

    /** Sets the charging status. */
    fun setCharging(chargingState: Boolean) {
        findViewById<BluetoothBatteryIndicator>(R.id.battery_ring).apply {
            charging = chargingState
        }
    }

    private fun updateBatteryUi() {
        findViewById<BluetoothBatteryIndicator>(R.id.battery_ring).batteryLevel = batteryLevel
        if (batteryLevel < 0 || !showText) {
            findViewById<TextView>(R.id.header_title).visibility = INVISIBLE
            return
        }
        findViewById<TextView>(R.id.header_title).visibility = VISIBLE
        val formattedBattery: String = Utils.formatPercentage(batteryLevel)
        findViewById<TextView>(R.id.header_title).text = when (subDeviceType) {
            SubDeviceType.Left -> {
                mContext.getString(
                    R.string.bluetooth_left_name_expressive, formattedBattery
                )
            }

            SubDeviceType.Case -> {
                mContext.getString(
                    R.string.bluetooth_middle_name_expressive, formattedBattery
                )
            }

            SubDeviceType.Right -> {
                mContext.getString(
                    R.string.bluetooth_right_name_expressive, formattedBattery
                )
            }

            SubDeviceType.Main -> {
                formattedBattery
            }
        }
        findViewById<TextView>(R.id.header_title).contentDescription = when (subDeviceType) {
            SubDeviceType.Left -> {
                mContext.getString(
                    R.string.bluetooth_left_content_description_expressive, formattedBattery
                )
            }

            SubDeviceType.Case -> {
                mContext.getString(
                    R.string.bluetooth_middle_content_description_expressive, formattedBattery
                )
            }

            SubDeviceType.Right -> {
                mContext.getString(
                    R.string.bluetooth_right_content_description_expressive, formattedBattery
                )
            }

            SubDeviceType.Main -> {
                mContext.getString(
                    R.string.bluetooth_main_content_description_expressive, formattedBattery
                )
            }
        }
    }

    sealed interface SubDeviceType {
        data object Left : SubDeviceType
        data object Case : SubDeviceType
        data object Right : SubDeviceType
        data object Main : SubDeviceType
    }
}
