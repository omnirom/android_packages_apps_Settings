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

package com.android.settings.display.darkmode

import android.app.settings.SettingsEnums
import android.content.Context
import android.icu.text.MessageFormat
import android.view.View
import android.view.accessibility.Flags
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settings.notification.modes.ZenModesListFragment
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.widget.FooterPreference
import java.util.Locale

// LINT.IfChange
class DarkModeCustomModesFooterPreference :
    FooterPreferenceMetadata, FooterPreferenceBinding, PreferenceTitleProvider {

    override val key: String
        get() = KEY

    override fun getTitle(context: Context): CharSequence? {
        val modesUsingDarkTheme = AutoDarkTheme.getModesThatChangeDarkTheme(context)
        val titleFormat =
            MessageFormat(
                context.getString(R.string.dark_ui_modes_footer_summary),
                Locale.getDefault(),
            )
        val args = mutableMapOf<String, Any>()
        args.put("count", modesUsingDarkTheme.size)
        for (i in 0 until modesUsingDarkTheme.size.coerceAtMost(3)) {
            args.put("mode_${i + 1}", modesUsingDarkTheme[i])
        }
        return titleFormat.format(args)
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val footerPreference = preference as FooterPreference
        val context = preference.context
        footerPreference.setLearnMoreAction { view ->
            SubSettingLauncher(context)
                .setDestination(ZenModesListFragment::class.java.getName())
                .setSourceMetricsCategory(SettingsEnums.DARK_UI_SETTINGS)
                .launch()
        }
        footerPreference.setLearnMoreText(context.getString(R.string.dark_ui_modes_footer_action))
        if (Flags.forceInvertColor()) {
            footerPreference.setIconVisibility(View.GONE)
            footerPreference.order = DarkModePreferenceOrderUtil.Order.MODES_FOOTER.value
        }
    }

    companion object {
        const val KEY = "dark_theme_custom_bedtime_footer"
    }
}
// LINT.ThenChange(DarkModeCustomModesPreferenceController.java)
