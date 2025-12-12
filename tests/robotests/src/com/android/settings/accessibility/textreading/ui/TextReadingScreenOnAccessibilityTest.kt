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

package com.android.settings.accessibility.textreading.ui

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** Tests for [TextReadingScreenOnAccessibility]. */
class TextReadingScreenOnAccessibilityTest : SettingsCatalystTestCase() {
    private val context: Context = ApplicationProvider.getApplicationContext()
    override val preferenceScreenCreator = TextReadingScreenOnAccessibility()

    override val flagName: String
        get() = Flags.FLAG_CATALYST_TEXT_READING_SCREEN

    @Test override fun migration() {}

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(TextReadingScreenOnAccessibility.KEY)
    }

    @Test
    fun isIndexable() {
        assertThat(preferenceScreenCreator.indexable).isFalse()
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_text_reading_options_title)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getLaunchIntent() {
        assertThat(preferenceScreenCreator.getLaunchIntent(context, null)).isNull()
    }

    @Test
    fun getEntryPoint() {
        assertThat(preferenceScreenCreator.entryPoint)
            .isEqualTo(TextReadingPreferenceFragment.EntryPoint.ACCESSIBILITY_SETTINGS)
    }

    @Test
    fun getIcon() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_adaptive_font_download)
    }
}
