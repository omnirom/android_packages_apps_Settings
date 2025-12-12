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
package com.android.settings.fuelgauge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.PrintWriter
import java.io.StringWriter

@RunWith(RobolectricTestRunner::class)
class BatteryOptimizationActionLogUtilsTest {
    private val testStringWriter = StringWriter()
    private val testPrintWriter = PrintWriter(testStringWriter)

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        BatteryOptimizationActionLogUtils.getSharedPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        BatteryOptimizationActionLogUtils.getSharedPreferences(context).edit().clear().commit()
    }

    @Test
    fun printHistoricalLog_withDefaultLogs() {
        BatteryOptimizationActionLogUtils.printBatteryOptimizationActionLogs(
            context,
            testPrintWriter
        )
        assertThat(testStringWriter.toString()).doesNotContain("=")
    }

    @Test
    fun writeLog_withExpectedLogs() {
        BatteryOptimizationActionLogUtils.writeLog(
            context,
            "pkg1",
            BatteryOptimizeHistoricalLogEntry.Action.APPLY
        )
        BatteryOptimizationActionLogUtils.printBatteryOptimizationActionLogs(
            context,
            testPrintWriter
        )

        assertThat(testStringWriter.toString()).contains("pkg1=APPLY")
    }

    @Test
    fun writeLog_multipleLogs_withCorrectCounts() {
        val expectedCount = 10
        for (i in 0..<expectedCount) {
            BatteryOptimizationActionLogUtils.writeLog(
                context,
                "pkg$i",
                BatteryOptimizeHistoricalLogEntry.Action.BATTERY_TIP_ACCEPT,
            )
        }
        BatteryOptimizationActionLogUtils.printBatteryOptimizationActionLogs(
            context,
            testPrintWriter
        )

        assertActionCount("BATTERY_TIP_ACCEPT", expectedCount)
    }

    @Test
    fun writeLog_multipleLogsWithTheSamePackageName_withCorrectCounts() {
        val expectedCount = 10
        for (i in 0..<expectedCount) {
            BatteryOptimizationActionLogUtils.writeLog(
                context,
                "pkg",
                if (i % 2 == 0) BatteryOptimizeHistoricalLogEntry.Action.BATTERY_TIP_APPLY
                else BatteryOptimizeHistoricalLogEntry.Action.BATTERY_TIP_ACCEPT,
            )
        }
        BatteryOptimizationActionLogUtils.printBatteryOptimizationActionLogs(
            context,
            testPrintWriter
        )

        assertActionCount("BATTERY_TIP_ACCEPT", 1)
    }

    private fun assertActionCount(token: String, count: Int) {
        val dumpResults = testStringWriter.toString()
        assertThat(testStringWriter.toString().split(token).size).isEqualTo(count + 1)
    }
}