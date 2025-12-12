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
package com.android.settings.applications.appinfo

import android.app.appfunctions.AppFunctionManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.UserManager
import android.permission.flags.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.preference.Preference
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.appfunctions.AppFunctionManagerWrapper
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settingslib.applications.ApplicationsState
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class ManageAgentAppFunctionAccessPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var appFunctionManagerWrapper: AppFunctionManagerWrapper
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var userManager: UserManager

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()

    private var controller: ManageAgentAppFunctionAccessPreferenceController? = null
    private var mockitoSession: MockitoSession? = null

    @Before
    fun setUp() {
        mockitoSession =
            Mockito.mockitoSession().initMocks(this).strictness(Strictness.LENIENT).startMocking()
        appFunctionManagerWrapper.stub { onBlocking { isValidAgent(PACKAGE_NAME) }.doReturn(true) }
        doReturn(packageManager).whenever(context).packageManager
        for (formFactor in unsupportedFormFactors) {
            doReturn(false)
                .whenever(packageManager)
                .hasSystemFeature(ArgumentMatchers.eq(formFactor))
        }
        whenever(context.getSystemService(Context.USER_SERVICE)).thenReturn(userManager)
        doReturn(false).whenever(userManager).isProfile

        controller = ManageAgentAppFunctionAccessPreferenceController(context, PREFERENCE_KEY)

        // Controller gets app info packagename from the AppEntry
        val appEntry: ApplicationsState.AppEntry = mock(ApplicationsState.AppEntry::class.java)
        appEntry.info = ApplicationInfo().apply { packageName = PACKAGE_NAME }
        controller!!.mAppEntry = appEntry
    }

    @After
    fun tearDown() {
        mockitoSession?.finishMocking()
    }

    @Test
    @DisableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsDisabled_thenPreferenceUnavailable() = runTest {
        controller!!.updateAvailability(appFunctionManagerWrapper)

        assertThat(controller!!.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_isInvalidAgent_thenPreferenceUnavailable() = runTest {
        appFunctionManagerWrapper.stub { onBlocking { isValidAgent(PACKAGE_NAME) }.doReturn(false) }

        controller!!.updateAvailability(appFunctionManagerWrapper)

        assertThat(controller!!.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_isProfile_thenPreferenceUnavailable() = runTest {
        doReturn(true).whenever(userManager).isProfile

        controller!!.updateAvailability(appFunctionManagerWrapper)

        assertThat(controller!!.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_andSupportedFormFactor_thenPreferenceAvailable() = runTest {
        controller!!.updateAvailability(appFunctionManagerWrapper)

        assertThat(controller!!.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_andUnsupportedFormFactor_thenPreferenceUnavailable() = runTest {
        for (formFactor in unsupportedFormFactors) {
            doReturn(true)
                .whenever(packageManager)
                .hasSystemFeature(ArgumentMatchers.eq(formFactor))
        }

        controller!!.updateAvailability(appFunctionManagerWrapper)

        assertThat(controller!!.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun handlePreferenceTreeClick() = runTest {
        controller!!.updateAvailability(appFunctionManagerWrapper)

        val preference = mock(Preference::class.java)
        whenever(preference.getKey()).thenReturn(PREFERENCE_KEY)

        controller!!.handlePreferenceTreeClick(preference)

        val intentCaptor: ArgumentCaptor<Intent> = ArgumentCaptor.forClass(Intent::class.java)
        verify(context).startActivityAsUser(intentCaptor.capture(), any())
        val intent: Intent = intentCaptor.value
        assertThat(intent.action)
            .isEqualTo(AppFunctionManager.ACTION_MANAGE_AGENT_APP_FUNCTION_ACCESS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
    }

    companion object {
        private const val PREFERENCE_KEY: String = "PREFERENCE_KEY"
        private const val PACKAGE_NAME = "package.name"
        private val unsupportedFormFactors: List<String> =
            listOf(
                PackageManager.FEATURE_AUTOMOTIVE,
                PackageManager.FEATURE_LEANBACK,
                PackageManager.FEATURE_WATCH,
            )
    }
}
