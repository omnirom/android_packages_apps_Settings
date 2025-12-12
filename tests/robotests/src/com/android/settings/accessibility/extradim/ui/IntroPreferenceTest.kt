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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settingslib.widget.TopIntroPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntroPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = IntroPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo(IntroPreference.KEY)
    }

    @Test
    fun getTitle_returnCorrectTitleRes() {
        assertThat(preference.title).isEqualTo(R.string.reduce_bright_colors_preference_intro_text)
    }

    @Test
    fun isIndexable_returnFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun createWidget_returnTopIntroPreference() {
        assertThat(preference.createWidget(context)).isInstanceOf(TopIntroPreference::class.java)
    }
}
