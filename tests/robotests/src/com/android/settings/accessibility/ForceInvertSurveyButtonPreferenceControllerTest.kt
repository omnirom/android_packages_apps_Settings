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
import android.app.UiModeManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.content.getSystemService
import androidx.core.util.Consumer
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID
import com.android.server.accessibility.Flags
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.core.BasePreferenceController.AVAILABLE
import com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.widget.ButtonPreference
import com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW
import com.google.common.truth.Truth.assertThat
import com.google.testing.junit.testparameterinjector.TestParameters
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.doAnswer
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestParameterInjector::class)
class ForceInvertSurveyButtonPreferenceControllerTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val shadowPackageManager = shadowOf(context.packageManager)
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val preferenceManager = PreferenceManager(context)
    private val preferenceScreen = preferenceManager.createPreferenceScreen(context)
    private lateinit var preference: ButtonPreference
    private lateinit var controller: ForceInvertSurveyButtonPreferenceController
    private val lifecycleOwner = TestLifecycleOwner()
    private val uiModeManager: UiModeManager = spy(context.getSystemService<UiModeManager>())!!
    private lateinit var surveyManager: SurveyManager
    private lateinit var surveyFeatureProvider: SurveyFeatureProvider

    @Before
    fun setUp() {
        surveyFeatureProvider =
            FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(context)!!
        surveyManager = SurveyManager(lifecycleOwner, context, TRIGGER_KEY, PAGE_ID)
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    @TestParameters(
        value =
            [
                "{forceInvertState: " +
                    UiModeManager.FORCE_INVERT_TYPE_DARK +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: false" +
                    ", expectedAvailabilityStatus: " +
                    AVAILABLE +
                    "}",
                "{forceInvertState: " +
                    UiModeManager.FORCE_INVERT_TYPE_LIGHT +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: false" +
                    ", expectedAvailabilityStatus: " +
                    CONDITIONALLY_UNAVAILABLE +
                    "}",
                "{forceInvertState: " +
                    UiModeManager.FORCE_INVERT_TYPE_OFF +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: false" +
                    ", expectedAvailabilityStatus: " +
                    CONDITIONALLY_UNAVAILABLE +
                    "}",
                "{forceInvertState: " +
                    UiModeManager.FORCE_INVERT_TYPE_DARK +
                    ", surveyAvailability: false" +
                    ", inSetupWizard: false" +
                    ", expectedAvailabilityStatus: " +
                    CONDITIONALLY_UNAVAILABLE +
                    "}",
                "{forceInvertState: " +
                    UiModeManager.FORCE_INVERT_TYPE_DARK +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: true" +
                    ", expectedAvailabilityStatus: " +
                    CONDITIONALLY_UNAVAILABLE +
                    "}",
            ]
    )
    fun getAvailabilityStatus_flagOn_returnExpectedAvailabilityStatus(
        forceInvertState: Int,
        surveyAvailability: Boolean,
        inSetupWizard: Boolean,
        expectedAvailabilityStatus: Int,
    ) {
        val newContext = createContext(inSetupWizard)
        setupForceInvertState(forceInvertState)
        setupSurveyAvailability(surveyAvailability)

        createController(newContext)

        assertThat(controller.availabilityStatus).isEqualTo(expectedAvailabilityStatus)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun getAvailabilityStatus_flagOff_returnUnavailable() {
        val newContext = createContext(inSetupWizard = false)
        setupForceInvertState(UiModeManager.FORCE_INVERT_TYPE_DARK)
        setupSurveyAvailability(available = true)

        createController(newContext)

        assertThat(controller.availabilityStatus).isEqualTo(CONDITIONALLY_UNAVAILABLE)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun performClick_shouldStartActivityAndHideButtonAndSendCancelBroadcast() {
        val newContext = createContext(inSetupWizard = false)
        setupForceInvertState(UiModeManager.FORCE_INVERT_TYPE_DARK)
        setupSurveyAvailability(available = true)
        createController(newContext)
        Shadows.shadowOf(context as Application?).clearBroadcastIntents()

        preference.button.performClick()

        verify(surveyFeatureProvider).sendActivityIfAvailable(TRIGGER_KEY)
        assertThat(preference.isVisible).isFalse()
        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents.size).isEqualTo(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction()).isEqualTo(ACTION_CANCEL_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN)).isEqualTo(PAGE_ID)
    }

    private fun setupSurveyAvailability(available: Boolean) {
        doAnswer { invocation ->
                val consumer: Consumer<Boolean?> = invocation.getArgument(2)
                consumer.accept(available)
                null
            }
            .`when`(surveyFeatureProvider)
            .checkSurveyAvailable(any(), anyString(), any())
    }

    private fun setupForceInvertState(forceInvertType: Int) {
        doAnswer { invocation -> forceInvertType }.`when`(uiModeManager).getForceInvertState()
    }

    private fun createController(context: Context) {
        controller = ForceInvertSurveyButtonPreferenceController(context, KEY)
        controller.initialize(surveyManager, uiModeManager)
        controller.onCreate(lifecycleOwner)
        preference = ButtonPreference(context)
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
        private const val TRIGGER_KEY = "trigger key"
        private const val PAGE_ID = 123
    }
}
