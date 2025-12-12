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

package com.android.settings.wifi.tether

import android.Manifest
import android.app.settings.SettingsEnums.ACTION_WIFI_HOTSPOT
import android.app.settings.SettingsEnums.WIFI_TETHER_SETTINGS
import android.content.Context
import android.net.TetheringManager
import android.net.TetheringManager.TETHERING_WIFI
import android.net.wifi.WifiClient
import android.net.wifi.WifiManager
import android.os.UserManager
import android.text.BidiFormatter
import android.util.Log
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.WifiTetherSettingsActivity
import com.android.settings.Utils
import com.android.settings.contract.KEY_WIFI_HOTSPOT
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.datausage.DataSaverMainSwitchPreference
import com.android.settings.datausage.DataSaverMainSwitchPreference.Companion.KEY as DATA_SAVER_KEY
import com.android.settings.flags.Flags
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.wifi.WifiUtils.canShowWifiHotspot
import com.android.settings.wifi.utils.tetheringManager
import com.android.settings.wifi.utils.wifiApState
import com.android.settings.wifi.utils.wifiManager
import com.android.settings.wifi.utils.wifiSoftApSsid
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.TetherUtil
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.wifi.WifiUtils.Companion.getWifiTetherSummaryForConnectedDevices
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(WifiHotspotScreen.KEY)
open class WifiHotspotScreen(context: Context) :
    PreferenceScreenMixin,
    BooleanValuePreference,
    PrimarySwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceRestrictionMixin {

    private val wifiHotspotStore =
        WifiHotspotStore(context, DataSaverMainSwitchPreference.createDataStore(context))

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.wifi_hotspot_checkbox_text

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = WIFI_TETHER_SETTINGS

    // This screen comes from the preference and uses the same flag with its parent.
    override fun isFlagEnabled(context: Context) = Flags.catalystTetherSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = WifiTetherSettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, WifiTetherSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +WifiHotspotMainSwitchPreference(wifiHotspotStore)
            +WifiHotspotNamePreference(context, coroutineScope, wifiHotspotStore)
            +WifiHotspotAutoOffSwitchPreference()
        }

    override val preferenceActionMetrics: Int
        get() = ACTION_WIFI_HOTSPOT

    override fun tags(context: Context) = arrayOf(KEY_WIFI_HOTSPOT)

    override fun isAvailable(context: Context) =
        canShowWifiHotspot(context) &&
                TetherUtil.isTetherAvailable(context) &&
                !Utils.isMonkeyRunning()

    override fun getSummary(context: Context): CharSequence? =
        when (context.wifiApState) {
            WifiManager.WIFI_AP_STATE_ENABLING -> context.getString(R.string.wifi_tether_starting)
            WifiManager.WIFI_AP_STATE_ENABLED -> {
                val sapClientsSize = wifiHotspotStore.sapClientsSize
                if (sapClientsSize == null) {
                    context.getString(
                        R.string.wifi_tether_enabled_subtext,
                        BidiFormatter.getInstance().unicodeWrap(context.wifiSoftApSsid),
                    )
                } else {
                    getWifiTetherSummaryForConnectedDevices(context, sapClientsSize)
                }
            }
            WifiManager.WIFI_AP_STATE_DISABLING -> context.getString(R.string.wifi_tether_stopping)
            WifiManager.WIFI_AP_STATE_DISABLED ->
                context.getString(R.string.wifi_hotspot_off_subtext)
            else ->
                when (wifiHotspotStore.sapFailureReason) {
                    WifiManager.SAP_START_FAILURE_NO_CHANNEL ->
                        context.getString(R.string.wifi_sap_no_channel_error)
                    else -> context.getString(R.string.wifi_error)
                }
        }

    override fun isEnabled(context: Context) =
        wifiHotspotStore.dataSaverStore.getBoolean(DATA_SAVER_KEY) != true &&
                super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_WIFI_TETHERING)

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

    override fun storage(context: Context): KeyValueStore = wifiHotspotStore

    @Suppress("UNCHECKED_CAST")
    private class WifiHotspotStore(
        private val context: Context,
        val dataSaverStore: KeyValueStore,
    ) :
        AbstractKeyedDataObservable<String>(),
        KeyValueStore,
        WifiTetherSoftApManager.WifiTetherSoftApCallback,
        TetheringManager.StartTetheringCallback,
        KeyedObserver<String> {

        private var wifiTetherSoftApManager: WifiTetherSoftApManager? = null
        var sapFailureReason: Int? = null
        var sapClientsSize: Int? = null

        override fun contains(key: String) =
            key == KEY && context.wifiManager != null && context.tetheringManager != null

        override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
            val wifiApState = context.wifiApState
            val value =
                wifiApState == WifiManager.WIFI_AP_STATE_ENABLING ||
                        wifiApState == WifiManager.WIFI_AP_STATE_ENABLED
            return value as T?
        }

        override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
            if (value !is Boolean) return
            val tetheringManager = context.tetheringManager ?: return
            if (value) {
                tetheringManager.startTethering(TETHERING_WIFI, HandlerExecutor.main, this)
            } else {
                tetheringManager.stopTethering(TETHERING_WIFI)
            }
        }

        override fun onFirstObserverAdded() {
            val apManager = WifiTetherSoftApManager(context.wifiManager, this)
            wifiTetherSoftApManager = apManager
            apManager.registerSoftApCallback()
            dataSaverStore.addObserver(DATA_SAVER_KEY, this, HandlerExecutor.main)
        }

        override fun onLastObserverRemoved() {
            dataSaverStore.removeObserver(DATA_SAVER_KEY, this)
            wifiTetherSoftApManager?.unRegisterSoftApCallback()
        }

        override fun onStateChanged(state: Int, failureReason: Int) {
            Log.d(TAG, "onStateChanged(),state=$state,failureReason=$failureReason")
            sapFailureReason = failureReason
            if (state == WifiManager.WIFI_AP_STATE_DISABLED) sapClientsSize = null
            notifyChange(KEY, PreferenceChangeReason.VALUE)
        }

        override fun onConnectedClientsChanged(clients: List<WifiClient>?) {
            sapClientsSize = clients?.size ?: 0
            Log.d(TAG, "onConnectedClientsChanged(),sapClientsSize=$sapClientsSize")
            notifyChange(KEY, PreferenceChangeReason.STATE)
        }

        override fun onTetheringStarted() {}

        override fun onTetheringFailed(error: Int) {
            Log.e(TAG, "onTetheringFailed(),error=$error")
        }

        override fun onKeyChanged(key: String, reason: Int) = notifyChange(KEY, reason)
    }

    companion object {
        const val TAG = "WifiHotspotScreen"
        const val KEY = "wifi_tether"
    }
}
// LINT.ThenChange(WifiTetherSettings.java)
