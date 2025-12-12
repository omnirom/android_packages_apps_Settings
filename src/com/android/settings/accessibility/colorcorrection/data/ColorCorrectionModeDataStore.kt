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

package com.android.settings.accessibility.colorcorrection.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager
import com.android.settings.R
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * Data store for a specific color correction mode. This class manages the state of a single color
 * correction mode, determined by the `modeValue` passed during construction.
 *
 * It interacts with [Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER] to determine if the current
 * system-wide color correction mode matches the `modeValue` of this instance.
 *
 * The `getValue` method will return `true` if the system's color correction mode matches
 * `modeValue`, and `false` otherwise.
 *
 * The `setValue` method, when given `true`, will set the system's color correction mode to
 * `modeValue`. Setting it to `false` has no effect, as this data store only represents one specific
 * mode.
 *
 * This class also observes changes to [Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER] and
 * notifies its own observers when the underlying setting changes.
 *
 * @param context The application context.
 */
class ColorCorrectionModeDataStore(context: Context) :
    KeyValueStore, AbstractKeyedDataObservable<String>(), KeyedObserver<String> {

    private val keyValueMap: Map<String, Int> by lazy {
        val keys = context.resources.getStringArray(R.array.daltonizer_mode_keys)
        val values = context.resources.getIntArray(R.array.daltonizer_type_values)

        require(keys.size == values.size) { "Key and value arrays must have the same size." }

        keys.zip(values.toTypedArray()).associate { (key, value) -> key to value }
    }
    private val settingSecureStore =
        SettingsSecureStore.get(context).apply { setDefaultValue(KEY, DEFAULT_MODE) }

    override fun contains(key: String): Boolean = settingSecureStore.contains(KEY)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? =
        (keyValueMap[key] == DEFAULT_MODE) as? T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        (settingSecureStore.getInt(KEY) == keyValueMap[key]) as? T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }

        if (value) {
            settingSecureStore.setInt(KEY, keyValueMap[key])
        }
    }

    override fun onFirstObserverAdded() {
        settingSecureStore.addObserver(KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingSecureStore.removeObserver(KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(reason)
    }

    companion object {
        const val DEFAULT_MODE = AccessibilityManager.DALTONIZER_CORRECT_DEUTERANOMALY
        const val KEY = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER
        private const val LOG_TAG = "ColorCorrectionModeDataStore"
    }
}
