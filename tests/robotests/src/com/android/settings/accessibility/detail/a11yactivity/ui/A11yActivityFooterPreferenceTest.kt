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

package com.android.settings.accessibility.detail.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.Context
import android.content.pm.ActivityInfo
import android.text.Html
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests for [A11yActivityFooterPreference]. */
@RunWith(RobolectricTestRunner::class)
class A11yActivityFooterPreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun getTitle_nonHtml_returnsCorrectTitle() {
        val preference =
            A11yActivityFooterPreference(KEY, createFakeShortcutInfo(), loadHtmlFooter = false)

        assertThat(preference.getTitle(context)).isEqualTo(FEATURE_FOOTER)
    }

    @Test
    fun getTitle_html_returnsCorrectTitle() {
        val preference =
            A11yActivityFooterPreference(KEY, createFakeShortcutInfo(), loadHtmlFooter = true)

        assertThat(preference.getTitle(context).toString())
            .isEqualTo(
                Html.fromHtml(
                        FEATURE_HTML_FOOTER,
                        Html.FROM_HTML_MODE_COMPACT,
                        /* imageGetter= */ null,
                        /* tagHandler= */ null,
                    )
                    .toString()
            )
    }

    @Test
    fun getIntroductionTitle() {
        val preference =
            A11yActivityFooterPreference(KEY, createFakeShortcutInfo(), loadHtmlFooter = false)

        assertThat(preference.getIntroductionTitle(context))
            .isEqualTo(context.getString(R.string.accessibility_introduction_title, FEATURE_NAME))
    }

    @Test
    fun isAvailable_hasTitle_returnTrue() {
        val preference =
            A11yActivityFooterPreference(KEY, createFakeShortcutInfo(), loadHtmlFooter = false)

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_noTitle_returnFalse() {
        val preference = A11yActivityFooterPreference(KEY, mock(), loadHtmlFooter = false)

        assertThat(preference.isAvailable(context)).isFalse()
    }

    private fun createFakeShortcutInfo(): AccessibilityShortcutInfo {
        val mockActivityInfo =
            mock<ActivityInfo> { on { loadLabel(context.packageManager) } doReturn FEATURE_NAME }
        return mock {
            on { activityInfo } doReturn mockActivityInfo
            on { loadDescription(context.packageManager) } doReturn FEATURE_FOOTER
            on { loadHtmlDescription(context.packageManager) } doReturn FEATURE_HTML_FOOTER
        }
    }

    companion object {
        private const val KEY = "prefKey"
        private const val FEATURE_NAME = "Fake A11y Feature"
        private const val FEATURE_FOOTER = "feature footer"
        private const val FEATURE_HTML_FOOTER = "<b>default html description</b><br/>"
    }
}
