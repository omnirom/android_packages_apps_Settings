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
package com.android.settings.network.telephony.satellite

import android.content.Context
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SatelliteSettingsPreferenceCategoryControllerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferenceScreen = PreferenceManager(context).createPreferenceScreen(context)

    private val controller =
        SatelliteSettingsPreferenceCategoryController(context = context, key = KEY)

    @Test
    fun hasVisiblePreference_noCategory_returnFalse() = runBlocking {
        val result = controller.hasVisiblePreference(null)

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun hasVisiblePreference_has1VisiblePreference_returnTrue() = runBlocking {
        val category = PreferenceCategory(context)
        val preference = Preference(context)
        preference.isVisible = true
        preferenceScreen.addPreference(category)
        category.addPreference(preference)

        val result = controller.hasVisiblePreference(category)

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun hasVisiblePreference_has1VisiblePreferenceAnd1InvisiblePreference_returnTrue() =
        runBlocking {
            val category = PreferenceCategory(context)
            val preference = Preference(context)
            preference.isVisible = true
            val preference2 = Preference(context)
            preference2.isVisible = false
            preferenceScreen.addPreference(category)
            category.addPreference(preference)
            category.addPreference(preference2)

            val result = controller.hasVisiblePreference(category)

            assertThat(result).isEqualTo(true)
        }

    @Test
    fun hasVisiblePreference_has2InvisiblePreference_returnFalse() = runBlocking {
        val category = PreferenceCategory(context)
        val preference = Preference(context)
        preference.isVisible = false
        val preference2 = Preference(context)
        preference2.isVisible = false
        preferenceScreen.addPreference(category)
        category.addPreference(preference)
        category.addPreference(preference2)

        val result = controller.hasVisiblePreference(category)

        assertThat(result).isEqualTo(false)
    }

    companion object {
        private const val KEY = "telephony_satellite_setting_key"
    }
}
