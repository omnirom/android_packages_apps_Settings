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

package com.android.settings.accessibility.textreading.dialogs

import android.app.Dialog
import android.app.settings.SettingsEnums
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.accessibility.textreading.data.BoldTextDataStore
import com.android.settings.accessibility.textreading.data.DisplaySizeDataStore
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settings.accessibility.textreading.data.OutlineTextDataStore
import com.android.settings.accessibility.textreading.ui.BoldTextPreference
import com.android.settings.accessibility.textreading.ui.OutlineTextPreference
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

/**
 * Dialog to confirm resetting text and reading options.
 *
 * This dialog is shown when the user clicks the reset button in the Text and reading options page.
 * It asks the user to confirm whether they want to reset the settings related to text and reading
 * options, including display size, font size, bold text, and outline text.
 */
class TextReadingResetDialog : InstrumentedDialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        return AlertDialog.Builder(context)
            .setTitle(R.string.accessibility_text_reading_confirm_dialog_title)
            .setMessage(R.string.accessibility_text_reading_confirm_dialog_message)
            .setCancelable(true)
            .setPositiveButton(R.string.accessibility_text_reading_confirm_dialog_reset_button) {
                _,
                _ ->
                resetTextReadingRelatedSettings()
            }
            .setNegativeButton(R.string.cancel, /* listener= */ null)
            .create()
    }

    private fun resetTextReadingRelatedSettings() {
        DisplaySizeDataStore(requireContext()).resetToDefault()
        FontSizeDataStore(requireContext()).resetToDefault()
        BoldTextDataStore(requireContext()).setBoolean(BoldTextPreference.KEY, false)
        OutlineTextDataStore(requireContext()).setBoolean(OutlineTextPreference.KEY, false)

        Toast.makeText(
                requireContext(),
                R.string.accessibility_text_reading_reset_message,
                Toast.LENGTH_SHORT,
            )
            .show()
        dismissAllowingStateLoss()
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_RESET_SETTINGS
    }

    companion object {

        @JvmStatic
        fun showDialog(fragmentManager: FragmentManager) {
            TextReadingResetDialog().apply {
                show(fragmentManager, /* tag= */ TextReadingResetDialog::class.simpleName)
            }
        }
    }
}
