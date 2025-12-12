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
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import android.view.accessibility.Flags
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DarkModeTopIntroPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preference = DarkModeTopIntroPreference()

    @Test
    fun key() {
        assertThat(preference.key).isEqualTo("dark_ui_top_intro")
    }

    @DisableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun getTitle_withoutForceInvert() {
        assertThat(preference.getTitle(context)).isEqualTo(context.getString(R.string.dark_ui_text))
    }

    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    @Test
    fun getTitle_withForceInvert() {
        assertThat(preference.getTitle(context))
            .isEqualTo(context.getString(R.string.dark_ui_text_force_invert))
    }

    @Test
    fun isIndexable() {
        assertThat(preference.indexable).isFalse()
    }
}
