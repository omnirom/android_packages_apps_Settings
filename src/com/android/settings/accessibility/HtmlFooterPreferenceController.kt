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

package com.android.settings.accessibility

import android.content.Context
import android.text.Html
import com.android.settings.R

/**
 * A preferenceController that handles displaying the footer with html format in
 * [AccessibilityFooterPreference]
 */
open class HtmlFooterPreferenceController(context: Context, prefKey: String) :
    AccessibilityFooterPreferenceController(context, prefKey) {

    /**
     * Converts the summary to html spannable by utilizing the [Html.fromHtml] with custom image
     * getter.
     */
    override fun setSummary(summary: CharSequence) {
        setSummary(summary, isHtml = true)
    }

    protected fun setSummary(summary: CharSequence, isHtml: Boolean) {
        val description: CharSequence =
            if (isHtml) {
                Html.fromHtml(
                    summary.toString(),
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
            } else {
                summary
            }
        super.setSummary(description)
    }
}

class ColorInversionFooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey) {
    init {
        introductionTitle = context.getString(R.string.accessibility_color_inversion_about_title)
        summary = context.getText(R.string.accessibility_display_inversion_preference_subtitle)
        setupHelpLink(
            R.string.help_url_color_inversion,
            context.getString(
                R.string.accessibility_color_inversion_footer_learn_more_content_description
            ),
        )
    }
}

class DaltonizerFooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey) {
    init {
        introductionTitle = context.getString(R.string.accessibility_daltonizer_about_title)
        summary =
            context.getText(
                com.android.settingslib.R.string
                    .accessibility_display_daltonizer_preference_subtitle
            )
        setupHelpLink(
            R.string.help_url_color_correction,
            context.getString(
                R.string.accessibility_daltonizer_footer_learn_more_content_description
            ),
        )
    }
}
