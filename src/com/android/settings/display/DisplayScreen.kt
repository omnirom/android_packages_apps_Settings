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
package com.android.settings.display

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.DisplaySettings
import com.android.settings.R
import com.android.settings.Settings.DisplaySettingsActivity
import com.android.settings.accessibility.Flags as AccessibilityFlags
import com.android.settings.accessibility.textreading.ui.TextReadingScreen
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.darkmode.DarkModeScreen
import com.android.settings.dream.ScreensaverScreen
import com.android.settings.flags.Flags
import com.android.settings.security.LockScreenPreferenceScreen
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceCategory as Category
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import com.android.systemui.shared.Flags.ambientAod
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(DisplayScreen.KEY)
open class DisplayScreen :
    PreferenceScreenMixin, PreferenceAvailabilityProvider, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.display_settings

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_display
            else -> R.drawable.ic_settings_display_filled
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getMetricsCategory() = SettingsEnums.DISPLAY

    override fun isFlagEnabled(context: Context) = Flags.catalystDisplaySettingsScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = DisplaySettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +Category("category_brightness", R.string.category_name_brightness) order -200 += {
                +BrightnessLevelPreference()
                if (Flags.catalystScreenBrightnessMode()) +AutoBrightnessScreen.KEY
            }
            +Category("category_lock_display", R.string.category_name_lock_display) order -190 += {
                if (Flags.catalystLockscreenFromDisplaySettings()) +LockScreenPreferenceScreen.KEY
                if (ambientAod()) +AmbientDisplayAlwaysOnPreferenceScreen.KEY
            }
            +Category("category_key_appearance", R.string.category_name_appearance) order -180 += {
                if (AccessibilityFlags.catalystDarkUiMode()) +DarkModeScreen.KEY
                if (Flags.catalystTextReadingScreen()) +TextReadingScreen.KEY
            }
            +Category("category_other", R.string.category_name_display_controls) order -150 += {
                +PeakRefreshRateSwitchPreference()
                if (Flags.catalystScreensaver()) +ScreensaverScreen.KEY
            }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, DisplaySettingsActivity::class.java, metadata?.key)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_top_level_display)

    companion object {
        const val KEY = "display_settings_screen"
    }
}
