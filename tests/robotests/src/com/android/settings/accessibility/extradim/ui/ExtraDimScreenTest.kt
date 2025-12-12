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

import android.app.settings.SettingsEnums
import android.provider.Settings.ACTION_REDUCE_BRIGHT_COLORS_SETTINGS
import com.android.internal.R as AndroidInternalR
import com.android.settings.R
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleReduceBrightColorsPreferenceFragment
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.shadow.SettingsShadowResources
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.robolectric.annotation.Config

@Config(shadows = [SettingsShadowResources::class])
class ExtraDimScreenTest : SettingsCatalystTestCase() {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    override val flagName = Flags.FLAG_CATALYST_EXTRA_DIM
    override val preferenceScreenCreator by lazy { ExtraDimScreen(appContext) }

    @Before
    fun setUp() {
        SettingsShadowResources.overrideResource(
            AndroidInternalR.bool.config_reduceBrightColorsAvailable,
            true,
        )
        SettingsShadowResources.overrideResource(
            AndroidInternalR.bool.config_evenDimmerEnabled,
            false,
        )
    }

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo("reduce_bright_colors_preference")
    }

    @Test
    fun getHighlightMenuKey_returnsAccessibilityKey() {
        assertThat(preferenceScreenCreator.highlightMenuKey)
            .isEqualTo(R.string.menu_key_accessibility)
    }

    @Test
    fun getKeywords_returnsCorrectKeywords() {
        assertThat(preferenceScreenCreator.keywords)
            .isEqualTo(R.string.keywords_reduce_bright_colors)
    }

    @Test
    fun getTitle_returnsCorrectTitle() {
        assertThat(preferenceScreenCreator.title)
            .isEqualTo(R.string.reduce_bright_colors_preference_title)
    }

    @Test
    fun getIcon_returnsCorrectIcon() {
        assertThat(preferenceScreenCreator.icon).isEqualTo(R.drawable.ic_reduce_bright_colors)
    }

    @Test
    fun getSummary_returnsCorrectSummary() {
        assertThat(preferenceScreenCreator.summary)
            .isEqualTo(R.string.reduce_bright_colors_preference_summary)
    }

    @Test
    fun getFragmentClass_returnsCorrectFragmentClass() {
        assertThat(preferenceScreenCreator.fragmentClass())
            .isEqualTo(ToggleReduceBrightColorsPreferenceFragment::class.java)
    }

    @Test
    fun getMetricsCategory_returnsCorrectCategory() {
        assertThat(preferenceScreenCreator.metricsCategory)
            .isEqualTo(SettingsEnums.REDUCE_BRIGHT_COLORS_SETTINGS)
    }

    @Test
    fun isAvailable_extraDimAvailable_returnTrue() {
        SettingsShadowResources.overrideResource(
            AndroidInternalR.bool.config_reduceBrightColorsAvailable,
            true,
        )
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isTrue()
    }

    @Test
    fun isAvailable_extraDimNotAvailable_returnFalse() {
        SettingsShadowResources.overrideResource(
            AndroidInternalR.bool.config_reduceBrightColorsAvailable,
            false,
        )
        assertThat(preferenceScreenCreator.isAvailable(appContext)).isFalse()
    }

    @Test
    fun getReadPermissions_returnsCorrectPermissions() {
        assertThat(preferenceScreenCreator.getReadPermissions(appContext))
            .isEqualTo(ExtraDimDataStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsCorrectPermissions() {
        assertThat(preferenceScreenCreator.getWritePermissions(appContext))
            .isEqualTo(ExtraDimDataStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preferenceScreenCreator.getReadPermit(appContext, 0, 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preferenceScreenCreator.getWritePermit(appContext, 0, 0))
            .isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getLaunchIntent_returnsCorrectIntent() {
        val prefKey = "fakePrefKey"
        val mockPrefMetadata = mock<PreferenceMetadata> { on { key } doReturn prefKey }
        val launchIntent = preferenceScreenCreator.getLaunchIntent(appContext, mockPrefMetadata)

        assertThat(launchIntent).isNotNull()
        assertThat(launchIntent.action).isEqualTo(ACTION_REDUCE_BRIGHT_COLORS_SETTINGS)
        assertThat(launchIntent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(prefKey)
    }
}
