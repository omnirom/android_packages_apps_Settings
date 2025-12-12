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

package com.android.settings.applications

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.AppDashboardActivity
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
@ProvidePreferenceScreen(AppDashboardScreen.KEY)
open class AppDashboardScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override val key
        get() = KEY

    override val title
        get() = R.string.apps_dashboard_title

    override val summary: Int
        get() = R.string.app_and_notification_dashboard_summary

    override fun getMetricsCategory() = SettingsEnums.MANAGE_APPLICATIONS

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_apps
            else -> R.drawable.ic_apps_filled
        }

    override val highlightMenuKey
        get() = R.string.menu_key_apps

    override fun isFlagEnabled(context: Context) = Flags.deeplinkApps25q4()

    override fun fragmentClass(): Class<out Fragment>? = AppDashboardFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, AppDashboardActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "top_level_apps"
    }
}
// LINT.ThenChange(AppDashboardFragment.java)
