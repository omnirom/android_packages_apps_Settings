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

package com.android.settings.accessibility.screenmagnification.dialogs

import android.app.Dialog
import android.app.settings.SettingsEnums
import android.content.DialogInterface
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityDialogUtils
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser.Companion.ARG_REQUEST_KEY
import com.android.settings.accessibility.screenmagnification.dialogs.MagnificationModeChooser.Companion.RESULT
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import com.android.settings.utils.AnnotationSpan

/** Display performance warning when using triple tap with the selected magnification mode. */
class TripleTapWarningDialog : InstrumentedDialogFragment() {
    private lateinit var requestKey: String
    @MagnificationMode private var selectedMode = MagnificationMode.FULLSCREEN

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestKey = requireArguments().getString(ARG_REQUEST_KEY, "")
        selectedMode = requireArguments().getInt(ARG_SELECTED_MODE)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val contentView: View =
            LayoutInflater.from(requireContext())
                .inflate(R.layout.magnification_triple_tap_warning_dialog, /* root= */ null)
                .also { updateLink(it) }

        val title = getText(R.string.accessibility_magnification_triple_tap_warning_title)
        val positiveBtnText =
            getText(R.string.accessibility_magnification_triple_tap_warning_positive_button)
        val negativeBtnText =
            getText(R.string.accessibility_magnification_triple_tap_warning_negative_button)

        val dialog =
            AccessibilityDialogUtils.createCustomDialog(
                requireContext(),
                title,
                contentView,
                positiveBtnText,
                DialogInterface.OnClickListener { _, _ -> confirmSelection() },
                negativeBtnText,
                DialogInterface.OnClickListener { _, _ ->
                    MagnificationModeChooser.showDialog(parentFragmentManager, requestKey)
                },
            )

        return dialog
    }

    private fun updateLink(view: View) {
        val messageView = view.requireViewById<TextView>(R.id.message)

        val linkListener =
            View.OnClickListener { _ ->
                confirmSelection()
                showEditShortcutsScreen()
                dismiss()
            }
        val linkInfo =
            AnnotationSpan.LinkInfo(AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener)
        val textWithLink =
            AnnotationSpan.linkify(
                getText(R.string.accessibility_magnification_triple_tap_warning_message),
                linkInfo,
            )

        messageView.apply {
            text = textWithLink
            movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun confirmSelection() {
        MagnificationCapabilities.setCapabilities(requireContext(), selectedMode)
        setFragmentResult(requestKey, Bundle().apply { putInt(RESULT, selectedMode) })
    }

    private fun showEditShortcutsScreen() {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
            requireContext(),
            metricsCategory,
            getText(R.string.accessibility_screen_magnification_shortcut_title),
            AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME,
            activity?.intent,
        )
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING
    }

    companion object {
        private const val ARG_SELECTED_MODE = "selectedMode"

        @JvmStatic
        fun showDialog(
            fragmentManager: FragmentManager,
            requestKey: String,
            @MagnificationMode selectedMagnificationMode: Int,
        ) {
            val bundle =
                Bundle().apply {
                    putString(ARG_REQUEST_KEY, requestKey)
                    putInt(ARG_SELECTED_MODE, selectedMagnificationMode)
                }

            TripleTapWarningDialog().apply {
                arguments = bundle
                show(fragmentManager, /* tag= */ MagnificationModeChooser::class.simpleName)
            }
        }
    }
}
