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
package com.android.settings.wifi.savedaccesspoints2

import android.app.settings.SettingsEnums
import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.SavedAccessPointsSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.wifi.WifiPickerTrackerHelper
import com.android.settings.wifi.utils.wifiManager
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.utils.StringUtil
import com.android.wifitrackerlib.WifiPickerTracker
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(SavedAccessPointsWifiScreen.KEY)
open class SavedAccessPointsWifiScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider,
    WifiPickerTracker.WifiPickerTrackerCallback {
    private lateinit var lifeCycleContext: PreferenceLifecycleContext
    private var wifiTracker: WifiPickerTracker? = null

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.wifi_saved_access_points_label

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun getMetricsCategory() = SettingsEnums.WIFI_SAVED_ACCESS_POINTS

    override fun isFlagEnabled(context: Context) = Flags.deeplinkNetworkAndInternet25q4()

    override fun fragmentClass(): Class<out Fragment>? = SavedAccessPointsWifiSettings2::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SavedAccessPointsSettingsActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context): Boolean {
        val wifiManager = context.wifiManager ?: return false

        val numSavedNetworks = getSavedNetworksSize(getSavedNetworkConfigs(wifiManager)).size
        val numSavedSubscriptions = getSubscriptions(wifiManager).size

        return (numSavedNetworks + numSavedSubscriptions) > 0
    }

    override fun getSummary(context: Context): CharSequence? {
        val wifiManager = context.wifiManager ?: return null

        val numSavedNetworks = getSavedNetworksSize(getSavedNetworkConfigs(wifiManager)).size
        val numSavedSubscriptions = getSubscriptions(wifiManager).size

        return getSavedNetworkSettingsSummaryText(context, numSavedNetworks, numSavedSubscriptions)
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (isEntryPoint(context)) {
            lifeCycleContext = context
            if (context.wifiManager != null) {
                val fragment = context.findFragment("NetworkProviderSettings") ?: return
                WifiPickerTrackerHelper(fragment.lifecycle, context, this).apply {
                    wifiTracker = wifiPickerTracker
                }
            }
        }
    }

    private fun PreferenceLifecycleContext.findFragment(targetFragment: String): Fragment? =
        fragmentManager.fragments.find { it -> it.javaClass.simpleName == targetFragment }

    override fun onNumSavedNetworksChanged() {
        lifeCycleContext.notifyPreferenceChange(KEY)
    }

    override fun onNumSavedSubscriptionsChanged() {
        lifeCycleContext.notifyPreferenceChange(KEY)
    }

    override fun onWifiStateChanged() {
        // Required override for WifiPickerTracker.WifiPickerTrackerCallback, but not used.
    }

    private fun getSavedNetworkConfigs(wifiManager: WifiManager): List<WifiConfiguration> =
        wifiManager.configuredNetworks.filter { config ->
            !config.carrierMerged &&
                !config.isPasspoint &&
                !config.fromWifiNetworkSuggestion &&
                !config.fromWifiNetworkSpecifier &&
                !config.isEphemeral
        }

    private fun getSavedNetworksSize(wifiConfigs: List<WifiConfiguration>) =
        wifiConfigs.map { it.networkId }.distinct()

    private fun getSubscriptions(wifiManager: WifiManager) = wifiManager.passpointConfigurations

    private fun getSavedNetworkSettingsSummaryText(
        context: Context,
        numSavedNetworks: Int,
        numSavedSubscriptions: Int,
    ): String =
        if (numSavedSubscriptions == 0) {
            StringUtil.getIcuPluralsString(
                context,
                numSavedNetworks,
                R.string.wifi_saved_access_points_summary,
            )
        } else if (numSavedNetworks == 0) {
            StringUtil.getIcuPluralsString(
                context,
                numSavedSubscriptions,
                R.string.wifi_saved_passpoint_access_points_summary,
            )
        } else {
            StringUtil.getIcuPluralsString(
                context,
                numSavedNetworks + numSavedSubscriptions,
                R.string.wifi_saved_all_access_points_summary,
            )
        }

    companion object {
        const val KEY = "saved_networks"
    }
}
// LINT.ThenChange(SavedAccessPointsWifiSettings2.java)
