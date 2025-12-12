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
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.android.settings.fuelgauge.BatteryOptimizeHistoricalLogEntry.Action
import java.io.PrintWriter

/** Writes and reads historical logs of battery optimization mode with certain actions. */
object BatteryOptimizationActionLogUtils {
    private const val TAG = "BatteryOptimizationActionLogUtils"
    private const val BATTERY_OPTIMIZE_ACTION_FILE_NAME = "battery_optimize_action_historical_logs"

    /** Writes the battery optimization mode update action history. */
    @JvmStatic
    fun writeLog(context: Context, packageName: String, action: Action) {
        val sharedPreferences = getSharedPreferences(context)
        sharedPreferences.edit { putInt(packageName, action.number) }
    }

    /** Gets the battery optimization mode change action for the given packages. */
    @JvmStatic
    fun getBatteryOptimizeActionLogs(
        context: Context,
        packageNames: List<String>
    ): List<Action> {
        val sharedPreferences = getSharedPreferences(context)
        val actionList: MutableList<Action> = ArrayList(packageNames.size)
        for (packageName in packageNames) {
            actionList.add(
                Action.forNumber(
                    sharedPreferences.getInt(packageName, Action.UNKNOWN.number)
                ) ?: Action.UNKNOWN
            )
        }
        return actionList
    }

    /** Prints the historical log that has previously been stored by this utility. */
    @JvmStatic
    fun printBatteryOptimizationActionLogs(context: Context, writer: PrintWriter) {
        val sharedPreferences = getSharedPreferences(context)
        writer.println("Battery optimization action history:")
        for (key in sharedPreferences.all.keys) {
            val action = Action.forNumber(sharedPreferences.getInt(key, Action.UNKNOWN.number))
            if (action != null && action != Action.UNKNOWN) {
                writer.print("$key=$action\t")
            }
        }
        writer.println()
    }

    @VisibleForTesting
    @JvmStatic
    fun getSharedPreferences(context: Context): SharedPreferences {
        return context.applicationContext
            .getSharedPreferences(
                BATTERY_OPTIMIZE_ACTION_FILE_NAME, Context.MODE_PRIVATE
            )
    }
}
