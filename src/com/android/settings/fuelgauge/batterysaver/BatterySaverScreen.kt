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
package com.android.settings.fuelgauge.batterysaver

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.BatterySaverSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(BatterySaverScreen.KEY)
open class BatterySaverScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.battery_saver

    override val keywords: Int
        get() = R.string.keywords_battery_saver

    override fun getMetricsCategory() = SettingsEnums.OPEN_BATTERY_SAVER

    override val highlightMenuKey
        get() = R.string.menu_key_battery

    override fun isFlagEnabled(context: Context) = Flags.catalystBatterySaverScreen()

    override fun fragmentClass(): Class<out Fragment>? = BatterySaverSettings::class.java

    override fun hasCompleteHierarchy() = false

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) { +BatterySaverPreference() order -100 }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, BatterySaverSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "battery_saver_screen"
    }
}
