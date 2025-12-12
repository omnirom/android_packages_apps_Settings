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
import androidx.core.content.getSystemService
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.PreferenceScreen
import com.android.server.accessibility.Flags.enableLowVisionHats
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.ButtonPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor

/** Controller for managing the display and behavior of a force invert survey button preference. */
class ForceInvertSurveyButtonPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey), DefaultLifecycleObserver {

    private var surveyManager: SurveyManager? = null
    private var uiModeManager: UiModeManager? = null
    private var forceInvertStateChangeListener: ForceInvertStateChangeListener? = null
    private var preferenceScreen: PreferenceScreen? = null

    @Volatile private var currentForceInvertState: Int = UiModeManager.FORCE_INVERT_TYPE_OFF
    @Volatile private var isSurveyButtonVisible: Boolean = false

    /**
     * Initializes the [ForceInvertSurveyButtonPreferenceController] with its required dependencies.
     * This method sets up the controller to manage the survey button's lifecycle and visibility.
     *
     * @param surveyManager The [SurveyManager] responsible for checking survey availability.
     * @param uiModeManager The [UiModeManager] instance for detecting UI modes.
     */
    @JvmOverloads
    fun initialize(
        surveyManager: SurveyManager,
        uiModeManager: UiModeManager? = mContext.getSystemService(),
    ) {
        this.surveyManager = surveyManager
        this.uiModeManager = uiModeManager
        if (enableLowVisionHats()) {
            surveyManager.checkSurveyAvailable { available ->
                isSurveyButtonVisible = available
                updateSurveyButtonVisibility()
            }
        }
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        if (forceInvertColor()) {
            setupForceInvertStateListener()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        forceInvertStateChangeListener?.let {
            uiModeManager?.removeForceInvertStateChangeListener(it)
        }
    }

    override fun getAvailabilityStatus(): Int {
        val available =
            enableLowVisionHats() &&
                !mContext.isInSetupWizard() &&
                isSurveyButtonVisible &&
                currentForceInvertState == UiModeManager.FORCE_INVERT_TYPE_DARK
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
                // Mark the button as invisible immediately after click
                isSurveyButtonVisible = false
                updateSurveyButtonVisibility()
            }
        }
    }

    private fun setupForceInvertStateListener() {
        if (forceInvertStateChangeListener != null) {
            return
        }

        forceInvertStateChangeListener = ForceInvertStateChangeListener { newState ->
            currentForceInvertState = newState
            updateSurveyButtonVisibility()
        }

        uiModeManager?.let { uiManager ->
            currentForceInvertState = uiManager.getForceInvertState()
            uiManager.addForceInvertStateChangeListener(
                Dispatchers.Default.asExecutor(),
                forceInvertStateChangeListener!!,
            )
        }
    }

    private fun updateSurveyButtonVisibility() {
        val shouldBeVisible = getAvailabilityStatus() == AVAILABLE
        preferenceScreen?.findPreference<ButtonPreference>(mPreferenceKey)?.isVisible =
            shouldBeVisible
        scheduleForceInvertSurvey(shouldBeVisible)
    }

    private fun scheduleForceInvertSurvey(isForceInvertEnabled: Boolean) {
        if (isForceInvertEnabled) {
            surveyManager?.scheduleSurveyNotification()
        } else {
            surveyManager?.cancelSurveyNotification()
        }
    }
}
