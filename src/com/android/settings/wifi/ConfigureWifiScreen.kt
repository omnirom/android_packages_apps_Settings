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

package com.android.settings.wifi

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.PowerManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ConfigureWifiSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.network.AirplaneModePreference
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.wifi.utils.wifiManager
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ConfigureWifiScreen.KEY)
open class ConfigureWifiScreen(context: Context) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceLifecycleProvider {

    private val airplaneModeDataStore = AirplaneModePreference.createDataStore(context)
    private lateinit var keyedObserver: KeyedObserver<String>

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.network_and_internet_preferences_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.CONFIGURE_WIFI

    override fun isFlagEnabled(context: Context) =
        Flags.catalystConfigureNetworkSettings() &&
            !ConfigureWifiSettings.isGuestUser(context) &&
            context.resources.getBoolean(R.bool.config_show_wifi_settings)

    override fun hasCompleteHierarchy() = false

    // TODO: need to monitor the WiFi state change for updating summary.
    override fun getSummary(context: Context): CharSequence? =
        if (context.isWifiWakeupEnabled()) {
            context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_on)
        } else {
            context.getString(R.string.wifi_configure_settings_preference_summary_wakeup_off)
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ConfigureWifiSettingsActivity::class.java, metadata?.key)

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            keyedObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(KEY) }
            airplaneModeDataStore.addObserver(
                AirplaneModePreference.KEY,
                keyedObserver,
                HandlerExecutor.main,
            )
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            airplaneModeDataStore.removeObserver(AirplaneModePreference.KEY, keyedObserver)
        }
    }

    override fun fragmentClass(): Class<out Fragment>? = ConfigureWifiSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +WifiWakeupSwitchPreference() }

    private fun Context.isWifiWakeupEnabled(): Boolean {
        val wifiManager = this.wifiManager ?: return false
        val powerManager = getSystemService(PowerManager::class.java) ?: return false
        return airplaneModeDataStore.getBoolean(AirplaneModePreference.KEY) == false &&
            wifiManager.isAutoWakeupEnabled &&
            wifiManager.isScanAlwaysAvailable &&
            !powerManager.isPowerSaveMode
    }

    companion object {
        const val KEY = "configure_network_settings"
    }
}
// LINT.ThenChange(ConfigureWifiSettings.java)
