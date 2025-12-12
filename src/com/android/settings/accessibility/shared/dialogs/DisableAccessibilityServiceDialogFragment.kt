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

package com.android.settings.accessibility.shared.dialogs

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Dialog
import android.app.settings.SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_DISABLE
import android.content.ComponentName
import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import com.android.settings.R
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.useService
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

private const val ARG_FEATURE_COMPONENT_NAME = "serviceComponentName"

class DisableAccessibilityServiceDialogFragment : InstrumentedDialogFragment() {
    private lateinit var serviceInfo: AccessibilityServiceInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val componentName =
            ComponentName.unflattenFromString(
                requireArguments().getString(ARG_FEATURE_COMPONENT_NAME) ?: ""
            )
        val serviceInfo =
            componentName?.let {
                AccessibilityRepositoryProvider.get(requireContext())
                    .getAccessibilityServiceInfo(it)
            }
        if (serviceInfo == null) {
            dismiss()
        } else {
            this.serviceInfo = serviceInfo
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val listener =
            object : DialogInterface.OnClickListener {
                override fun onClick(dialog: DialogInterface?, which: Int) {
                    when (which) {
                        DialogInterface.BUTTON_POSITIVE -> {
                            serviceInfo.useService(requireContext(), enabled = false)
                        }
                        DialogInterface.BUTTON_NEGATIVE -> {
                            // Do nothing.
                        }
                    }
                }
            }

        return AlertDialog.Builder(context)
            .setTitle(
                context.getString(
                    R.string.disable_service_title,
                    serviceInfo.getFeatureName(context),
                )
            )
            .setCancelable(true)
            .setPositiveButton(R.string.accessibility_dialog_button_stop, listener)
            .setNegativeButton(R.string.accessibility_dialog_button_cancel, listener)
            .create()
    }

    override fun getMetricsCategory(): Int {
        return DIALOG_ACCESSIBILITY_SERVICE_DISABLE
    }

    companion object {
        @JvmStatic
        fun showDialog(fragmentManager: FragmentManager, componentName: ComponentName) {
            val bundle =
                Bundle().apply {
                    putString(ARG_FEATURE_COMPONENT_NAME, componentName.flattenToString())
                }

            DisableAccessibilityServiceDialogFragment().apply {
                arguments = bundle
                show(
                    fragmentManager,
                    /* tag= */ DisableAccessibilityServiceDialogFragment::class.simpleName,
                )
            }
        }
    }
}
