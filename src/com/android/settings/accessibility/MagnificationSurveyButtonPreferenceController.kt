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

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType
import com.android.server.accessibility.Flags.enableLowVisionHats
import com.android.settings.accessibility.AccessibilitySettingsContentObserver.ContentObserverCallback
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.ButtonPreference

/** Controller for managing the display and behavior of a magnification survey button preference. */
class MagnificationSurveyButtonPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey), DefaultLifecycleObserver {

    private var surveyManager: SurveyManager? = null
    private val settingsContentObserver by lazy {
        AccessibilitySettingsContentObserver(Looper.myLooper()?.run { Handler(/* async= */ true) })
    }
    private var preferenceScreen: PreferenceScreen? = null
    private var componentName: ComponentName =
        AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
    @Volatile private var isSurveyButtonVisible: Boolean = false

    /**
     * Initializes the [MagnificationSurveyButtonPreferenceController] with a [SurveyManager]
     * instance. This method must be called to provide the controller with the means to handle
     * survey.
     *
     * @param surveyManager The [SurveyManager] instance to be used by this controller.
     */
    fun initialize(surveyManager: SurveyManager) {
        this.surveyManager = surveyManager
        if (enableLowVisionHats()) {
            surveyManager.checkSurveyAvailable { available ->
                isSurveyButtonVisible = available
                updateSurveyButtonVisibility()
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        settingsContentObserver.registerKeysToObserverCallback(
            ShortcutConstants.MAGNIFICATION_SHORTCUT_SETTINGS.toList(),
            ContentObserverCallback { key: String? -> updateSurveyButtonVisibility() },
        )
        settingsContentObserver.register(mContext.contentResolver)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        settingsContentObserver.unregister(mContext.contentResolver)
    }

    override fun getAvailabilityStatus(): Int {
        val available =
            enableLowVisionHats() &&
                !mContext.isInSetupWizard() &&
                isSurveyButtonVisible &&
                AccessibilityUtil.getUserShortcutTypesFromSettings(mContext, componentName) !=
                    UserShortcutType.DEFAULT
        return if (available) AVAILABLE else CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        preferenceScreen = screen
        val buttonPreference: ButtonPreference? = screen.findPreference(mPreferenceKey)
        buttonPreference?.apply {
            order = Integer.MAX_VALUE
            setOnClickListener { view ->
                surveyManager?.startSurvey()
                isSurveyButtonVisible = false
                updateSurveyButtonVisibility()
            }
        }
    }

    private fun updateSurveyButtonVisibility() {
        val shouldBeVisible = getAvailabilityStatus() == AVAILABLE
        preferenceScreen?.findPreference<ButtonPreference>(mPreferenceKey)?.isVisible =
            shouldBeVisible
        scheduleSurvey(shouldBeVisible)
    }

    private fun scheduleSurvey(shouldBeVisible: Boolean) {
        if (shouldBeVisible) {
            surveyManager?.scheduleSurveyNotification()
        } else {
            surveyManager?.cancelSurveyNotification()
        }
    }
}
