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

package com.android.settings.network

import android.Manifest
import android.app.settings.SettingsEnums.ACTION_ADAPTIVE_WIFI_SCORER
import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings.Secure.ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission
import com.android.settings.R
import com.android.settings.contract.KEY_ADAPTIVE_WIFI_SCORER
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.datastore.SettingsStore
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class WifiScorerTogglePreference() :
    SwitchPreference(
        KEY,
        R.string.adaptive_connectivity_wifi_switch_title,
        R.string.adaptive_connectivity_wifi_switch_summary,
    ),
    PreferenceActionMetricsProvider {

    override val preferenceActionMetrics: Int
        get() = ACTION_ADAPTIVE_WIFI_SCORER

    override val key: String
        get() = KEY

    override fun tags(context: Context) = arrayOf(KEY_ADAPTIVE_WIFI_SCORER)

    override fun storage(context: Context): KeyValueStore = WifiScorerToggleStorage(context)

    override fun getReadPermissions(context: Context) =
        SettingsSecureStore.getReadPermissions() and Manifest.permission.READ_PHONE_STATE

    override fun getWritePermissions(context: Context) =
        SettingsSecureStore.getWritePermissions() and Manifest.permission.NETWORK_SETTINGS

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
    private class WifiScorerToggleStorage(
        private val context: Context,
        private val settingsStore: SettingsStore = SettingsSecureStore.get(context),
    ) : KeyValueStoreDelegate {

        override val keyValueStoreDelegate
            get() = settingsStore

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T {
            val subscriptionManager =
                context.getSystemService(SubscriptionManager::class.java)
                    ?: return DEFAULT_VALUE as T
            return (SubscriptionUtil.hasSubscriptionForWifiScorerToggleOff(
                context,
                subscriptionManager,
            ) == false) as T
        }

        @RequiresPermission(Manifest.permission.NETWORK_SETTINGS)
        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            settingsStore.setValue(key, valueType, value)
            context
                .getSystemService(WifiManager::class.java)
                ?.setWifiScoringEnabled(
                    (value as Boolean?) ?: getDefaultValue(key, Boolean::class.java)
                )
        }
    }

    companion object {
        const val KEY = ADAPTIVE_CONNECTIVITY_WIFI_ENABLED
        const val DEFAULT_VALUE = true
    }
}
