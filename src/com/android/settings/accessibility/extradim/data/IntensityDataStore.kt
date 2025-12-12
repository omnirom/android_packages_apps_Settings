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

package com.android.settings.accessibility.extradim.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore

class IntensityDataStore(private val context: Context) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore
        get() = SettingsSecureStore.get(context)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return MAX_VALUE as? T
    }

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val settingValue = keyValueStoreDelegate.getInt(SETTING_KEY) ?: 0
        return (MAX_VALUE - settingValue) as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Int) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }
        value as Int
        keyValueStoreDelegate.setInt(SETTING_KEY, MAX_VALUE - value)
    }

    companion object {
        const val SETTING_KEY = Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL
        private const val LOG_TAG = "ExtraDimIntensityDataStore"
        const val MAX_VALUE = 100
        const val MIN_VALUE = 0
    }
}
