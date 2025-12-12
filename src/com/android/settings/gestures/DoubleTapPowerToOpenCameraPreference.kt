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

package com.android.settings.gestures

import android.content.Context
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore

// TODO: migrate for "Quickly open camera" item.
class DoubleTapPowerToOpenCameraPreference {

    companion object {
        const val KEY = "camera_double_tap_power_gesture_disabled"

        // This key value is different with others. The 0 is ON and 1 is OFF.
        private const val ON = 0
        private const val OFF = 1

        fun createDataStore(context: Context): KeyValueStore =
            DoubleTapPowerToOpenCameraStore(context)

        @Suppress("UNCHECKED_CAST")
        private class DoubleTapPowerToOpenCameraStore(private val context: Context) :
            KeyValueStoreDelegate {
            override val keyValueStoreDelegate: KeyValueStore
                get() = SettingsSecureStore.get(context).apply { setDefaultValue(KEY, ON) }

            override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
                (keyValueStoreDelegate.getInt(KEY) == ON) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                when {
                    value == null -> keyValueStoreDelegate.setInt(KEY, null)
                    else ->
                        if (value is Boolean) {
                            keyValueStoreDelegate.setInt(KEY, if (value) ON else OFF)
                        }
                }
            }
        }
    }
}
