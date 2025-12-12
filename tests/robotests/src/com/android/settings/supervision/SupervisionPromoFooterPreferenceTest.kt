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
import android.content.pm.ResolveInfo
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.SupervisionPromoFooterPreference.Companion.KEY
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.widget.CardPreference
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SupervisionPromoFooterPreferenceTest {
    private val mockPackageManager: PackageManager = mock()
    private val context: Context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getPackageManager() = mockPackageManager
        }
    private val preference = spy(CardPreference(context))

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
    fun onResume_setTitle() =
        testScope.runTest {
            val title = "test title"
            preferenceData = PreferenceData(title = title)

            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)

            promoPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(2)).notifyPreferenceChange(KEY)
            promoPreference.bind(preference, mock())

            assertThat(preference.title).isEqualTo(title)
        }

    @Test
    fun onResume_setSummary() =
        testScope.runTest {
            val summary = "test summary"
            preferenceData = PreferenceData(summary = summary)

            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)

            promoPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(2)).notifyPreferenceChange(KEY)
            promoPreference.bind(preference, mock())

            assertThat(preference.summary).isEqualTo(summary)
        }

    @Test
    fun onResume_loadingIconSetFromSupervisionPackage() =
        testScope.runTest {
            preferenceData = PreferenceData(icon = 123)
            preferenceDataProvider.stub { on { packageName } doReturn "test.package" }
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)

            promoPreference.onResume(preferenceLifecycleContext)
            verify(preferenceLifecycleContext, times(2)).notifyPreferenceChange(KEY)
            promoPreference.bind(preference, mock())
        }

    @Test
    fun onResume_emptyPreferenceData_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData = null

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext).notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
            verify(mockPackageManager, never())
                .queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>())
        }

    @Test
    fun onResume_noActivitiesCanHandleIntent_preferenceIsHidden() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            preferenceData =
                PreferenceData(
                    action = "Test Action",
                    intentData = "url",
                )

            mockPackageManager.stub {
                on { queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>()) }
                    .thenReturn(emptyList())
            }

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext, times(2))
                .notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())

            assertThat(preference.isVisible).isFalse()
        }

    @Test
    fun onResume_validIntent_hasActivityToHandleIntent_preferenceIsVisible_validIntentCreated() =
        testScope.runTest {
            val promoPreference =
                SupervisionPromoFooterPreference(preferenceDataProvider, testDispatcher)
            val intentData = "https://www.familylink.com"
            preferenceData =
                PreferenceData(
                    title = "Test Title",
                    action = "Test Action",
                    intentData = intentData,
                )

            mockPackageManager.stub {
                on { queryIntentActivitiesAsUser(any(), any<Int>(), any<Int>()) }
                    .thenReturn(listOf(ResolveInfo()))
            }

            promoPreference.onResume(preferenceLifecycleContext)

            verify(preferenceLifecycleContext, times(2))
                .notifyPreferenceChange(KEY) // will trigger binding
            promoPreference.bind(preference, mock())
            verify(preference, atLeastOnce()).setAdditionalAction(
                null,
                context.getString(R.string.supervision_promo_footer_action_button_description),
            ) {
                it.performClick()
            }

            assertThat(preference.isVisible).isTrue()
            val intent = preference.intent!!
            assertThat(intent.action).isEqualTo("Test Action")
            assertThat(intent.data).isEqualTo(intentData.toUri())
        }
}
