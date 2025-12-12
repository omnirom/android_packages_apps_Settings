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

package com.android.settings.connecteddevice

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ConnectedDeviceDashboardActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ConnectedDeviceDashboardScreen.KEY)
open class ConnectedDeviceDashboardScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.connected_devices_dashboard_title

    override val summary: Int
        get() = R.string.connected_devices_dashboard_default_summary

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY

    override val highlightMenuKey
        get() = R.string.menu_key_connected_devices

    override fun isFlagEnabled(context: Context) = Flags.deeplinkConnectedDevices25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? =
        ConnectedDeviceDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ConnectedDeviceDashboardActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context): Boolean =
        context.resources.getBoolean(R.bool.config_show_top_level_connected_devices)

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_connected_device
            else -> R.drawable.ic_devices_other_filled
        }

    companion object {
        const val KEY = "top_level_connected_devices"
    }
}
// LINT.ThenChange(ConnectedDeviceDashboardFragment.java, TopLevelConnectedDevicesPreferenceController.java)
