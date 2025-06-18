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
import android.provider.Settings.Secure.SEARCH_CONTENT_FILTERS_ENABLED
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
class SupervisionSafeSearchPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var dataStore: SupervisionSafeSearchDataStore
    private lateinit var searchFilterOffPreference: SupervisionSearchFilterOffPreference
    private lateinit var searchFilterOnPreference: SupervisionSearchFilterOnPreference

    @Before
    fun setUp() {
        dataStore = SupervisionSafeSearchDataStore(context)
        searchFilterOffPreference = SupervisionSearchFilterOffPreference(dataStore)
        searchFilterOnPreference = SupervisionSearchFilterOnPreference(dataStore)
    }

    @Test
    fun getTitle_filterOn() {
        assertThat(searchFilterOnPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_on_title)
    }

    @Test
    fun getSummary_filterOn() {
        assertThat(searchFilterOnPreference.summary)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_on_summary)
    }

    @Test
    fun getTitle_filterOff() {
        assertThat(searchFilterOffPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_off_title)
    }

    @Test
    fun getSummary_filterOff() {
        assertThat(searchFilterOffPreference.summary)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_off_summary)
    }

    @Test
    fun filterOffIsChecked_whenNoValueIsSet() {
        assertThrows(SettingNotFoundException::class.java) {
            Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
        }
        assertThat(getFilterOnWidget().isChecked).isFalse()
        assertThat(getFilterOffWidget().isChecked).isTrue()
    }

    @Test
    fun filterOnIsChecked_whenPreviouslyEnabled() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 1)
        assertThat(getFilterOffWidget().isChecked).isFalse()
        assertThat(getFilterOnWidget().isChecked).isTrue()
    }

    @Test
    fun clickBlockExplicitSites_enablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 0)
        val filterOnWidget = getFilterOnWidget()
        assertThat(filterOnWidget.isChecked).isFalse()

        filterOnWidget.performClick()

        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(1)
        assertThat(filterOnWidget.isChecked).isTrue()
    }

    @Test
    fun clickAllowAllSites_disablesFilter() {
        Settings.Secure.putInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED, 1)
        val filterOffWidget = getFilterOffWidget()
        assertThat(filterOffWidget.isChecked).isFalse()

        filterOffWidget.performClick()

        assertThat(
                Settings.Secure.getInt(context.getContentResolver(), SEARCH_CONTENT_FILTERS_ENABLED)
            )
            .isEqualTo(0)
        assertThat(filterOffWidget.isChecked).isTrue()
    }

    private fun getFilterOnWidget(): SelectorWithWidgetPreference {
        return searchFilterOnPreference.createAndBindWidget(context)
    }

    private fun getFilterOffWidget(): SelectorWithWidgetPreference {
        return searchFilterOffPreference.createAndBindWidget(context)
    }
}
