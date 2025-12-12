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
import android.content.ContextWrapper
import android.content.pm.PackageManager
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.supervision.SupervisionAocFooterPreference.Companion.KEY
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.widget.FooterPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SupervisionAocFooterPreferenceTest {
    private val mockPackageManager: PackageManager = mock()
    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager() = mockPackageManager
        }
    private val preference = FooterPreference(context)

    private var preferenceData: PreferenceData? = null

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
        on { lifecycleScope }.thenReturn(testScope)
        on { packageManager }.thenReturn(mockPackageManager)
        on { findPreference<Preference>(any()) }.thenReturn(preference)
    }
    private val preferenceDataProvider: PreferenceDataProvider = mock {
        onBlocking { getPreferenceData(any()) }
            .thenAnswer {
                when (preferenceData) {
                    null -> mapOf<String, PreferenceData>()
                    else -> mapOf(KEY to preferenceData)
                }
            }
    }

    @Test
    fun onResume_allGood_titleIsSet_preferenceIsVisible() =
        testScope.runTest {
            preferenceData = PreferenceData(title = PREFERENCE_TITLE, isVisible = true)

            val aosFooterPreference =
                SupervisionAocFooterPreference(preferenceDataProvider, testDispatcher)

            aosFooterPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(1)).notifyPreferenceChange(KEY)
            aosFooterPreference.bind(preference, mock())

            assertThat(preference.isVisible).isTrue()
            assertThat(preference.title.toString()).isEqualTo(PREFERENCE_TITLE)
        }

    @Test
    fun onResume_notVisible_preferenceIsHidden() =
        testScope.runTest {
            preferenceData = PreferenceData(title = PREFERENCE_TITLE, isVisible = false)

            val aosFooterPreference =
                SupervisionAocFooterPreference(preferenceDataProvider, testDispatcher)

            aosFooterPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(1)).notifyPreferenceChange(KEY)
            aosFooterPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
            assertThat(preference.title.toString()).isEqualTo(PREFERENCE_TITLE)
        }

    @Test
    fun onResume_noTitle_preferenceIsHidden() =
        testScope.runTest {
            preferenceData = PreferenceData(isVisible = false)

            val aosFooterPreference =
                SupervisionAocFooterPreference(preferenceDataProvider, testDispatcher)

            aosFooterPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(1)).notifyPreferenceChange(KEY)
            aosFooterPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
        }

    private companion object {
        const val PREFERENCE_TITLE = "test title"
    }
}
