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
package com.android.settings.supervision

import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.R
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

/** Web content filters landing page (Settings > Supervision > Web content filters). */
@ProvidePreferenceScreen(SupervisionWebContentFiltersScreen.KEY)
class SupervisionWebContentFiltersScreen : PreferenceScreenCreator {
    override fun isFlagEnabled(context: Context) = Flags.enableWebContentFiltersScreen()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_web_content_filters_title

    // TODO(b/395134536) update the summary once the string is finalized.
    override val icon: Int
        get() = R.drawable.ic_globe

    override fun fragmentClass() = SupervisionWebContentFiltersFragment::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +PreferenceCategory(
                BROWSER_RADIO_BUTTON_GROUP,
                R.string.supervision_web_content_filters_browser_title,
            ) +=
                {
                    val dataStore = SupervisionSafeSitesDataStore(context)
                    +SupervisionBlockExplicitSitesPreference(dataStore)
                    +SupervisionAllowAllSitesPreference(dataStore)
                }
            +PreferenceCategory(
                SEARCH_RADIO_BUTTON_GROUP,
                R.string.supervision_web_content_filters_search_title,
            ) +=
                {
                    val dataStore = SupervisionSafeSearchDataStore(context)
                    +SupervisionSearchFilterOnPreference(dataStore)
                    +SupervisionSearchFilterOffPreference(dataStore)
                }
        }

    companion object {
        const val KEY = "supervision_web_content_filters"
        internal const val BROWSER_RADIO_BUTTON_GROUP = "browser_radio_button_group"
        internal const val SEARCH_RADIO_BUTTON_GROUP = "search_radio_button_group"
    }
}
