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

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.ButtonPreference

/** Represents a preference item for sending feedback within a preference screen. */
open class FeedbackButtonPreference(feedbackManagerProvider: () -> FeedbackManager) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_send_feedback_title

    override val icon: Int
        get() = R.drawable.ic_feedback

    protected val feedbackManager: FeedbackManager by lazy { feedbackManagerProvider.invoke() }

    override fun isAvailable(context: Context): Boolean =
        !context.isInSetupWizard() && feedbackManager.isAvailable()

    override fun createWidget(context: Context): Preference =
        ButtonPreference(context).apply {
            setButtonStyle(ButtonPreference.TYPE_TONAL, ButtonPreference.SIZE_NORMAL)
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        context.findPreference<ButtonPreference>(key)?.apply {
            order = Integer.MAX_VALUE
            setOnClickListener { view ->
                view.context.startActivityForResult(
                    key,
                    feedbackManager.getFeedbackIntent(),
                    /* requestCode= */ 0,
                    /* options= */ null,
                )
            }
        }
    }

    companion object {
        const val KEY = "feedback"
    }
}
