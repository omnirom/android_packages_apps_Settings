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

import android.app.supervision.flags.Flags
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SupervisionWebContentFiltersScreenTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var supervisionWebContentFiltersScreen: SupervisionWebContentFiltersScreen

    @Before
    fun setUp() {
        supervisionWebContentFiltersScreen = SupervisionWebContentFiltersScreen()
    }

    @Test
    fun key() {
        assertThat(supervisionWebContentFiltersScreen.key)
            .isEqualTo(SupervisionWebContentFiltersScreen.KEY)
    }

    @Test
    fun getTitle() {
        assertThat(supervisionWebContentFiltersScreen.title)
            .isEqualTo(R.string.supervision_web_content_filters_title)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun flagEnabled() {
        assertThat(supervisionWebContentFiltersScreen.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun flagDisabled() {
        assertThat(supervisionWebContentFiltersScreen.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSitesPreferences() {
        FragmentScenario.launchInContainer(supervisionWebContentFiltersScreen.fragmentClass())
            .onFragment { fragment ->
                val allowAllSitesPreference =
                    fragment.findPreference<SelectorWithWidgetPreference>(
                        SupervisionAllowAllSitesPreference.KEY
                    )!!
                val blockExplicitSitesPreference =
                    fragment.findPreference<SelectorWithWidgetPreference>(
                        SupervisionBlockExplicitSitesPreference.KEY
                    )!!

                assertThat(allowAllSitesPreference.isChecked).isTrue()
                assertThat(blockExplicitSitesPreference.isChecked).isFalse()

                blockExplicitSitesPreference.performClick()

                assertThat(blockExplicitSitesPreference.isChecked).isTrue()
                assertThat(allowAllSitesPreference.isChecked).isFalse()
            }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_WEB_CONTENT_FILTERS_SCREEN)
    fun switchSafeSearchPreferences() {
        FragmentScenario.launchInContainer(supervisionWebContentFiltersScreen.fragmentClass())
            .onFragment { fragment ->
                val searchFilterOffPreference =
                    fragment.findPreference<SelectorWithWidgetPreference>(
                        SupervisionSearchFilterOffPreference.KEY
                    )!!
                val searchFilterOnPreference =
                    fragment.findPreference<SelectorWithWidgetPreference>(
                        SupervisionSearchFilterOnPreference.KEY
                    )!!

                assertThat(searchFilterOffPreference.isChecked).isTrue()
                assertThat(searchFilterOnPreference.isChecked).isFalse()

                searchFilterOnPreference.performClick()

                assertThat(searchFilterOnPreference.isChecked).isTrue()
                assertThat(searchFilterOffPreference.isChecked).isFalse()
            }
    }
}
