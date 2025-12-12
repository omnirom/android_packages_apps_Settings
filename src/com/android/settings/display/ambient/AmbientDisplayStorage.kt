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
package com.android.settings.display.ambient

import android.content.Context
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import com.android.settings.display.AmbientDisplayAlwaysOnPreference
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceScreen
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore

@Suppress("UNCHECKED_CAST")
class AmbientDisplayStorage(
    private val context: Context,
    private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    override fun contains(key: String) = settingsStore.contains(DOZE_ALWAYS_ON)

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        context.resources.getBoolean(com.android.internal.R.bool.config_dozeAlwaysOnEnabled) as T

    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        settingsStore.getValue(DOZE_ALWAYS_ON, valueType) ?: getDefaultValue(key, valueType)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
        settingsStore.setValue(DOZE_ALWAYS_ON, valueType, value)

    override fun onFirstObserverAdded() {
        // observe the underlying storage key
        settingsStore.addObserver(DOZE_ALWAYS_ON, this, HandlerExecutor.main)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        // forward data change to preference hierarchy key
        notifyChange(AmbientDisplayAlwaysOnPreferenceScreen.KEY, reason)
        notifyChange(AmbientDisplayAlwaysOnPreference.KEY, reason)
        notifyChange(AmbientDisplayMainSwitchPreference.KEY, reason)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(DOZE_ALWAYS_ON, this)
    }
}
