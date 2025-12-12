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
package com.android.settings.fuelgauge.batteryusage

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.PowerUsageSummaryActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.BatteryPercentageSwitchPreference
import com.android.settings.flags.Flags
import com.android.settings.fuelgauge.BatteryHeaderPreference
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(PowerUsageSummaryScreen.KEY)
open class PowerUsageSummaryScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.power_usage_summary_title

    override val keywords: Int
        get() = R.string.keywords_battery

    override fun getMetricsCategory() = SettingsEnums.FUELGAUGE_POWER_USAGE_SUMMARY_V2

    override val highlightMenuKey
        get() = R.string.menu_key_battery

    override fun isFlagEnabled(context: Context) = Flags.catalystPowerUsageSummaryScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = PowerUsageSummary::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, PowerUsageSummaryActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_top_level_battery)

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_battery
            else -> R.drawable.ic_settings_battery_filled
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +BatteryHeaderPreference()
            if (Flags.deeplinkBattery25q4()) {
                +UntitledPreferenceCategoryMetadata("power_usage_summary_category") += {
                    +PowerUsageAdvancedScreen.KEY
                }
            }
            +UntitledPreferenceCategoryMetadata("percentage_category") += {
                +BatteryPercentageSwitchPreference()
            }
        }

    companion object {
        const val KEY = "power_usage_summary_screen"
    }
}
