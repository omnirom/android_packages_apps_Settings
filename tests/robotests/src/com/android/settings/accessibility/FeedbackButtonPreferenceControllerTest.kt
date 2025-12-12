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

package com.android.settings.accessibility

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.server.accessibility.Flags
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.ButtonPreference
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class FeedbackButtonPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ButtonPreference
    private lateinit var controller: FeedbackButtonPreferenceController

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun getAvailabilityStatus_whenInSetupWizard_returnUnavailable() {
        setUp(/* inSetupWizard= */ true, PACKAGE_NAME)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun getAvailabilityStatus_whenNotInSetupWizardAndNoValidProvider_returnUnavailable() {
        setUp(/* inSetupWizard= */ false)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun getAvailabilityStatus_whenNotInSetupWizardAndHasValidProvider_returnAvailable() {
        setUp(/* inSetupWizard= */ false, PACKAGE_NAME)

        assertThat(controller.availabilityStatus).isEqualTo(AVAILABLE)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun getAvailabilityStatus_disableLowVisionGeneric_returnUnavailable() {
        setUp(/* inSetupWizard= */ false, PACKAGE_NAME)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_GENERIC_FEEDBACK)
    fun performClick_shouldStartBugReportIntent() {
        setUp(/* inSetupWizard= */ false, PACKAGE_NAME)

        preference.button.performClick()

        val startedIntent = Shadows.shadowOf(context as Application?).nextStartedActivity
        assertThat(startedIntent.getAction()).isEqualTo(Intent.ACTION_BUG_REPORT)
        assertThat(startedIntent.getPackage()).isEqualTo(PACKAGE_NAME)
        val extras = startedIntent.getExtras()!!
        assertThat(extras.getString(FeedbackManager.CATEGORY_TAG_EXTRA)).isEqualTo(CATEGORY_TAG)
        assertThat(extras.getString(FeedbackManager.TRIGGER_ID_EXTRA)).isEqualTo(TRIGGER_ID)
    }

    private fun setUp(inSetupWizard: Boolean = false, reporterPackage: String = "") {
        var newContext: Context = createContext(inSetupWizard)
        controller = FeedbackButtonPreferenceController(newContext, KEY)
        controller.initialize(FeedbackManager(reporterPackage, CATEGORY_TAG, TRIGGER_ID))
        preference = ButtonPreference(newContext)
        preference.setKey(KEY)
        preferenceScreen.addPreference(preference)
        controller.displayPreference(preferenceScreen)
        preference.onBindViewHolder(preference.inflateViewHolder())
    }

    private fun createContext(inSetupWizard: Boolean = false): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, EmptyFragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        if (inSetupWizard) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        }
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }

    companion object {
        private const val KEY = "test_key"
        private const val PACKAGE_NAME = "android"
        private const val CATEGORY_TAG = "category tag"
        private const val TRIGGER_ID = "trigger id"
    }
}
