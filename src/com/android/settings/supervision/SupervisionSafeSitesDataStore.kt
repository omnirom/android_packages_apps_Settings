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
package com.android.settings.supervision

import android.content.Context
import android.provider.Settings.Secure.BROWSER_CONTENT_FILTERS_ENABLED
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore

/** Datastore of the safe sites preference. */
@Suppress("UNCHECKED_CAST")
class SupervisionSafeSitesDataStore(
    context: Context,
    private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    override fun contains(key: String) = settingsStore.contains(BROWSER_CONTENT_FILTERS_ENABLED)

    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        val settingValue: Int? = settingsStore.getInt(BROWSER_CONTENT_FILTERS_ENABLED)
        return (settingValue != null && (settingValue > 0)) as T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) return
        settingsStore.setBoolean(BROWSER_CONTENT_FILTERS_ENABLED, value)
    }

    override fun onFirstObserverAdded() {
        // observe the underlying storage key
        settingsStore.addObserver(BROWSER_CONTENT_FILTERS_ENABLED, this, HandlerExecutor.main)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        // forward data change to preference hierarchy key
        notifyChange(SupervisionSafeSitesSwitchPreference.KEY, reason)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(BROWSER_CONTENT_FILTERS_ENABLED, this)
    }
}
