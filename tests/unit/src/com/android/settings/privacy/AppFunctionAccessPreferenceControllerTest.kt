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
package com.android.settings.privacy

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import android.permission.flags.Flags
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.core.BasePreferenceController
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.MockitoSession
import org.mockito.Spy
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class AppFunctionAccessPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock lateinit var packageManager: PackageManager
    @Mock lateinit var userManager: UserManager

    @Spy private val context: Context = ApplicationProvider.getApplicationContext()

    private var controller: AppFunctionAccessPreferenceController? = null
    private var mockitoSession: MockitoSession? = null

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        mockitoSession =
            Mockito.mockitoSession().initMocks(this).strictness(Strictness.WARN).startMocking()
        Mockito.doReturn(packageManager).`when`<Context>(context).packageManager
        for (formFactor in unsupportedFormFactors) {
            Mockito.doReturn(false)
                .`when`(packageManager)
                .hasSystemFeature(ArgumentMatchers.eq(formFactor))
        }
        `when`(context.getSystemService(Context.USER_SERVICE)).thenReturn(userManager)
        Mockito.doReturn(false).`when`(userManager).isProfile

        controller = AppFunctionAccessPreferenceController(context, PREFERENCE_KEY)
    }

    @After
    fun tearDown() {
        mockitoSession?.finishMocking()
    }

    @Test
    @DisableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsDisabled_thenPreferenceUnavailable() {
        Truth.assertThat(controller!!.availabilityStatus)
            .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_isProfile_thenPreferenceUnavailable() {
        Mockito.doReturn(true).`when`(userManager).isProfile
        Truth.assertThat(controller!!.availabilityStatus)
            .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_andSupportedFormFactor_thenPreferenceAvailable() {
        Truth.assertThat(controller!!.availabilityStatus)
            .isEqualTo(BasePreferenceController.AVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenAppFunctionsEnabled_andUnsupportedFormFactor_thenPreferenceUnavailable() {
        for (formFactor in unsupportedFormFactors) {
            Mockito.doReturn(true)
                .`when`(packageManager)
                .hasSystemFeature(ArgumentMatchers.eq(formFactor))
            Truth.assertThat(controller!!.availabilityStatus)
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE)
        }
    }

    companion object {
        private const val PREFERENCE_KEY: String = "PREFERENCE_KEY"
        private val unsupportedFormFactors: List<String> =
            listOf(
                PackageManager.FEATURE_AUTOMOTIVE,
                PackageManager.FEATURE_LEANBACK,
                PackageManager.FEATURE_WATCH,
            )
    }
}
