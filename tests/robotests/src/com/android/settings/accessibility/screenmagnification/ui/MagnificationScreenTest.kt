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

package com.android.settings.accessibility.screenmagnification.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags as serverFlags
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.Flags
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test

class MagnificationScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingStoreRule = SettingsStoreRule()
    override val preferenceScreenCreator = MagnificationScreen()
    override val flagName: String
        get() = Flags.FLAG_CATALYST_MAGNIFICATION

    private val context: Context = ApplicationProvider.getApplicationContext()

    override fun migration() {
        // For update the availability of the one-finger panning switch preference.
        AccessibilityTestUtils.setWindowMagnificationSupported(context, true)
        setFlagsRule.enableFlags(serverFlags.FLAG_ENABLE_MAGNIFICATION_ONE_FINGER_PANNING_GESTURE)
        super.migration()
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo("magnification_preference_screen")
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.accessibility_screen_magnification_title)
    }

    @Test
    fun getKeywords() {
        assertThat(preferenceScreenCreator.keywords).isEqualTo(R.string.keywords_magnification)
    }

    @Test
    fun getHighlightMenuKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getIcon() {
        assertThat(preferenceScreenCreator.icon)
            .isEqualTo(R.drawable.ic_accessibility_magnification)
    }

    @Test
    fun getMetricsCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION)
    }

    @Test
    fun isIndexable() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MAGNIFICATION)
    fun hasCompleteHierarchy_isTrue() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MAGNIFICATION)
    fun hasCompleteHierarchy_isFalse() {
        assertThat(preferenceScreenCreator.hasCompleteHierarchy()).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_CATALYST_MAGNIFICATION)
    fun isFlagEnabled_isTrue() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_CATALYST_MAGNIFICATION)
    fun isFlagEnabled_isFalse() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    fun getLaunchIntent_noMetadata_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(context, null)!!

        assertThat(underTest.getComponent()?.getClassName())
            .isEqualTo(Settings.MagnificationActivity::class.java.getName())
        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isFalse()
    }

    @Test
    fun getLaunchIntent_metadata_correctActivityWithExtraKey() {
        val underTest =
            preferenceScreenCreator.getLaunchIntent(context, TestMetadata("preference_key"))!!

        assertThat(underTest.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isTrue()
        assertThat(underTest.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo("preference_key")
    }
}

private data class TestMetadata(override val key: String) : PreferenceMetadata
