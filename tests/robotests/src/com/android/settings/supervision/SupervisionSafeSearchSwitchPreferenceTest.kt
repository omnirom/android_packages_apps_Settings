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

import android.app.Activity
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SEARCH_FILTER_OFF
import android.app.settings.SettingsEnums.ACTION_SUPERVISION_SEARCH_FILTER_ON
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.provider.Settings.Secure.SEARCH_CONTENT_FILTERS_ENABLED
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.testutils.MetricsRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@RunWith(AndroidJUnit4::class)
class SupervisionSafeSearchSwitchPreferenceTest {
    @get:Rule val metricsRule = MetricsRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dataStore = SupervisionSafeSearchDataStore(context)
    private val switchPreference = SupervisionSafeSearchSwitchPreference(dataStore)

    private val mockActivityResultLauncher: ActivityResultLauncher<Intent> = mock()
    private val mockPackageManager: PackageManager = mock {
        on { queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()) } doReturn
            listOf(ResolveInfo())
    }
    private val mockLifeCycleContext: PreferenceLifecycleContext = mock {
        on { packageManager } doReturn mockPackageManager
        on { registerForActivityResult(any<StartActivityForResult>(), any()) } doReturn
            mockActivityResultLauncher
    }

    private var dataStoreValue: Int?
        get() = SettingsSecureStore.get(context).getInt(SEARCH_CONTENT_FILTERS_ENABLED)
        set(value) = SettingsSecureStore.get(context).setInt(SEARCH_CONTENT_FILTERS_ENABLED, value)

    @Before
    fun setUp() {
        switchPreference.onCreate(mockLifeCycleContext)
    }

    @Test
    fun getTitle() {
        assertThat(switchPreference.title)
            .isEqualTo(R.string.supervision_web_content_filters_search_filter_title)
    }

    @Test
    fun filterOffIsChecked_whenNoValueIsSet() {
        dataStoreValue = null
        assertThat(switchPreference.createWidget().isChecked).isFalse()
    }

    @Test
    fun filterOnIsChecked_whenPreviouslyEnabled() {
        dataStoreValue = 1
        assertThat(switchPreference.createWidget().isChecked).isTrue()
    }

    @Test
    fun switchToFilterOn_failedToEnablesFilter_activityFailed() {
        dataStoreValue = 0
        val searchSwitchWidget = switchPreference.createWidget()
        assertThat(searchSwitchWidget.isChecked).isFalse()

        searchSwitchWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        switchPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_CANCELED, null))

        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(searchSwitchWidget.isChecked).isFalse()
    }

    @Test
    fun switchToFilterOn_unresolvedIntent_activityNotLaunched() {
        mockPackageManager.stub {
            on { queryIntentActivitiesAsUser(any<Intent>(), anyInt(), anyInt()) } doReturn listOf()
        }

        dataStoreValue = 0
        val searchSwitchWidget = switchPreference.createWidget()
        assertThat(searchSwitchWidget.isChecked).isFalse()

        searchSwitchWidget.performClick()

        verify(mockActivityResultLauncher, never()).launch(any())
        verifyNoInteractions(metricsRule.metricsFeatureProvider)
        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(searchSwitchWidget.isChecked).isFalse()
    }

    @Test
    fun switchToFilterOn_enablesFilter() {
        dataStoreValue = -1
        val searchSwitchWidget = switchPreference.createWidget()
        assertThat(searchSwitchWidget.isChecked).isFalse()

        searchSwitchWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        switchPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(dataStoreValue).isEqualTo(1)
        assertThat(searchSwitchWidget.isChecked).isTrue()
        verify(metricsRule.metricsFeatureProvider)
            .action(context, ACTION_SUPERVISION_SEARCH_FILTER_ON)
    }

    @Test
    fun switchToFilterOff_disablesFilter() {
        dataStoreValue = 1
        val searchSwitchWidget = switchPreference.createWidget()
        assertThat(searchSwitchWidget.isChecked).isTrue()

        searchSwitchWidget.performClick()
        verifyConfirmSupervisionCredentialsActivity()
        switchPreference.onConfirmCredentials(ActivityResult(Activity.RESULT_OK, null))

        assertThat(dataStoreValue).isEqualTo(0)
        assertThat(searchSwitchWidget.isChecked).isFalse()
        verify(metricsRule.metricsFeatureProvider)
            .action(context, ACTION_SUPERVISION_SEARCH_FILTER_OFF)
    }

    private fun SupervisionSafeSearchSwitchPreference.createWidget() =
        createAndBindWidget<SwitchPreferenceCompat>(context).also { widget ->
            mockLifeCycleContext.stub { on { requirePreference<Preference>(key) } doReturn widget }
        }

    private fun verifyConfirmSupervisionCredentialsActivity() {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockActivityResultLauncher).launch(intentCaptor.capture())

        assertThat(intentCaptor.allValues.size).isEqualTo(1)
        val intent = intentCaptor.firstValue
        assertThat(intent.action)
            .isEqualTo("android.app.supervision.action.CONFIRM_SUPERVISION_CREDENTIALS")
        assertThat(intent.`package`).isEqualTo("com.android.settings")
    }
}
