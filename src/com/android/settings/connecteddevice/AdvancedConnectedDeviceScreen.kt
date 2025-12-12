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
import com.android.settings.Settings.AdvancedConnectedDeviceActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(AdvancedConnectedDeviceScreen.KEY)
open class AdvancedConnectedDeviceScreen : PreferenceScreenMixin, PreferenceSummaryProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.connected_device_connections_title

    override fun getMetricsCategory() = SettingsEnums.CONNECTION_DEVICE_ADVANCED

    override val highlightMenuKey
        get() = R.string.menu_key_connected_devices

    override fun isFlagEnabled(context: Context) = Flags.deeplinkConnectedDevices25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? =
        AdvancedConnectedDeviceDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getSummary(context: Context): CharSequence? =
        context.getText(
            AdvancedConnectedDeviceController.getConnectedDevicesSummaryResourceId(context)
        )

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, AdvancedConnectedDeviceActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "connection_preferences"
    }
}
// LINT.ThenChange(AdvancedConnectedDeviceDashboardFragment.java, AdvancedConnectedDeviceController.java)
