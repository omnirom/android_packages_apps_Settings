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
package com.android.settings.accessibility

import android.content.Context
import android.provider.Settings.System.VIBRATE_ON
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore

/** SettingsStore for vibration toggle preferences with custom default value. */
open class VibrationToggleSettingsStore(
    private val context: Context,
    private val preferenceKey: String,
    private val settingsProviderKey: String,
    private val defaultValue: Boolean = false,
    private val keyValueStoreDelegate: KeyValueStore = SettingsSystemStore.get(context),
    private val dependencyStore: VibrationIntensitySettingsStore? = null,
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    fun isPreferenceEnabled(): Boolean {
        if (
            (settingsProviderKey != VIBRATE_ON) &&
                (keyValueStoreDelegate.getBoolean(VIBRATE_ON) == false)
        ) {
            return false
        }
        return dependencyStore?.isIntensityOff()?.not() ?: true
    }

    override fun contains(key: String) = keyValueStoreDelegate.contains(settingsProviderKey)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) = defaultValue as T

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        if (isPreferenceEnabled()) {
            keyValueStoreDelegate.getBoolean(settingsProviderKey) ?: defaultValue
        } else {
            // Preference must show toggle off when disabled, but value stored must be preserved.
            false
        }
            as T?

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
        keyValueStoreDelegate.setBoolean(settingsProviderKey, value as Boolean?)

    override fun onFirstObserverAdded() {
        // observe the underlying storage key
        keyValueStoreDelegate.addObserver(settingsProviderKey, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        keyValueStoreDelegate.removeObserver(settingsProviderKey, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        // forward data change to preference hierarchy key
        notifyChange(preferenceKey, reason)
    }
}
