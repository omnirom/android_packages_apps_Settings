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

import android.content.Context
import android.content.Intent
import androidx.core.util.Consumer
import androidx.lifecycle.LifecycleOwner
import com.android.internal.accessibility.common.NotificationConstants
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.overlay.SurveyFeatureProvider

/**
 * Manages the survey flow. This class is responsible for checking survey availability and sending
 * survey.
 */
class SurveyManager
@JvmOverloads
constructor(
    private val lifecycleOwner: LifecycleOwner,
    private val context: Context,
    private val surveyKey: String,
    private val pageId: Int,
) {
    private val surveyFeatureProvider: SurveyFeatureProvider? =
        featureFactory.getSurveyFeatureProvider(context)

    /**
     * Triggers the survey sending process using the provided SurveyFeatureProvider.
     *
     * If a SurveyFeatureProvider is available, it initiates the survey activity using the feedback
     * key. If the provider is null, this method does nothing.
     */
    fun startSurvey() = surveyFeatureProvider?.sendActivityIfAvailable(surveyKey)

    /**
     * Checks if a survey is available for the given host and feedback key.
     *
     * @param callback A Consumer that will be called with the survey availability status (true if
     *   available, false otherwise).
     */
    fun checkSurveyAvailable(callback: Consumer<Boolean>) =
        surveyFeatureProvider?.checkSurveyAvailable(lifecycleOwner, surveyKey, callback)

    /** Schedule a broadcast to trigger the scheduling of any eligible survey notifications. */
    fun scheduleSurveyNotification() =
        sendSurveyBroadcast(NotificationConstants.ACTION_SCHEDULE_SURVEY_NOTIFICATION)

    /** Cancels any existing or pending survey notifications. */
    fun cancelSurveyNotification() =
        sendSurveyBroadcast(NotificationConstants.ACTION_CANCEL_SURVEY_NOTIFICATION)

    /**
     * Sends a survey broadcast with the specified action.
     *
     * @param action The action string for the broadcast intent.
     */
    private fun sendSurveyBroadcast(action: String?) {
        val intent =
            Intent(action)
                .setPackage(context.getPackageName())
                .putExtra(NotificationConstants.EXTRA_PAGE_ID, pageId)
        context.sendBroadcast(intent)
    }
}
