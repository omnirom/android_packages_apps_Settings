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

import android.content.Context
import androidx.annotation.VisibleForTesting
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.accessibility.shared.data.AccessibilityShortcutDataStore
import com.android.settings.accessibility.shared.ui.BaseSurveyButtonPreference
import com.android.settingslib.core.instrumentation.Instrumentable.METRICS_CATEGORY_UNKNOWN
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceLifecycleContext

class MagnificationSurveyButtonPreference(metricsCategory: Int = METRICS_CATEGORY_UNKNOWN) :
    BaseSurveyButtonPreference(metricsCategory) {

    private var settingsKeyedObserver: KeyedObserver<String>? = null
    @VisibleForTesting var dataStore: AccessibilityShortcutDataStore? = null

    override val surveyKey: String
        get() = SURVEY_KEY

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        dataStore =
            AccessibilityShortcutDataStore(
                context.baseContext,
                AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME,
            )
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        settingsKeyedObserver = KeyedObserver { _, _ ->
            context.notifyPreferenceChange(key)
            scheduleSurvey(context)
        }
        dataStore?.addObserver(key, settingsKeyedObserver!!, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        settingsKeyedObserver?.let {
            dataStore?.removeObserver(key, it)
            settingsKeyedObserver = null
        }
    }

    override fun isSurveyConditionMet(context: Context): Boolean =
        dataStore?.getBoolean(key) == true

    companion object {
        const val SURVEY_KEY = "A11yMagnificationUser"
    }
}
