/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.display

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.graphics.drawable.ClipDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.Display.Mode
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.android.settings.R
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Resolution change confirmation pop-up dialog which has a countdown to auto-revert the change if
 * change was not confirmed
 */
class ResolutionChangeDialogFragment : DialogFragment() {

    private var countdownTimer: CountDownTimer? = null
    private var isConfirmed = false

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val customTitleView =
            requireActivity()
                .layoutInflater
                .inflate(R.layout.resolution_change_dialog_title_view, null)

        return AlertDialog.Builder(requireActivity())
            .apply {
                setCustomTitle(customTitleView)
                setMessage(R.string.external_display_resolution_change_dialog_description)
                setPositiveButton(R.string.external_display_resolution_change_dialog_confirm) { _, _
                    ->
                    isConfirmed = true
                    Log.i(TAG, "Change was confirmed, keep updated resolution")
                    parentFragmentManager.setFragmentResult(
                        KEY_RESULT,
                        Bundle().apply {
                            putBoolean(KEY_CONFIRMED, true)
                            putParcelable(
                                KEY_NEW_MODE,
                                arguments?.getParcelable(KEY_NEW_MODE, Mode::class.java),
                            )
                        },
                    )
                }
                setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            }
            .create()
            .also { it.setCanceledOnTouchOutside(true) }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (isConfirmed) {
            return
        }
        Log.i(TAG, "Dialog was dismissed, cancelling update")
        parentFragmentManager.setFragmentResult(
            KEY_RESULT,
            Bundle().apply {
                putBoolean(KEY_CONFIRMED, false)
                putParcelable(
                    KEY_EXISTING_MODE,
                    arguments?.getParcelable(KEY_EXISTING_MODE, Mode::class.java),
                )
            },
        )
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? AlertDialog ?: return
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE)?.let {
            setupCancelButton(it)
            startCountdown(it)
        }
    }

    override fun onStop() {
        super.onStop()
        countdownTimer?.cancel()
        countdownTimer = null
        // Cancel to ensure resolution change is reverted even on device rotation or when settings
        // got minimized
        dismissAllowingStateLoss()
    }

    private fun setupCancelButton(cancelButton: Button) {
        cancelButton.apply {
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close, 0, 0, 0)
            compoundDrawableTintList = textColors
            val paddingPx = (4 * resources.displayMetrics.density).toInt()
            compoundDrawablePadding = paddingPx
            setBackgroundResource(R.drawable.resolution_change_cancel_countdown_button_background)
        }
    }

    private fun startCountdown(cancelButton: Button) {
        val buttonBackground = cancelButton.background as LayerDrawable
        val progressDrawable =
            buttonBackground.findDrawableByLayerId(R.id.resolution_change_cancel_countdown_progress)
                as ClipDrawable
        val totalCountdownMillis = CONFIRMATION_TIMEOUT.inWholeMilliseconds

        countdownTimer =
            object :
                    CountDownTimer(
                        totalCountdownMillis,
                        CONFIRMATION_TIMEOUT_UI_UPDATE_INTERVAL.inWholeMilliseconds,
                    ) {
                    override fun onTick(millisUntilFinished: Long) {
                        val progressLevel =
                            ((totalCountdownMillis - millisUntilFinished) *
                                    CONFIRMATION_TIMEOUT_PROGRESS_SCALE / totalCountdownMillis)
                                .toInt()
                        progressDrawable.level = progressLevel
                    }

                    override fun onFinish() {
                        dialog?.dismiss()
                    }
                }
                .start()
        Log.i(TAG, "Started resolution change cancellation countdown")
    }

    companion object {
        const val TAG = "ResolutionChangeDialog"
        const val KEY_RESULT = "resolution_change_dialog_key"
        const val KEY_CONFIRMED = "is_confirmed"
        const val KEY_NEW_MODE = "new_mode"
        const val KEY_EXISTING_MODE = "existing_mode"

        private val CONFIRMATION_TIMEOUT = 15.seconds
        private val CONFIRMATION_TIMEOUT_UI_UPDATE_INTERVAL = 50.milliseconds
        private val CONFIRMATION_TIMEOUT_PROGRESS_SCALE = 10000

        fun newInstance(newMode: Mode, existingMode: Mode) =
            ResolutionChangeDialogFragment().apply {
                arguments =
                    Bundle().apply {
                        putParcelable(KEY_NEW_MODE, newMode)
                        putParcelable(KEY_EXISTING_MODE, existingMode)
                    }
            }
    }
}
