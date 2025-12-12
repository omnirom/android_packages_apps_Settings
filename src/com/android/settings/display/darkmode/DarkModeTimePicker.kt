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

package com.android.settings.display.darkmode

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TimePicker
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.android.settings.core.instrumentation.InstrumentedDialogFragment
import java.time.LocalTime

internal class DarkModeTimePicker(private val metricsCategory: Int) :
    InstrumentedDialogFragment(), TimePickerDialog.OnTimeSetListener {
    private lateinit var requestKey: String

    override fun getMetricsCategory(): Int {
        return metricsCategory
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        requestKey = requireArguments().getString(ARG_REQUEST_KEY, "")
        val initialHour = requireArguments().getInt(ARG_INITIAL_HOUR)
        val initialMinute = requireArguments().getInt(ARG_INITIAL_MINUTE)
        val context = requireContext()

        return TimePickerDialog(
            context,
            this,
            initialHour,
            initialMinute,
            TimeFormatter(context).is24HourFormat,
        )
    }

    override fun onTimeSet(view: TimePicker?, hourOfDay: Int, minute: Int) {
        val selectedTime = LocalTime.of(hourOfDay, minute)

        setFragmentResult(
            requestKey,
            Bundle().apply {
                putInt(RESULT_HOUR, selectedTime.hour)
                putInt(RESULT_MINUTE, selectedTime.minute)
            },
        )
    }

    companion object {
        internal const val ARG_REQUEST_KEY = "requestKey"
        internal const val ARG_INITIAL_HOUR = "initialHour"
        internal const val ARG_INITIAL_MINUTE = "initialMinute"

        internal const val RESULT_HOUR = "resultHour"
        internal const val RESULT_MINUTE = "resultMinute"

        /**
         * Shows the TimePickerDialogFragment.
         *
         * @param fragmentManager The FragmentManager to use for showing the dialog.
         * @param requestKey A unique key to identify the result when it's returned.
         * @param initialTime The initial time for the picker. Defaults to now.
         */
        @JvmStatic
        fun showDialog(
            fragmentManager: FragmentManager,
            requestKey: String,
            metricsCategory: Int,
            initialTime: LocalTime = LocalTime.now(),
        ) {
            val bundle =
                Bundle().apply {
                    putString(ARG_REQUEST_KEY, requestKey)
                    putInt(ARG_INITIAL_HOUR, initialTime.hour)
                    putInt(ARG_INITIAL_MINUTE, initialTime.minute)
                }

            DarkModeTimePicker(metricsCategory).apply {
                arguments = bundle
                show(fragmentManager, DarkModeTimePicker::class.simpleName)
            }
        }

        /** Retrieves the selected hour from the result Bundle. */
        @JvmStatic
        fun getResultHour(bundle: Bundle): Int {
            return bundle.getInt(RESULT_HOUR)
        }

        /** Retrieves the selected minute from the result Bundle. */
        @JvmStatic
        fun getResultMinute(bundle: Bundle): Int {
            return bundle.getInt(RESULT_MINUTE)
        }
    }
}
