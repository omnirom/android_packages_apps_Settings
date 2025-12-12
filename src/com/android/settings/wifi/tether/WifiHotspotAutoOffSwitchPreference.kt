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

package com.android.settings.wifi.tether

import android.Manifest
import android.content.Context
import android.net.wifi.SoftApConfiguration
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.wifi.utils.wifiManager
import com.android.settings.wifi.utils.wifiSoftApConfig
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class WifiHotspotAutoOffSwitchPreference :
    SwitchPreference(
        KEY,
        R.string.wifi_hotspot_auto_off_title,
        R.string.wifi_hotspot_auto_off_summary
    ),
    PreferenceLifecycleProvider {

    override fun onResume(context: PreferenceLifecycleContext) {
        context.findPreference<SwitchPreferenceCompat>(KEY)?.apply {
            isChecked = context.wifiSoftApConfig?.isAutoShutdownEnabled ?: false
        }
    }

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.ACCESS_WIFI_STATE)

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.TETHER_PRIVILEGED)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun storage(context: Context): KeyValueStore = Storage(context)

    @Suppress("UNCHECKED_CAST")
    private class Storage(private val context: Context) :
        KeyValueStore, NoOpKeyedObservable<String>() {

        private var needShutdownSecondarySap: Boolean = false

        init {
            featureFactory.wifiFeatureProvider.wifiHotspotRepository.let {
                if (it.isSpeedFeatureAvailable && it.isDualBand) {
                    needShutdownSecondarySap = true
                }
            }
        }

        override fun contains(key: String) = key == KEY && context.wifiManager != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>) =
            context.wifiSoftApConfig?.isAutoShutdownEnabled as T?

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            val isEnabled = value as? Boolean ?: return

            context.wifiSoftApConfig?.let { currentConfig ->
                val newConfig = SoftApConfiguration.Builder(currentConfig).apply {
                    setAutoShutdownEnabled(isEnabled)
                    if (needShutdownSecondarySap) {
                        setBridgedModeOpportunisticShutdownEnabled(isEnabled)
                    }
                }.build()

                context.wifiSoftApConfig = newConfig
            }
        }
    }

    companion object {
        const val KEY = "wifi_tether_auto_turn_off"
    }
}