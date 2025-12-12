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
package com.android.settings.network.tether

import android.app.settings.SettingsEnums
import android.content.Context
import android.net.TetheringManager
import android.os.UserManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.TetherSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.network.TetherPreferenceController
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settings.wifi.tether.WifiHotspotScreen
import com.android.settingslib.TetherUtil
import com.android.settingslib.Utils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(TetherScreen.KEY)
open class TetherScreen :
    PreferenceScreenMixin,
    PreferenceTitleProvider,
    PreferenceAvailabilityProvider,
    PreferenceRestrictionMixin {

    override val key: String
        get() = KEY

    override val icon: Int
        get() = R.drawable.ic_wifi_tethering

    override val keywords: Int
        get() = R.string.keywords_hotspot_tethering

    override fun getTitle(context: Context): CharSequence? =
        if (TetherPreferenceController.isTetherConfigDisallowed(context)) {
            context.getText(R.string.tether_settings_title_all)
        } else {
            val tetheringManager = context.getSystemService(TetheringManager::class.java)!!
            context.getText(Utils.getTetheringLabel(tetheringManager))
        }

    override fun isAvailable(context: Context) = TetherUtil.isTetherAvailable(context)

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override fun getMetricsCategory() = SettingsEnums.TETHER

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_CONFIG_TETHERING)

    override val highlightMenuKey
        get() = R.string.menu_key_network

    override fun isFlagEnabled(context: Context) = Flags.catalystTetherSettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = TetherSettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, TetherSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            if (Flags.catalystTetherSettings()) +WifiHotspotScreen.KEY
            if (Flags.catalystTetherSettings26q1()) +BluetoothTetherSwitchPreference()
        }

    companion object {
        const val KEY = "tether_settings"
    }
}
