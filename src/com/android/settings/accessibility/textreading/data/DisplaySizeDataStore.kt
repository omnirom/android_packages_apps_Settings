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

package com.android.settings.accessibility.textreading.data

import android.content.Context
import android.util.Log
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settings.core.instrumentation.SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED
import com.android.settings.core.instrumentation.SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_DISPLAY_SIZE
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.display.DisplayDensityUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DataStore for the display size setting. When the display changes, the configuration will changes,
 * hence do nothing on observers for now until needed later.
 */
class DisplaySizeDataStore(
    private val context: Context,
    private val displayDensityUtils: DisplayDensityUtils = DisplayDensityUtils(context),
    @EntryPoint private val entryPoint: Int = EntryPoint.UNKNOWN_ENTRY,
) : NoOpKeyedObservable<String>(), KeyValueStore {
    private val _displaySizeData = MutableStateFlow(loadDisplaySize())
    val displaySizeData = _displaySizeData.asStateFlow()

    override fun contains(key: String) = true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return _displaySizeData.value.run { values.indexOf(defaultValue) } as T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return _displaySizeData.value.currentIndex as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Int?) {
            Log.w(
                TAG,
                "setValue(key = $key, value = $value) ignored because the value is not a Int",
            )
            return
        }

        val data = _displaySizeData.value
        val currentIndex = data.currentIndex
        val newValue = value as? Int ?: getDefaultValue(key, Int::class.javaObjectType)!!
        if (newValue != currentIndex) {
            if (data.values[newValue] == data.defaultValue) {
                displayDensityUtils.clearForcedDisplayDensity()
            } else {
                displayDensityUtils.setForcedDisplayDensity(newValue)
            }
            SettingsStatsLog.write(
                ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_DISPLAY_SIZE,
                newValue,
                AccessibilityStatsLogUtils.convertToEntryPoint(entryPoint),
            )
            _displaySizeData.value = data.copy(currentIndex = newValue)
        }
    }

    fun resetToDefault() {
        setInt(key = "reset", value = null)
    }

    private fun loadDisplaySize(): DisplaySize {
        val currentIndex = displayDensityUtils.currentIndex
        val values = displayDensityUtils.values
        val currentDisplaySize =
            if (currentIndex < 0 || values == null) {
                // Failed to obtain default density, which means we failed to
                // connect to the window manager service. Just use the current
                // density and don't let the user change anything.
                val currentDensityDpi = context.resources.displayMetrics.densityDpi
                DisplaySize(
                    currentIndex = 0,
                    values = intArrayOf(currentDensityDpi),
                    defaultValue = currentDensityDpi,
                )
            } else {
                DisplaySize(
                    currentIndex = currentIndex,
                    values = values,
                    defaultValue = displayDensityUtils.defaultDensity,
                )
            }

        return currentDisplaySize
    }

    companion object {
        private const val TAG = "DisplaySizeDataStore"
    }
}
