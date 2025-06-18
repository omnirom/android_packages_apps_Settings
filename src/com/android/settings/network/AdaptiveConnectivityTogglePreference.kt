/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.network

import android.app.settings.SettingsEnums.ACTION_ADAPTIVE_CONNECTIVITY
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_ENABLED
import com.android.settings.R
import com.android.settings.contract.KEY_ADAPTIVE_CONNECTIVITY
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.MainSwitchPreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel

// LINT.IfChange
class AdaptiveConnectivityTogglePreference :
    MainSwitchPreference(KEY, R.string.adaptive_connectivity_main_switch_title),
    PreferenceActionMetricsProvider {

    override val preferenceActionMetrics: Int
        get() = ACTION_ADAPTIVE_CONNECTIVITY

    override fun tags(context: Context) = arrayOf(KEY_ADAPTIVE_CONNECTIVITY)

    override fun storage(context: Context): KeyValueStore =
        AdaptiveConnectivityToggleStorage(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int,
    ) = ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    @Suppress("UNCHECKED_CAST")
    private class AdaptiveConnectivityToggleStorage(
        private val context: Context,
        private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
    ) : KeyValueStoreDelegate {

        override val keyValueStoreDelegate
            get() = settingsStore

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
            DEFAULT_VALUE as T

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            settingsStore.setValue(key, valueType, value)
            context
                .getSystemService(WifiManager::class.java)
                ?.setWifiScoringEnabled((value as Boolean?) ?: DEFAULT_VALUE)
        }
    }

    companion object {
        const val KEY = ADAPTIVE_CONNECTIVITY_ENABLED
        const val DEFAULT_VALUE = true
    }
}
// LINT.ThenChange(AdaptiveConnectivityTogglePreferenceController.java)
