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
package com.android.settings.spa.app.appinfo

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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.dx.mockito.inline.extended.ExtendedMockito
import com.android.settings.R
import com.android.settingslib.spa.testutils.delay
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.MockitoSession
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness

@RunWith(AndroidJUnit4::class)
class ManageTargetAppFunctionAccessPreferenceTest {
    @get:Rule val composeTestRule = createComposeRule()
    @get:Rule val setFlagsRule = SetFlagsRule()

    @Mock private lateinit var appFunctionManager: AppFunctionManager
    @Mock private lateinit var packageManager: PackageManager
    @Mock private lateinit var userManager: UserManager

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            doNothing().whenever(mock).startActivityAsUser(any(), any())
        }

    private lateinit var mockSession: MockitoSession

    @Before
    fun setUp() {
        mockSession =
            ExtendedMockito.mockitoSession()
                .initMocks(this)
                .strictness(Strictness.LENIENT)
                .startMocking()
        whenever(context.getSystemService(Context.APP_FUNCTION_SERVICE))
            .thenReturn(appFunctionManager)
        doReturn(listOf(PACKAGE_NAME)).whenever(appFunctionManager).validTargets
        whenever(context.packageManager).thenReturn(packageManager)
        doReturn(false).whenever(packageManager).hasSystemFeature(any())
        whenever(context.getSystemService(Context.USER_SERVICE)).thenReturn(userManager)
        doReturn(false).whenever(userManager).isProfile
    }

    @After
    fun tearDown() {
        mockSession.finishMocking()
    }

    @Test
    @DisableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun content_display_flagDisabled() {
        setContent()

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_title)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_summary)
            )
            .assertDoesNotExist()
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun content_display_flagEnabled() {
        setContent()

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_title)
            )
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_summary)
            )
            .assertIsDisplayed()
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun content_display_invalidTarget_flagEnabled() {
        doReturn(emptyList<String>()).whenever(appFunctionManager).validTargets

        setContent()

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_title)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_summary)
            )
            .assertDoesNotExist()
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun content_display_isProfile_flagEnabled() {
        doReturn(true).whenever(userManager).isProfile

        setContent()

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_title)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_summary)
            )
            .assertDoesNotExist()
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun content_display_invalidFormFactor_flagEnabled() {
        for (formFactor in unsupportedFormFactors) {
            doReturn(true)
                .whenever(packageManager)
                .hasSystemFeature(ArgumentMatchers.eq(formFactor))
        }

        setContent()

        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_title)
            )
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(
                context.getString(R.string.manage_target_app_function_access_settings_summary)
            )
            .assertDoesNotExist()
    }

    @Test
    @EnableFlags(Flags.FLAG_APP_FUNCTION_ACCESS_UI_ENABLED)
    fun whenClick_startActivity() {
        setContent()
        composeTestRule.onRoot().performClick()
        composeTestRule.delay()

        val intent =
            argumentCaptor { verify(context).startActivityAsUser(capture(), any()) }.firstValue
        assertThat(intent.action)
            .isEqualTo(AppFunctionManager.ACTION_MANAGE_TARGET_APP_FUNCTION_ACCESS)
        assertThat(intent.getStringExtra(Intent.EXTRA_PACKAGE_NAME)).isEqualTo(PACKAGE_NAME)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalContext provides context) {
                ManageTargetAppFunctionAccessPreference(APP)
            }
        }
        composeTestRule.delay()
    }

    private companion object {
        const val PACKAGE_NAME = "package.name"

        val APP = ApplicationInfo().apply { packageName = PACKAGE_NAME }

        val unsupportedFormFactors: List<String> =
            listOf(
                PackageManager.FEATURE_AUTOMOTIVE,
                PackageManager.FEATURE_LEANBACK,
                PackageManager.FEATURE_WATCH,
            )
    }
}
