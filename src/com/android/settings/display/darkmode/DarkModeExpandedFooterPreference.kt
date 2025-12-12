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

import android.content.Context
import android.view.accessibility.Flags
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.HelpUtils
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.FooterPreference

// LINT.IfChange
class DarkModeExpandedFooterPreference :
    FooterPreferenceMetadata, FooterPreferenceBinding, PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override fun isAvailable(context: Context): Boolean = Flags.forceInvertColor()

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val footerPreference = preference as FooterPreference
        val context = preference.context

        footerPreference.order = DarkModePreferenceOrderUtil.Order.EXPANDED_DARK_THEME_FOOTER.value
        val helpIntent =
            HelpUtils.getHelpIntent(
                context,
                context.getString(R.string.help_url_dark_theme_link),
                context.javaClass.getName(),
            )
        if (helpIntent != null) {
            footerPreference.setLearnMoreAction { view ->
                view?.startActivityForResult(helpIntent, /* requestCode= */ 0)
            }
            footerPreference.setLearnMoreText(
                context.getString(R.string.accessibility_dark_theme_footer_learn_more_helper_link)
            )
        }
    }

    companion object {
        const val KEY = "dark_theme_expanded_footer"
    }
}
// LINT.ThenChange(DarkModeExpandedFooterPreferenceController.java)
