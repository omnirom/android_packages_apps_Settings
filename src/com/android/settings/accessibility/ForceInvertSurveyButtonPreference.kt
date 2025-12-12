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

import android.app.UiModeManager
import android.app.UiModeManager.ForceInvertStateChangeListener
import android.content.Context
import android.view.accessibility.Flags.forceInvertColor
import androidx.annotation.VisibleForTesting
import androidx.core.content.getSystemService
import com.android.settings.accessibility.shared.ui.BaseSurveyButtonPreference
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.metadata.PreferenceLifecycleContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

class ForceInvertSurveyButtonPreference(metricsCategory: Int = METRICS_CATEGORY_UNKNOWN) :
    BaseSurveyButtonPreference(metricsCategory) {

    private var forceInvertStateChangeListener: ForceInvertStateChangeListener? = null
    @VisibleForTesting var uiModeManager: UiModeManager? = null

    override val surveyKey: String
        get() = SURVEY_KEY

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        uiModeManager = context.getSystemService()
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        if (forceInvertColor()) {
            forceInvertStateChangeListener = ForceInvertStateChangeListener { newState ->
                context.notifyPreferenceChange(key)
                scheduleSurvey(context)
            }
            uiModeManager?.let { uiManager ->
                uiManager.addForceInvertStateChangeListener(
                    Dispatchers.Default.asExecutor(),
                    forceInvertStateChangeListener!!,
                )
            }
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        forceInvertStateChangeListener?.let {
            uiModeManager?.removeForceInvertStateChangeListener(it)
        }
    }

    override fun isSurveyConditionMet(context: Context): Boolean =
        uiModeManager?.getForceInvertState() == UiModeManager.FORCE_INVERT_TYPE_DARK

    companion object {
        const val SURVEY_KEY = "A11yForceInvertUser"
    }
}
