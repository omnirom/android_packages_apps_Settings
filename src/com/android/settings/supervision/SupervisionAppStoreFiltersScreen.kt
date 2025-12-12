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

import android.app.settings.SettingsEnums
import android.app.supervision.flags.Flags
import android.content.Context
import com.android.settings.CatalystSettingsActivity
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

class SupervisionAppStoreFiltersActivity :
    CatalystSettingsActivity(SupervisionAppStoreFiltersScreen.KEY)

@ProvidePreferenceScreen(SupervisionAppStoreFiltersScreen.KEY)
open class SupervisionAppStoreFiltersScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider {

    override fun isAvailable(context: Context) = Flags.enableAppStoreFiltersScreen()

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.supervision_app_store_filters_title

    override val icon: Int
        get() = R.drawable.ic_apps_library

    override val highlightMenuKey: Int
        get() = R.string.menu_key_supervision

    override fun getMetricsCategory() = SettingsEnums.SUPERVISION_APP_STORE_FILTERS

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    companion object {
        const val KEY = "supervision_app_store_filters"
    }
}
