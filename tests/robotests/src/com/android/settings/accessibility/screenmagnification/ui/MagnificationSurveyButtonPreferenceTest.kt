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

package com.android.settings.accessibility.screenmagnification.ui

import android.app.Application
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.platform.test.flag.junit.SetFlagsRule
import androidx.core.util.Consumer
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.preference.PreferenceManager
import androidx.preference.PreferenceScreen
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_PAGE_ID
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE
import com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.Utils.SETTINGS_PACKAGE_NAME
import com.android.settings.accessibility.screenmagnification.ui.MagnificationPreferenceFragment.Companion.MAGNIFICATION_SURVEY_KEY
import com.android.settings.accessibility.shared.ui.BaseSurveyButtonPreference.Companion.PREFERENCE_KEY
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.android.settings.testutils.SettingsStoreRule
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
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
import org.mockito.Mockito.verify
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.robolectric.RobolectricTestParameterInjector
import org.robolectric.Shadows
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

@RunWith(RobolectricTestParameterInjector::class)
class MagnificationSurveyButtonPreferenceTest {
    @get:Rule val setFlagsRule = SetFlagsRule()
    @get:Rule val settingStoreRule = SettingsStoreRule()
    private lateinit var context: Context
    private lateinit var shadowPackageManager: ShadowPackageManager
    private lateinit var surveyFeatureProvider: SurveyFeatureProvider
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var preferenceScreen: PreferenceScreen
    private var activityScenario: ActivityScenario<EmptyFragmentActivity>? = null
    private val lifecycleOwner = TestLifecycleOwner()
    private var buttonPreference: ButtonPreference? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowPackageManager = shadowOf(context.packageManager)
        preferenceManager = PreferenceManager(context)
        preferenceScreen = preferenceManager.createPreferenceScreen(context)
        surveyFeatureProvider =
            FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(context)!!
    }

    @After
    fun tearDown() {
        activityScenario?.close()
    }

    @Test
    fun getKey() {
        val preference = createPreference()

        assertThat(preference.key).isEqualTo(PREFERENCE_KEY)
    }

    @Test
    fun getIcon() {
        val preference = createPreference()

        assertThat(preference.icon).isEqualTo(R.drawable.ic_rate_review)
    }

    @Test
    fun getTitle() {
        val preference = createPreference()

        assertThat(preference.title).isEqualTo(R.string.accessibility_send_survey_title)
    }

    @Test
    fun createWidget() {
        val preference = createPreference()

        val widget = preference.createWidget(context)

        assertThat(widget).isInstanceOf(ButtonPreference::class.java)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    @TestParameters(
        value =
            [
                "{hasShortcuts: true" +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: false" +
                    ", expectedValue: true" +
                    "}",
                "{hasShortcuts: false" +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: false" +
                    ", expectedValue: false" +
                    "}",
                "{hasShortcuts: true" +
                    ", surveyAvailability: false" +
                    ", inSetupWizard: false" +
                    ", expectedValue: false" +
                    "}",
                "{hasShortcuts: true" +
                    ", surveyAvailability: true" +
                    ", inSetupWizard: true" +
                    ", expectedValue: false" +
                    "}",
            ]
    )
    fun isAvailable_flagOn_returnExpectedValue(
        hasShortcuts: Boolean,
        surveyAvailability: Boolean,
        inSetupWizard: Boolean,
        expectedValue: Boolean,
    ) {
        val newContext = createContext(inSetupWizard = inSetupWizard)
        setupSurveyAvailability(surveyAvailability)

        val preference = createPreference(newContext, hasShortcuts)

        assertThat(preference.isAvailable(newContext)).isEqualTo(expectedValue)
    }

    @Test
    @DisableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun isAvailable_flagOff_returnFalse() {
        val newContext = createContext(inSetupWizard = false)
        setupSurveyAvailability(available = true)

        val preference = createPreference(newContext, hasShortcuts = true)

        assertThat(preference.isAvailable(newContext)).isFalse()
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun buttonClick_callsSurveyMethodsAndHidesButton() {
        val newContext = createContext(inSetupWizard = false)
        setupSurveyAvailability(available = true)
        createPreference(newContext, hasShortcuts = true)
        Shadows.shadowOf(context as Application?).clearBroadcastIntents()

        buttonPreference!!.button.performClick()

        assertThat(buttonPreference!!.isVisible).isFalse()
        verify(surveyFeatureProvider).sendActivityIfAvailable(MAGNIFICATION_SURVEY_KEY)
        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents.size).isEqualTo(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction()).isEqualTo(ACTION_CANCEL_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(EXTRA_PAGE_ID, SettingsEnums.PAGE_UNKNOWN))
            .isEqualTo(SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun onCreate_surveyIntentPresent_startsSurvey() {
        val newContext = createContext(isStartSurveyIntent = true)

        createPreference(newContext)

        verify(surveyFeatureProvider).sendActivityIfAvailable(MAGNIFICATION_SURVEY_KEY)
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_LOW_VISION_HATS)
    fun onCreate_noSurveyIntentPresent_checksSurveyAvailability() {
        val newContext = createContext(isStartSurveyIntent = false)

        createPreference(newContext)

        verify(surveyFeatureProvider, never()).sendActivityIfAvailable(MAGNIFICATION_SURVEY_KEY)
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

    private fun createPreference(
        context: Context = this.context,
        hasShortcuts: Boolean = false,
    ): MagnificationSurveyButtonPreference {
        val preference =
            MagnificationSurveyButtonPreference(
                SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION
            )
        buttonPreference =
            preference.createAndBindWidget<ButtonPreference>(context, preferenceScreen)
        preferenceScreen.addPreference(buttonPreference!!)
        val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
            on { findPreference<ButtonPreference>(PREFERENCE_KEY) }.thenReturn(buttonPreference)
            on { baseContext }.thenReturn(context)
            on { lifecycleOwner }.thenReturn(lifecycleOwner)
            on { notifyPreferenceChange(anyString()) }
                .thenAnswer {
                    preference.bind(buttonPreference!!, preference)
                    null
                }
        }
        preference.onCreate(preferenceLifecycleContext)
        preference.dataStore?.setBoolean(PREFERENCE_KEY, hasShortcuts)
        buttonPreference!!.inflateViewHolder()
        return preference
    }

    private fun createContext(
        inSetupWizard: Boolean = false,
        isStartSurveyIntent: Boolean = false,
    ): Context {
        shadowPackageManager.addActivityIfNotPresent(
            ComponentName(context, EmptyFragmentActivity::class.java)
        )
        var startedActivity: Context? = null
        val intent = Intent(context, EmptyFragmentActivity::class.java)
        if (inSetupWizard) {
            intent.putExtra(EXTRA_IS_SETUP_FLOW, inSetupWizard)
        }
        if (isStartSurveyIntent) {
            intent.putExtra(EXTRA_SOURCE, SOURCE_START_SURVEY)
        }
        activityScenario = ActivityScenario.launch(intent)
        activityScenario!!.onActivity { activity -> startedActivity = activity }
        return startedActivity!!
    }
}
