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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.util.Log
import android.view.View
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.Utils
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.widget.ValidatedEditTextPreference
import com.android.settings.wifi.WifiUtils
import com.android.settings.wifi.dpp.WifiDppUtils
import com.android.settings.wifi.utils.wifiApState
import com.android.settings.wifi.utils.wifiManager
import com.android.settings.wifi.utils.wifiSoftApConfig
import com.android.settings.wifi.utils.wifiSoftApSsid
import com.android.settingslib.TetherUtil
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// LINT.IfChange
class WifiHotspotNamePreference(
    context: Context,
    private val scope: CoroutineScope,
    private val wifiHotspotStore: KeyValueStore,
) : PreferenceMetadata,
    PreferenceAvailabilityProvider,
    PreferenceBinding,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    Preference.OnPreferenceChangeListener,
    ValidatedEditTextPreference.Validator {

    private val ssidFlow = MutableStateFlow(context.wifiSoftApSsid ?: "")
    private var ssidFlowJob: Job? = null
    private val wifiDeviceNameTextValidator = WifiDeviceNameTextValidator()
    private lateinit var keyedObserver: KeyedObserver<String>

    override val key
        get() = KEY

    override val title: Int
        get() = R.string.wifi_hotspot_name_title

    override fun isAvailable(context: Context) =
        WifiUtils.canShowWifiHotspot(context) &&
                TetherUtil.isTetherAvailable(context) &&
                !Utils.isMonkeyRunning()

    override fun getSummary(context: Context): CharSequence? = ssidFlow.value

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        ssidFlowJob?.cancel()
        ssidFlowJob = null
        (preference as? WifiTetherSsidPreference)?.apply {
            text = ssidFlow.value
            setValidator(this@WifiHotspotNamePreference)
            onPreferenceChangeListener = this@WifiHotspotNamePreference

            ssidFlowJob = scope.launch {
                showQrCodeButton(this@apply)
                ssidFlow.collect {
                    summary = it
                    text = it
                }
            }
        } ?: run {
            Log.e(TAG, "Unexpected preference type: ${preference.javaClass.simpleName}")
            return
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        keyedObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(KEY) }
        wifiHotspotStore.addObserver(WifiHotspotScreen.KEY, keyedObserver, HandlerExecutor.main)
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        scope.launch { refresh(context) }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        wifiHotspotStore.removeObserver(WifiHotspotScreen.KEY, keyedObserver)
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any): Boolean {
        val newSsid = newValue as String
        if (ssidFlow.value == newSsid) {
            return false
        }
        ssidFlow.value = newSsid

        featureFactory.metricsFeatureProvider.action(
            preference.context,
            SettingsEnums.ACTION_SETTINGS_CHANGE_WIFI_HOTSPOT_NAME
        )
        scope.launch { updateConfig(preference.context) }
        return true
    }

    override fun isTextValid(value: String?): Boolean {
        val isTextValid = wifiDeviceNameTextValidator.isTextValid(value)
        if (!isTextValid) {
            Log.w(TAG, "Wi-Fi hotspot name/SSID validation failed.")
        }
        return isTextValid
    }

    private suspend fun refresh(context: PreferenceLifecycleContext) {
        withContext(Dispatchers.Default) {
            val newSsid = context.wifiSoftApSsid ?: return@withContext
            ssidFlow.value = newSsid
        }
    }

    private suspend fun updateConfig(context: Context) {
        withContext(Dispatchers.Default) {
            context.wifiSoftApSsid = ssidFlow.value
            featureFactory.wifiFeatureProvider.wifiHotspotRepository?.restartTetheringIfNeeded()
        }
    }

    private suspend fun showQrCodeButton(preference: WifiTetherSsidPreference) {
        withContext(Dispatchers.Default) {
            val context = preference.context
            if (context.wifiApState != WifiManager.WIFI_AP_STATE_ENABLED) {
                preference.setButtonVisible(false)
                return@withContext
            }

            val intent = WifiDppUtils.getHotspotConfiguratorIntentOrNull(
                context,
                context.wifiManager,
                context.wifiSoftApConfig
            )
            if (intent == null) {
                preference.setButtonVisible(false)
                return@withContext
            }

            preference.setButtonOnClickListener { _: View? ->
                launchQrCodeSettings(context, intent)
            }
            preference.setButtonVisible(true)
        }
    }

    private fun launchQrCodeSettings(context: Context, intent: Intent) {
        WifiDppUtils.showLockScreenForWifiSharing(context) {
            featureFactory.metricsFeatureProvider.action(
                SettingsEnums.PAGE_UNKNOWN,
                SettingsEnums.ACTION_SETTINGS_SHARE_WIFI_HOTSPOT_QR_CODE,
                SettingsEnums.SETTINGS_WIFI_DPP_CONFIGURATOR,
                /* key */ null,
                /* value */ Int.MIN_VALUE
            )
            context.startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "WifiHotspotNamePreference"
        const val KEY = "wifi_tether_network_name"
    }
}
// LINT.ThenChange(WifiTetherSSIDPreferenceController.java)