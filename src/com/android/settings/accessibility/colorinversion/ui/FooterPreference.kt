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

package com.android.settings.accessibility.colorinversion.ui

import android.content.Context
import android.text.Html
import com.android.settings.R
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider

class FooterPreference :
    AccessibilityFooterPreferenceMetadata,
    AccessibilityFooterPreferenceBinding,
    PreferenceTitleProvider {
    override val key: String
        get() = KEY

    override fun getTitle(context: Context): CharSequence? {
        return Html.fromHtml(
            context.getString(R.string.accessibility_display_inversion_preference_subtitle),
            Html.FROM_HTML_MODE_COMPACT,
            /* imageGetter= */ null,
            /* tagHandler= */ null,
        )
    }

    override val introductionTitle: Int
        get() = R.string.accessibility_color_inversion_about_title

    override val helpResource: Int
        get() = R.string.help_url_color_inversion

    override val learnMoreText: Int
        get() = R.string.accessibility_color_inversion_footer_learn_more_content_description

    companion object {
        const val KEY = "html_description"
    }
}
