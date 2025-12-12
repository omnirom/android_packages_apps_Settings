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
import androidx.preference.PreferenceScreen
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.widget.ButtonPreference

/** Controller for managing the display and behavior of a feedback button preference. */
open class FeedbackButtonPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {

    private var feedbackManager: FeedbackManager? = null

    /**
     * Initializes the [FeedbackButtonPreferenceController] with a [FeedbackManager] instance. This
     * method must be called to provide the controller with the means to handle feedback.
     *
     * @param feedbackManager The [FeedbackManager] instance to be used by this controller.
     */
    fun initialize(feedbackManager: FeedbackManager) {
        this.feedbackManager = feedbackManager
    }

    fun getFeedbackManager(): FeedbackManager? {
        return feedbackManager
    }

    /**
     * Determines the availability status of the feedback button preference.
     *
     * The preference is [AVAILABLE] only if the current context is not within the setup wizard and
     * the [feedbackManager] is initialized and reports that feedback is available. Otherwise, it is
     * [CONDITIONALLY_UNAVAILABLE].
     *
     * @return An integer representing the availability status, either [AVAILABLE] or
     *   [CONDITIONALLY_UNAVAILABLE].
     */
    override fun getAvailabilityStatus(): Int =
        if (!mContext.isInSetupWizard() && feedbackManager?.isAvailable == true) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        val buttonPreference: ButtonPreference? = screen.findPreference(mPreferenceKey)
        buttonPreference?.apply {
            order = Integer.MAX_VALUE
            setOnClickListener { view ->
                view.context.startActivityForResult(
                    mPreferenceKey,
                    feedbackManager?.getFeedbackIntent(),
                    /* requestCode= */ 0,
                    /* options= */ null,
                )
            }
        }
    }
}
