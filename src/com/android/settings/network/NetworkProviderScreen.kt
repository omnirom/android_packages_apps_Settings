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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.UserManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.NetworkProviderSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.wifi.WifiDataUsagePreference
import com.android.settings.wifi.WifiSwitchPreference
import com.android.settings.wifi.savedaccesspoints2.SavedAccessPointsWifiScreen
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(NetworkProviderScreen.KEY)
open class NetworkProviderScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceRestrictionMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.provider_internet_settings

    override val icon: Int
        get() = R.drawable.ic_settings_wireless

    override val keywords: Int
        get() = R.string.keywords_internet

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_internet_settings)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun getMetricsCategory() = SettingsEnums.WIFI

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_WIFI)

    override val highlightMenuKey
        get() = R.string.menu_key_network

    override fun isFlagEnabled(context: Context) = Flags.catalystInternetSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = NetworkProviderSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory("wifi_category", R.string.wifi_settings) += {
                +WifiSwitchPreference()
            }
            +UntitledPreferenceCategoryMetadata("wifi_ext_category") += {
                if (Flags.deeplinkNetworkAndInternet25q4()) +SavedAccessPointsWifiScreen.KEY
                +WifiDataUsagePreference(context)
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, NetworkProviderSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "internet_settings"
    }
}
