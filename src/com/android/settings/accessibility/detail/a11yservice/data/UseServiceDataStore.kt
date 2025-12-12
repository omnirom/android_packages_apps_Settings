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

package com.android.settings.accessibility.detail.a11yservice.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settings.accessibility.extensions.isServiceEnabled
import com.android.settings.accessibility.extensions.useService
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * DataStore for [com.android.settings.accessibility.detail.a11yservice.ui.UseServicePreference] to
 * turn on/off the accessibility service.
 */
class UseServiceDataStore(
    private val context: Context,
    private val serviceInfo: AccessibilityServiceInfo,
    private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    override fun contains(key: String): Boolean {
        return settingsStore.contains(KEY)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return false as? T
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return serviceInfo.isServiceEnabled(context) as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean?) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }

        serviceInfo.useService(context, value ?: false)
    }

    override fun onFirstObserverAdded() {
        settingsStore.addObserver(KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsStore.removeObserver(KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(reason)
    }

    companion object {
        private const val KEY = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        private const val LOG_TAG = "UseServiceDataStore"
    }
}
