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

package com.android.settings.fuelgauge.batteryusage

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.PowerUsageAdvancedActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(PowerUsageAdvancedScreen.KEY)
open class PowerUsageAdvancedScreen : PreferenceScreenMixin, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.advanced_battery_preference_title

    override val summary: Int
        get() = R.string.advanced_battery_preference_summary

    override val screenTitle: Int
        get() = R.string.advanced_battery_title

    override val keywords: Int
        get() = R.string.keywords_battery_usage

    override fun getMetricsCategory() = SettingsEnums.FUELGAUGE_BATTERY_HISTORY_DETAIL

    override val highlightMenuKey
        get() = R.string.menu_key_battery

    override fun isFlagEnabled(context: Context) = Flags.deeplinkBattery25q4()

    override fun fragmentClass(): Class<out Fragment>? = PowerUsageAdvanced::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, PowerUsageAdvancedActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context) =
        featureFactory.powerUsageFeatureProvider.isBatteryUsageEnabled()

    companion object {
        const val KEY = "battery_usage_summary"
    }
}
// LINT.ThenChange(PowerUsageAdvanced.java)
