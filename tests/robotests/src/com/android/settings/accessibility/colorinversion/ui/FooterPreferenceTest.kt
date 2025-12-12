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
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FooterPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference: FooterPreference = FooterPreference()

    @Test
    fun getTitle_returnsCorrectTitleText() {
        val title = preference.getTitle(context)

        assertThat(title.toString())
            .isEqualTo(
                Html.fromHtml(
                        context.getString(
                            R.string.accessibility_display_inversion_preference_subtitle
                        ),
                        Html.FROM_HTML_MODE_COMPACT,
                        /* imageGetter= */ null,
                        /* tagHandler= */ null,
                    )
                    .toString()
            )
    }

    @Test
    fun getIntroductionTitle_returnsCorrectTitleRes() {
        assertThat(preference.introductionTitle)
            .isEqualTo(R.string.accessibility_color_inversion_about_title)
    }

    @Test
    fun getHelpResource_returnsCorrectHelpResource() {
        assertThat(preference.helpResource).isEqualTo(R.string.help_url_color_inversion)
    }

    @Test
    fun getLearnMoreText_returnsCorrectLearnMoreText() {
        assertThat(preference.learnMoreText)
            .isEqualTo(R.string.accessibility_color_inversion_footer_learn_more_content_description)
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(preference.key).isEqualTo(FooterPreference.KEY)
    }
}
