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

package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.Context
import android.view.accessibility.Flags
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ButtonNavigationSettingsScreen.KEY)
class ButtonNavigationSettingsScreen : PreferenceScreenMixin {
    override fun isFlagEnabled(context: Context): Boolean {
        return Flags.navbarFlipOrderOption()
    }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.button_navigation_settings_activity_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun fragmentClass() = ButtonNavigationSettingsFragment::class.java

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_BUTTON_NAV_DLG

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +PreferenceCategory("group", R.string.button_navigation_settings_order_title) += {
                +DefaultButtonNavigationSettingsOrderPreference(
                    ButtonNavigationSettingsOrderStore(context)
                )
                +ReverseButtonNavigationSettingsOrderPreference(
                    ButtonNavigationSettingsOrderStore(context)
                )
            }
        }

    companion object {
        const val KEY = "button_navigation_settings_page"
    }
}
