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

package com.android.settings.accessibility.textreading.ui

import android.content.Context
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.textreading.dialogs.TextReadingResetDialog
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.ButtonPreference

internal class ResetPreference(@EntryPoint private val entryPoint: Int) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_text_reading_reset_button_title

    override val icon: Int
        get() = R.drawable.ic_history

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard()
    }

    override fun createWidget(context: Context): Preference {
        return ButtonPreference(context).apply {
            setButtonStyle(ButtonPreference.TYPE_TONAL, ButtonPreference.SIZE_NORMAL)
        }
    }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        context.findPreference<ButtonPreference>(key)?.apply {
            setOnClickListener { view ->
                SettingsStatsLog.write(
                    SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
                    SettingsStatsLog
                        .ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_RESET,
                    /* reset */ -1,
                    AccessibilityStatsLogUtils.convertToEntryPoint(entryPoint),
                )
                TextReadingResetDialog.showDialog(context.childFragmentManager)
            }
        }
    }

    companion object {
        const val KEY = "reset"
    }
}
