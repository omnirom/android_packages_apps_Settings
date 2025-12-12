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

package com.android.settings.display.darkmode

import android.content.Context
import android.provider.Settings
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

class DarkThemeModeStorage(context: Context) :
    AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {
    val settingsStore = SettingsSecureStore.get(context)

    override fun contains(key: String) =
        key == StandardDarkModeSelectorPreference.KEY ||
            key == ExpandedDarkModeSelectorPreference.KEY

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val enabled = settingsStore.getBoolean(KEY) == true
        return when (key) {
            StandardDarkModeSelectorPreference.KEY -> (!enabled) as T?
            ExpandedDarkModeSelectorPreference.KEY -> enabled as T?
            else -> false as T?
        }
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) return
        when (key) {
            StandardDarkModeSelectorPreference.KEY -> settingsStore.setBoolean(KEY, !value)
            ExpandedDarkModeSelectorPreference.KEY -> settingsStore.setBoolean(KEY, value)
            else -> return
        }
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(DataChangeReason.UPDATE)
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED

        fun getReadPermissions() = SettingsSecureStore.getReadPermissions()

        fun getWritePermissions() = SettingsSecureStore.getWritePermissions()
    }
}
