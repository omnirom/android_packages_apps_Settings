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

package com.android.settings.accessibility.colorcorrection.ui

import android.content.Context
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntroPreferenceTest {
    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val preference = IntroPreference()

    @Test
    fun bindWidget_verifyTitle() {
        val widget = preference.createAndBindWidget<Preference>(appContext)

        assertThat(widget.title.toString())
            .isEqualTo(appContext.getString(R.string.accessibility_daltonizer_about_intro_text))
    }

    @Test
    fun createWidget_isTopIntroPreference() {
        val widget = preference.createWidget(appContext)

        assertThat(widget).isInstanceOf(TopIntroPreference::class.java)
    }

    @Test
    fun isIndexable_returnsFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun getKey_returnsCorrectValue() {
        assertThat(preference.key).isEqualTo(IntroPreference.KEY)
    }
}
