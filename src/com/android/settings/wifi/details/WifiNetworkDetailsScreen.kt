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

package com.android.settings.wifi.details

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// TODO(b/418768192): add a lint check to update WifiNetworkDetailsFragment.java when this file is
// changed and vice versa.
@ProvidePreferenceScreen(WifiNetworkDetailsScreen.KEY, parameterized = true)
open class WifiNetworkDetailsScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceTitleProvider {

    private val wifiEntryKey = arguments.getString(KEY_ARGUMENT_WIFI_ENTRY_KEY)!!

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.pref_title_network_details

    override val highlightMenuKey: Int
        get() = R.string.menu_key_network

    override fun fragmentClass(): Class<out PreferenceFragment>? =
        WifiNetworkDetailsFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.WIFI_NETWORK_DETAILS

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence = ""

    // TODO(b/418768192): update based on
    // https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/AndroidManifest.xml;l=500;drc=3e1b7b3d940e8c8c2210fd08ff7a8e596ca3bfdd
    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return SubSettingLauncher(context)
            .setTitleRes(R.string.pref_title_network_details)
            .setDestination(WifiNetworkDetailsFragment::class.java.getName())
            .setArguments(Bundle().apply { putString(KEY_ARGUMENT_WIFI_ENTRY_KEY, wifiEntryKey) })
            .setSourceMetricsCategory(getMetricsCategory())
            .toIntent()
    }

    override fun isFlagEnabled(context: Context) = false

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "wifi_network_details"

        const val KEY_EXTRA_WIFI_ENTRY_KEY = "wifi_entry_key"
        const val KEY_ARGUMENT_WIFI_ENTRY_KEY = "key_chosen_wifientry_key"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            // TODO(b/418767976): generate all possible WiFi network entries.
        }
    }
}
