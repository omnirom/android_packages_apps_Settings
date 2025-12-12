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

package com.android.settings.accessibility.shared.ui

import android.app.Activity
import android.content.Context
import androidx.preference.Preference
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE
import com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY
import com.android.server.accessibility.Flags.enableLowVisionHats
import com.android.settings.R
import com.android.settings.accessibility.SurveyManager
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.ButtonPreference

/**
 * Base class for accessibility survey button preferences. It manages the button's visibility,
 * checks for survey availability, and handles notifications.
 */
abstract class BaseSurveyButtonPreference(val metricsCategory: Int = METRICS_CATEGORY_UNKNOWN) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    protected var surveyManager: SurveyManager? = null
    protected var isSurveyButtonVisible: Boolean = false

    protected abstract val surveyKey: String

    override val key: String
        get() = PREFERENCE_KEY

    override val title: Int
        get() = R.string.accessibility_send_survey_title

    override val icon: Int
        get() = R.drawable.ic_rate_review

    override val indexable
        get() = false

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        if (enableLowVisionHats()) {
            surveyManager =
                SurveyManager(
                    context.lifecycleOwner,
                    context.baseContext,
                    surveyKey,
                    metricsCategory,
                )
            (context.baseContext as? Activity)?.let { activity ->
                val intent = activity.intent
                if (intent?.getStringExtra(EXTRA_SOURCE) == SOURCE_START_SURVEY) {
                    surveyManager?.startSurvey()
                } else {
                    surveyManager?.checkSurveyAvailable { available ->
                        isSurveyButtonVisible = available
                        context.notifyPreferenceChange(key)
                        scheduleSurvey(context)
                    }
                }
            }
        }

        context.findPreference<ButtonPreference>(key)?.apply {
            order = Integer.MAX_VALUE
            setOnClickListener {
                surveyManager?.startSurvey()
                isSurveyButtonVisible = false
                context.notifyPreferenceChange(key)
                scheduleSurvey(context)
            }
        }
    }

    override fun isAvailable(context: Context): Boolean =
        enableLowVisionHats() &&
            !context.isInSetupWizard() &&
            isSurveyButtonVisible &&
            isSurveyConditionMet(context)

    abstract fun isSurveyConditionMet(context: Context): Boolean

    override fun createWidget(context: Context): Preference =
        ButtonPreference(context).apply {
            setButtonStyle(ButtonPreference.TYPE_TONAL, ButtonPreference.SIZE_NORMAL)
        }

    protected fun scheduleSurvey(context: Context) {
        if (isAvailable(context)) {
            surveyManager?.scheduleSurveyNotification()
        } else {
            surveyManager?.cancelSurveyNotification()
        }
    }

    companion object {
        const val PREFERENCE_KEY = "survey"
    }
}
