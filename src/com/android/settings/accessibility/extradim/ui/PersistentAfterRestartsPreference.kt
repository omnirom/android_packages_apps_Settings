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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference

class PersistentAfterRestartsPreference(
    context: Context,
    private val extraDimStorage: ExtraDimDataStore = ExtraDimDataStore(context),
) :
    SwitchPreference(key = KEY, title = R.string.reduce_bright_colors_persist_preference_title),
    PreferenceLifecycleProvider {

    private var settingsKeyedObserver: KeyedObserver<String?>? = null

    // The term `restarts` is usually used for indicating restarting devices.
    // Therefore, We wouldn't want `Keep on after device restarts` preference in the Extra Dim
    // shows up as the search result when the user searches `restart`
    override val indexable
        get() = false

    override fun isEnabled(context: Context): Boolean = extraDimStorage.getBoolean(KEY) == true

    override fun storage(context: Context): KeyValueStore = SettingsSecureStore.get(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int? = ReadWritePermit.ALLOW

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (settingsKeyedObserver == null) {
            settingsKeyedObserver = KeyedObserver { _, _ ->
                context.notifyPreferenceChange(bindingKey)
            }
        }
        settingsKeyedObserver?.let { observer ->
            extraDimStorage.addObserver(observer, HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        settingsKeyedObserver?.let { observer -> extraDimStorage.removeObserver(observer) }
        settingsKeyedObserver = null
    }

    companion object {
        private const val KEY = Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS
    }
}
