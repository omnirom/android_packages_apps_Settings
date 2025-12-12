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

package com.android.settings.system

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.SystemDashboardActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(SystemDashboardScreen.KEY)
open class SystemDashboardScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.header_category_system

    override val summary: Int
        get() = R.string.system_dashboard_summary

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun getIcon(context: Context) =
        when (isExpressiveTheme(context)) {
            true -> R.drawable.ic_homepage_system_dashboard
            else -> R.drawable.ic_settings_system_dashboard_filled
        }

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_SYSTEM_CATEGORY

    override fun isFlagEnabled(context: Context) = Flags.deeplinkSystem25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = SystemDashboardFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SystemDashboardActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "top_level_system"
    }
}
// LINT.ThenChange(SystemDashboardFragment.java)
