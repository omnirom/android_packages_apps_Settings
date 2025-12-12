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
import android.app.supervision.SupervisionManager
import android.app.supervision.flags.Flags
import android.content.Context
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.supervision.SupervisionMainSwitchPreference.Companion.REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settingslib.ipc.MessengerServiceRule
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.launchFragmentScenario
import com.android.settingslib.widget.MainSwitchPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class SupervisionDashboardScreenTest {
    private val preferenceScreenCreator = SupervisionDashboardScreen()

    private val context: Context = ApplicationProvider.getApplicationContext()

    private val mockLifeCycleContext = mock<PreferenceLifecycleContext>()
    private val mockSupervisionManager = mock<SupervisionManager>()

    @get:Rule val setFlagsRule = SetFlagsRule()

    @get:Rule
    val serviceRule =
        MessengerServiceRule<SupervisionMessengerClient>(
            TestSupervisionMessengerService::class.java
        )

    @Before
    fun setUp() {
        mockLifeCycleContext.stub {
            on { preferenceScreenKey } doReturn preferenceScreenCreator.bindingKey
            on { getSystemService(SupervisionManager::class.java) } doReturn mockSupervisionManager
        }
    }

    @Test
    fun key() {
        assertThat(preferenceScreenCreator.key).isEqualTo(SupervisionDashboardScreen.KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun flagEnabled() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isTrue()
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun flagDisabled() {
        assertThat(preferenceScreenCreator.isFlagEnabled(context)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun toggleMainSwitch_pinVerificationSucceeded_enablesChildPreferences() {
        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val mainSwitchPreference =
                fragment.findPreference<MainSwitchPreference>(SupervisionMainSwitchPreference.KEY)!!
            val childPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            assertThat(childPreference.isEnabled).isFalse()

            mainSwitchPreference.performClick()
            // Pretend the PIN verification succeeded.
            fragment.onActivityResult(
                REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
                Activity.RESULT_OK,
                null,
            )

            assertThat(childPreference.isEnabled).isTrue()
        }
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_SUPERVISION_SETTINGS_SCREEN)
    fun toggleMainSwitch_pinVerificationFailed_childPreferencesRemainDisabled() {
        preferenceScreenCreator.launchFragmentScenario().onFragment { fragment ->
            val mainSwitchPreference =
                fragment.findPreference<MainSwitchPreference>(SupervisionMainSwitchPreference.KEY)!!
            val childPreference =
                fragment.findPreference<Preference>(SupervisionWebContentFiltersScreen.KEY)!!

            assertThat(childPreference.isEnabled).isFalse()

            mainSwitchPreference.performClick()
            // Pretend the PIN verification failed.
            fragment.onActivityResult(
                REQUEST_CODE_CONFIRM_SUPERVISION_CREDENTIALS,
                Activity.RESULT_CANCELED,
                null,
            )

            assertThat(childPreference.isEnabled).isFalse()
        }
    }

    @Test
    fun getTitle() {
        assertThat(preferenceScreenCreator.title).isEqualTo(R.string.supervision_settings_title)
    }

    @Test
    fun getKeywords() {
        assertThat(preferenceScreenCreator.keywords)
            .isEqualTo(R.string.keywords_supervision_settings)
    }

    @Test
    fun isIndexable() {
        assertThat(preferenceScreenCreator.indexable).isTrue()
    }

    @Test
    fun onCreate_registersListener() {
        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(any())
    }

    @Test
    fun onDestroy_unregistersListener() {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        preferenceScreenCreator.onDestroy(mockLifeCycleContext)
        verify(mockSupervisionManager).unregisterSupervisionListener(listenerCaptor.firstValue)
    }

    @Test
    fun listener_onSupervisionDisabled_refreshesPreferences() {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionDisabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
    }

    @Test
    fun listener_onSupervisionEnabled_refreshesPreferences() {
        val listenerCaptor = argumentCaptor<SupervisionManager.SupervisionListener>()

        preferenceScreenCreator.onCreate(mockLifeCycleContext)
        verify(mockSupervisionManager).registerSupervisionListener(listenerCaptor.capture())

        listenerCaptor.firstValue.onSupervisionEnabled(0 /* userId */)

        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionDashboardScreen.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionMainSwitchPreference.KEY)
        verify(mockLifeCycleContext).notifyPreferenceChange(SupervisionPinManagementScreen.KEY)
    }
}
