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
package com.android.settings.display

import android.content.Context
import android.content.res.Resources
import android.hardware.display.ColorDisplayManager
import android.hardware.display.ColorDisplayManager.*
import android.util.Log

import com.android.settings.R

object ColorModeUtils {

    private val TAG = "ColorModeUtils"

    @JvmStatic
    fun getColorModeMapping(resources: Resources): Map<Int, String> {
        val colorModeOptionsStrings = resources.getStringArray(
                R.array.config_color_mode_options_strings
        )
        val colorModeOptionsValues = resources.getIntArray(
                R.array.config_color_mode_options_values
        )
        if (colorModeOptionsStrings.size!= colorModeOptionsValues.size) {
            throw RuntimeException("Color mode options of unequal length")
        }

        val colorModesToSummaries = colorModeOptionsValues.zip(colorModeOptionsStrings).toMap().filterKeys { colorMode ->
            colorMode == COLOR_MODE_NATURAL ||
                    colorMode == COLOR_MODE_BOOSTED ||
                    colorMode == COLOR_MODE_SATURATED ||
                    colorMode == COLOR_MODE_AUTOMATIC ||
                    (colorMode >= VENDOR_COLOR_MODE_RANGE_MIN &&
                            colorMode <= VENDOR_COLOR_MODE_RANGE_MAX)
        }

        return colorModesToSummaries
    }

    @JvmStatic
    fun getColorMode(context: Context): Int =
            context.getSystemService(ColorDisplayManager::class.java).colorMode

    @JvmStatic
    fun getActiveColorModeName(context: Context): String =
        getColorModeMapping(context.resources)[getColorMode(context)] ?: ""

    @JvmStatic
    fun getAvailableColorModes(context: Context): IntArray =
        context.getResources().getIntArray(com.android.internal.R.array.config_availableColorModes)
}