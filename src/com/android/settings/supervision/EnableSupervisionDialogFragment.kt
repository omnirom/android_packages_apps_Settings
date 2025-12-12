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
package com.android.settings.supervision

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import com.android.settings.R

/** Dialog to explicitly ask the user for permission to supervise the device. */
class EnableSupervisionDialogFragment : DialogFragment() {

    companion object {
        const val DIALOG_POSITIVE_BUTTON_CLICKED = "dialog_positive_button_clicked"
        const val DIALOG_NEGATIVE_BUTTON_CLICKED = "dialog_negative_button_clicked"
        const val DIALOG_DISMISSED = "dialog_dismissed"
        private const val ARG_MESSAGE = "supervision_message"
        private const val ARG_SUPERVISION_APP_NAME = "supervision_app_name"

        fun newInstance(
            message: CharSequence? = null,
            supervisionAppName: CharSequence? = null,
        ): EnableSupervisionDialogFragment {
            val fragment = EnableSupervisionDialogFragment()
            val args =
                Bundle().apply {
                    putCharSequence(ARG_MESSAGE, message)
                    putCharSequence(ARG_SUPERVISION_APP_NAME, supervisionAppName)
                }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)

        val builder = AlertDialog.Builder(requireContext())
        builder
            .setTitle(R.string.profile_owner_add_title_simplified)
            .setPositiveButton(R.string.allow) { _, _ ->
                setFragmentResult(DIALOG_POSITIVE_BUTTON_CLICKED, Bundle())
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                setFragmentResult(DIALOG_NEGATIVE_BUTTON_CLICKED, Bundle())
            }
            .setView(R.layout.enable_supervision_dialog)

        val dialog = builder.create()

        dialog.setOnShowListener { _ ->
            val message = arguments?.getString(ARG_MESSAGE)
            dialog.findViewById<TextView>(R.id.supervision_activation_message)?.setText(message)

            val supervisionAppName = arguments?.getString(ARG_SUPERVISION_APP_NAME)
            val warning = getString(R.string.device_admin_warning_simplified, supervisionAppName)
            dialog.findViewById<TextView>(R.id.supervision_activation_warning)?.setText(warning)

            val positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            positiveButton?.setFilterTouchesWhenObscured(true)
        }

        return dialog
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        setFragmentResult(DIALOG_DISMISSED, Bundle())
    }

    override fun onDetach() {
        super.onDetach()
    }
}
