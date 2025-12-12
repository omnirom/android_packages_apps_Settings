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

package com.android.settings.accessibility.hearingdevices.ui

import android.content.Context
import android.text.Html
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.HearingAidHelper
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HearingDevicesFooterPreferenceTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val helper: HearingAidHelper = mock<HearingAidHelper>()
    private val preference = HearingDevicesFooterPreference(context, helper)

    @Test
    fun isAvailable_hearingAidSupported_returnTrue() {
        helper.stub { on { isHearingAidSupported } doReturn true }

        assertThat(preference.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_hearingAidNotSupported_returnFalse() {
        helper.stub { on { isHearingAidSupported } doReturn false }

        assertThat(preference.isAvailable(context)).isFalse()
    }

    @Test
    fun getTitle_ashaHapSupported_titleCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn true
            on { isHapClientProfileSupported } doReturn true
        }

        val expectedTitle =
            Html.fromHtml(
                    context.getString(R.string.accessibility_hearing_device_footer_summary),
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
                .toString()
        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedTitle)
    }

    @Test
    fun getTitle_ashaSupported_titleCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn true
            on { isHapClientProfileSupported } doReturn false
        }

        val expectedTitle =
            Html.fromHtml(
                    context.getString(
                        R.string.accessibility_hearing_device_footer_asha_only_summary
                    ),
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
                .toString()
        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedTitle)
    }

    @Test
    fun getTitle_hapSupported_titleCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn false
            on { isHapClientProfileSupported } doReturn true
        }

        val expectedTitle =
            Html.fromHtml(
                    context.getString(
                        R.string.accessibility_hearing_device_footer_hap_only_summary
                    ),
                    Html.FROM_HTML_MODE_COMPACT,
                    /* imageGetter= */ null,
                    /* tagHandler= */ null,
                )
                .toString()
        assertThat(preference.getTitle(context).toString()).isEqualTo(expectedTitle)
    }

    @Test
    fun bind_ashaHapSupported_contentDescriptionCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn true
            on { isHapClientProfileSupported } doReturn true
        }

        val footerPreference = mock<FooterPreference> { on { context } doReturn context }
        preference.bind(footerPreference, preference)

        val aboutTitle = context.getString(R.string.accessibility_hearing_device_about_title)
        val expectedResId = R.string.accessibility_hearing_device_footer_summary_tts
        val expectedString = "$aboutTitle\n${context.getString(expectedResId)}"
        verify(footerPreference).contentDescription = expectedString
    }

    @Test
    fun bind_ashaSupported_contentDescriptionCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn true
            on { isHapClientProfileSupported } doReturn false
        }

        val footerPreference = mock<FooterPreference> { on { context } doReturn context }
        preference.bind(footerPreference, preference)

        val aboutTitle = context.getString(R.string.accessibility_hearing_device_about_title)
        val expectedResId = R.string.accessibility_hearing_device_footer_asha_only_summary_tts
        val expectedString = "$aboutTitle\n${context.getString(expectedResId)}"
        verify(footerPreference).contentDescription = expectedString
    }

    @Test
    fun bind_hapSupported_contentDescriptionCorrect() {
        helper.stub {
            on { isAshaProfileSupported } doReturn false
            on { isHapClientProfileSupported } doReturn true
        }

        val footerPreference = mock<FooterPreference> { on { context } doReturn context }
        preference.bind(footerPreference, preference)

        val aboutTitle = context.getString(R.string.accessibility_hearing_device_about_title)
        val expectedResId = R.string.accessibility_hearing_device_footer_hap_only_summary_tts
        val expectedString = "$aboutTitle\n${context.getString(expectedResId)}"
        verify(footerPreference).contentDescription = expectedString
    }
}
