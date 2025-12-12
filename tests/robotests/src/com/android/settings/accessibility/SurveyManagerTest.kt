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
import android.content.Context
import android.content.Intent
import androidx.core.util.Consumer
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.android.internal.accessibility.common.NotificationConstants
import com.android.settings.Utils
import com.android.settings.overlay.SurveyFeatureProvider
import com.android.settings.testutils.FakeFeatureFactory
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.mockito.kotlin.doAnswer
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

private const val TEST_SURVEY_TRIGGER_KEY = "surveyTriggerKey"
private const val TEST_PAGE_ID = 10

/** Tests for [SurveyManager]. */
@RunWith(RobolectricTestRunner::class)
class SurveyManagerTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private val context: Context = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var surveyManager: SurveyManager
    private lateinit var surveyFeatureProvider: SurveyFeatureProvider

    @Before
    fun setUp() {
        surveyFeatureProvider =
            FakeFeatureFactory.setupForTest().getSurveyFeatureProvider(context)!!
        surveyManager =
            SurveyManager(TestLifecycleOwner(), context, TEST_SURVEY_TRIGGER_KEY, TEST_PAGE_ID)
    }

    @Test
    fun scheduleSurveyNotification_sendCancelBroadcast() {
        surveyManager.scheduleSurveyNotification()

        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents).hasSize(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction())
            .isEqualTo(NotificationConstants.ACTION_SCHEDULE_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(Utils.SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(NotificationConstants.EXTRA_PAGE_ID, /* def= */ -1))
            .isEqualTo(TEST_PAGE_ID)
    }

    @Test
    fun cancelSurveyNotification_sendCancelBroadcast() {
        surveyManager.cancelSurveyNotification()

        val intents = Shadows.shadowOf(context as Application?).getBroadcastIntents()
        assertThat(intents).hasSize(1)
        val intent: Intent = intents.get(0)
        assertThat(intent.getAction())
            .isEqualTo(NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION)
        assertThat(intent.getPackage()).isEqualTo(Utils.SETTINGS_PACKAGE_NAME)
        assertThat(intent.getIntExtra(NotificationConstants.EXTRA_PAGE_ID, /* def= */ -1))
            .isEqualTo(TEST_PAGE_ID)
    }

    @Test
    fun startSurvey_providerAvailable_sendsActivity() {
        surveyManager.startSurvey()

        verify(surveyFeatureProvider).sendActivityIfAvailable(TEST_SURVEY_TRIGGER_KEY)
    }

    @Test
    fun checkSurveyAvailable_surveyAvailable_callbackInvokedWithTrue() {
        setupSurveyAvailability(/* available= */ true)
        var result: Boolean? = null
        val callback = Consumer<Boolean> { available -> result = available }

        surveyManager.checkSurveyAvailable(callback)

        assertThat(result).isTrue()
        verify(surveyFeatureProvider).checkSurveyAvailable(any(), anyString(), any())
    }

    @Test
    fun checkSurveyAvailable_surveyUnavailable_callbackInvokedWithFalse() {
        setupSurveyAvailability(/* available= */ false)
        var result: Boolean? = null
        val callback = Consumer<Boolean> { available -> result = available }

        surveyManager.checkSurveyAvailable(callback)

        assertThat(result).isFalse()
        verify(surveyFeatureProvider).checkSurveyAvailable(any(), anyString(), any())
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
}
