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

import android.view.View
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner


@RunWith(RobolectricTestRunner::class)
class BluetoothHeaderSubDeviceTest {
    private lateinit var underTest: BluetoothHeaderSubDevice

    @Before
    fun setUp() {
        underTest = BluetoothHeaderSubDevice(ApplicationProvider.getApplicationContext(), null)
    }

    @Test
    fun leftBattery() {
        underTest.subDeviceType = BluetoothHeaderSubDevice.SubDeviceType.Left
        underTest.batteryLevel = 99

        val textView = underTest.findViewById<TextView>(R.id.header_title)
        assertThat(textView.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView.text).isEqualTo("Left: 99%")
    }

    @Test
    fun rightBattery() {
        underTest.subDeviceType = BluetoothHeaderSubDevice.SubDeviceType.Right
        underTest.batteryLevel = 99

        val textView = underTest.findViewById<TextView>(R.id.header_title)
        assertThat(textView.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView.text).isEqualTo("Right: 99%")
    }

    @Test
    fun caseBattery() {
        underTest.subDeviceType = BluetoothHeaderSubDevice.SubDeviceType.Case
        underTest.batteryLevel = 99

        val textView = underTest.findViewById<TextView>(R.id.header_title)
        assertThat(textView.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView.text).isEqualTo("Case: 99%")
    }

    @Test
    fun mainBattery() {
        underTest.subDeviceType = BluetoothHeaderSubDevice.SubDeviceType.Main
        underTest.batteryLevel = 99

        val textView = underTest.findViewById<TextView>(R.id.header_title)
        assertThat(textView.visibility).isEqualTo(View.VISIBLE)
        assertThat(textView.text).isEqualTo("99%")
    }

    @Test
    fun doNotShowBatteryText() {
        underTest.subDeviceType = BluetoothHeaderSubDevice.SubDeviceType.Main
        underTest.batteryLevel = 99
        underTest.showText = false

        val textView = underTest.findViewById<TextView>(R.id.header_title)
        assertThat(textView.visibility).isEqualTo(View.INVISIBLE)
    }
}
