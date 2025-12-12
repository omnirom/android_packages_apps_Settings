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
import androidx.test.core.app.ApplicationProvider
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner

/** Tests for [IntroPreference]. */
@RunWith(RobolectricTestRunner::class)
class IntroPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun getKey() {
        val introPreference = IntroPreference(mock())

        assertThat(introPreference.key).isEqualTo(IntroPreference.KEY)
    }

    @Test
    fun bindWidget_hasTitle_preferenceIsVisibleWithCorrectTitle() {
        val topIntroTest = "top intro text"
        val mockShortcutInfo = createMockShortcutInfo(topIntroTest)

        val introPreference = IntroPreference(mockShortcutInfo)
        val widget: TopIntroPreference = introPreference.createAndBindWidget(appContext)

        assertThat(widget.title).isEqualTo(topIntroTest)
        assertThat(widget.isVisible).isEqualTo(true)
    }

    @Test
    fun bindWidget_hasNoTitle_veryWidgetTypeAndPreferenceInvisible() {
        val mockShortcutInfo = createMockShortcutInfo(topIntro = "")

        val introPreference = IntroPreference(mockShortcutInfo)
        val widget: TopIntroPreference = introPreference.createAndBindWidget(appContext)

        assertThat(widget.title).isEqualTo("")
        assertThat(widget.isVisible).isEqualTo(false)
    }

    @Test
    fun isAvailable_hasTitle_returnsTrue() {
        val mockShortcutInfo = createMockShortcutInfo("Top intro text")
        val introPreference = IntroPreference(mockShortcutInfo)

        assertThat(introPreference.isAvailable(appContext)).isEqualTo(true)
    }

    @Test
    fun isAvailable_hasNoTitle_returnsFalse() {
        val mockShortcutInfo = createMockShortcutInfo(null)
        val introPreference = IntroPreference(mockShortcutInfo)

        assertThat(introPreference.isAvailable(appContext)).isEqualTo(false)
    }

    private fun createMockShortcutInfo(topIntro: String?): AccessibilityShortcutInfo {
        return mock { on { loadIntro(any()) } doReturn topIntro }
    }
}
