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
package com.android.settings.supervision

import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.BROWSER_CONTENT_FILTERS_ENABLED
import android.provider.Settings.SettingNotFoundException
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSitesPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dataStore: SupervisionSafeSitesDataStore
    private lateinit var allowAllSitesPreference: SupervisionAllowAllSitesPreference
    private lateinit var blockExplicitSitesPreference: SupervisionBlockExplicitSitesPreference

    @Before
    fun setUp() {
        dataStore = SupervisionSafeSitesDataStore(context)
        allowAllSitesPreference = SupervisionAllowAllSitesPreference(dataStore)
        blockExplicitSitesPreference = SupervisionBlockExplicitSitesPreference(dataStore)
    }

    @Test
    fun getTitle_allowAllSites() {
        assertThat(allowAllSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_allow_all_sites_title)
    }

    @Test
    fun getTitle_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_browser_block_explicit_sites_title)
    }

    @Test
    fun getSummary_blockExplicitSites() {
        assertThat(blockExplicitSitesPreference.summary)
            .isEqualTo(
                R.string.supervision_web_content_filters_browser_block_explicit_sites_summary
            )
    }

    @Test
    fun allowAllSitesIsChecked_whenNoValueIsSet() {
        assertThrows(SettingNotFoundException::class.java) {
            Settings.Secure.getInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED)
        }
        assertThat(getBlockExplicitSitesWidget().isChecked).isFalse()
        assertThat(getAllowAllSitesWidget().isChecked).isTrue()
    }

    @Test
    fun blockExplicitSitesIsChecked_whenPreviouslyEnabled() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 1)
        assertThat(getAllowAllSitesWidget().isChecked).isFalse()
        assertThat(getBlockExplicitSitesWidget().isChecked).isTrue()
    }

    @Test
    fun clickBlockExplicitSites_enablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 0)
        val blockExplicitSitesWidget = getBlockExplicitSitesWidget()
        assertThat(blockExplicitSitesWidget.isChecked).isFalse()

        blockExplicitSitesWidget.performClick()

        assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(),
                BROWSER_CONTENT_FILTERS_ENABLED,
            )
        )
            .isEqualTo(1)
        assertThat(blockExplicitSitesWidget.isChecked).isTrue()
    }

    @Test
    fun clickAllowAllSites_disablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), BROWSER_CONTENT_FILTERS_ENABLED, 1)
        val allowAllSitesWidget = getAllowAllSitesWidget()
        assertThat(allowAllSitesWidget.isChecked).isFalse()

        allowAllSitesWidget.performClick()

        assertThat(
            Settings.Secure.getInt(
                context.getContentResolver(),
                BROWSER_CONTENT_FILTERS_ENABLED,
            )
        )
            .isEqualTo(0)
        assertThat(allowAllSitesWidget.isChecked).isTrue()
    }

    private fun getBlockExplicitSitesWidget(): SelectorWithWidgetPreference {
        return blockExplicitSitesPreference.createAndBindWidget(context)
    }

    private fun getAllowAllSitesWidget(): SelectorWithWidgetPreference {
        return allowAllSitesPreference.createAndBindWidget(context)
    }
}
