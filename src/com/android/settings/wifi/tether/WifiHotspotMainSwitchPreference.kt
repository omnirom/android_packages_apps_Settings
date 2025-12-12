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
import android.net.wifi.WifiManager.WIFI_AP_STATE_ENABLED
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.widget.MainSwitchBarMetadata
import com.android.settings.wifi.utils.wifiApState
import com.android.settingslib.datastore.*
import com.android.settingslib.metadata.*

class WifiHotspotMainSwitchPreference(private val wifiHotspotStore: KeyValueStore) :
    MainSwitchBarMetadata, PreferenceAvailabilityProvider {

    override val key
        get() = KEY

    override val title
        get() = R.string.use_wifi_hotsopt_main_switch_title

    override val disableWidgetOnCheckedChanged: Boolean
        get() = false

    override fun isAvailable(context: Context) =
        context.wifiApState == WIFI_AP_STATE_ENABLED ||
                !(featureFactory.wifiFeatureProvider.wifiHotspotRepository?.isRestarting ?: false)

    override fun storage(context: Context): KeyValueStore = UseWifiHotspotStore(wifiHotspotStore)

    override fun getReadPermissions(context: Context) =
        Permissions.allOf(Manifest.permission.ACCESS_WIFI_STATE)

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(Manifest.permission.TETHER_PRIVILEGED)

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.HIGH_SENSITIVITY

    @Suppress("UNCHECKED_CAST")
    private class UseWifiHotspotStore(private val wifiHotspotStore: KeyValueStore) :
        AbstractKeyedDataObservable<String>(), KeyValueStore, KeyedObserver<String> {

        override fun contains(key: String) = key == KEY

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
            wifiHotspotStore.getValue(WifiHotspotScreen.KEY, valueType)

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) =
            wifiHotspotStore.setValue(WifiHotspotScreen.KEY, valueType, value)

        override fun onFirstObserverAdded() {
            wifiHotspotStore.addObserver(WifiHotspotScreen.KEY, this, HandlerExecutor.main)
        }

        override fun onLastObserverRemoved() {
            wifiHotspotStore.removeObserver(WifiHotspotScreen.KEY, this)
        }

        override fun onKeyChanged(key: String, reason: Int) = notifyChange(KEY, reason)
    }

    companion object {
        const val TAG = "WifiHotspotMainSwitchPreference"
        const val KEY = "use_wifi_hotspot"
    }
}