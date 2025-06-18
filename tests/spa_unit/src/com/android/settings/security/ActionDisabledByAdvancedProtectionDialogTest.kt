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

package com.android.settings.security

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Bundle
import android.platform.test.annotations.RequiresFlagsEnabled
import android.platform.test.flag.junit.CheckFlagsRule
import android.platform.test.flag.junit.DeviceFlagsValueProvider
import android.security.Flags
import android.security.advancedprotection.AdvancedProtectionManager
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_CELLULAR_2G
import android.security.advancedprotection.AdvancedProtectionManager.FEATURE_ID_DISALLOW_WEP
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
import android.security.advancedprotection.AdvancedProtectionManager.SUPPORT_DIALOG_TYPE_DISABLED_SETTING
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ActivityScenario.launch
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RequiresFlagsEnabled(Flags.FLAG_AAPM_API)
@RunWith(AndroidJUnit4::class)
class ActionDisabledByAdvancedProtectionDialogTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ActionDisabledByAdvancedProtectionDialog>()

    @get:Rule
    val checkFlagsRule: CheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule()

    private val mockPackageManager = mock<PackageManager>()

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun blockedInteractionDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_action_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun wepBlockedInteraction_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_WEP,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_wep_action_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabled2gSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_on_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledMteSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            AdvancedProtectionManager.FEATURE_ID_ENABLE_MTE,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_on_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledWepSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_WEP,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_off_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun disabledInstallUnknownSourcesSettingDialog_showsCorrectTitleAndMessage() {
        val intent = AdvancedProtectionManager.createSupportIntent(
            AdvancedProtectionManager.FEATURE_ID_DISALLOW_INSTALL_UNKNOWN_SOURCES,
            SUPPORT_DIALOG_TYPE_DISABLED_SETTING
        )

        launchDialogActivity(intent) {
            composeTestRule
                .onNodeWithText(context.getString(R.string.disabled_by_advanced_protection_title))
                .assertIsDisplayed()
            composeTestRule
                .onNodeWithText(context.getString(
                    R.string.disabled_by_advanced_protection_setting_is_off_message))
                .assertIsDisplayed()
        }
    }

    @Test
    fun helpIntentDoesNotExist_getSupportButtonIfExists_returnsNull() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, /* uriToReturn */ null)

                val button = spyActivity.getSupportButtonIfExists()
                assertNull(button)
            }
        }
    }

    @Test
    fun helpIntentExistsAndDoesNotResolveToActivity_getSupportButtonIfExists_returnsNull() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, helpIntentUri)
                mockResolveActivity(spyActivity, /* resolveInfoToReturn */ null)

                val button = spyActivity.getSupportButtonIfExists()
                assertNull(button)
            }
        }
    }

    @Test
    fun helpIntentExistsAndResolvesToActivity_getSupportButtonIfExists_returnsButton() {
        launchDialogActivity(defaultIntent) { scenario ->
            scenario.onActivity { activity ->
                val spyActivity = spyOnActivityHelpIntentUri(activity, helpIntentUri)
                val resolveInfoToReturn = ResolveInfo().apply {
                    activityInfo = ActivityInfo().apply {
                        packageName = HELP_INTENT_PKG_NAME
                    }
                }
                mockResolveActivity(spyActivity, resolveInfoToReturn)

                // 1. Check the button is returned.
                val button = spyActivity.getSupportButtonIfExists()
                assertNotNull(button)

                // 2. Check the button has correct text.
                assertEquals(context.getString(
                    R.string.disabled_by_advanced_protection_help_button_title), button!!.text
                )

                // 3. Check the button's onClick launches the help activity and finishes the dialog.
                button.onClick()

                val intentCaptor = ArgumentCaptor.forClass(Intent::class.java)
                verify(spyActivity).startActivity(intentCaptor.capture())
                val launchedIntent = intentCaptor.value
                assertEquals(HELP_INTENT_ACTION, launchedIntent.action)
                assertEquals(HELP_INTENT_PKG_NAME, launchedIntent.`package`)

                assertTrue(spyActivity.isFinishing)
            }
        }
    }

    private fun spyOnActivityHelpIntentUri(
        activity: ActionDisabledByAdvancedProtectionDialog,
        uriToReturn: String?
    ): ActionDisabledByAdvancedProtectionDialog {
        val spyActivity = spy(activity)
        val spyResources = spy(spyActivity.resources)
        doReturn(spyResources).whenever(spyActivity).resources
        doReturn(uriToReturn).whenever(spyResources).getString(helpUriResourceId)
        return spyActivity
    }

    private fun mockResolveActivity(
        spyActivity: ActionDisabledByAdvancedProtectionDialog,
        resolveInfoToReturn: ResolveInfo?
    ) {
        doReturn(mockPackageManager).whenever(spyActivity).packageManager
        doReturn(resolveInfoToReturn).whenever(mockPackageManager).resolveActivity(any(), anyInt())
    }

    private fun launchDialogActivity(
        intent: Intent,
        onScenario: (ActivityScenario<ActionDisabledByAdvancedProtectionDialog>) -> Unit
    ) {
        intent.setComponent(
            ComponentName(
                context,
                ActionDisabledByAdvancedProtectionDialog::class.java
            )
        )
        launch<ActionDisabledByAdvancedProtectionDialog>(intent).use(onScenario)
    }

    class HelpTestActivity : Activity() {
        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            finish()
        }
    }

    private companion object {
        val defaultIntent = AdvancedProtectionManager.createSupportIntent(
            FEATURE_ID_DISALLOW_CELLULAR_2G,
            SUPPORT_DIALOG_TYPE_BLOCKED_INTERACTION
        )
        const val HELP_INTENT_PKG_NAME = "com.android.settings.tests.spa_unit"
        const val HELP_INTENT_ACTION = "$HELP_INTENT_PKG_NAME.HELP_ACTION"
        val helpIntent = Intent(HELP_INTENT_ACTION).setPackage(HELP_INTENT_PKG_NAME)
        val helpIntentUri = helpIntent.toUri(Intent.URI_INTENT_SCHEME)
        val helpUriResourceId =
            com.android.internal.R.string.config_help_url_action_disabled_by_advanced_protection
    }
}
