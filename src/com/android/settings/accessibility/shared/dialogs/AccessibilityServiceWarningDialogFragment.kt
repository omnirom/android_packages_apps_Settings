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
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.setFragmentResult
import com.android.internal.accessibility.dialog.AccessibilityServiceWarning
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider
import com.android.settings.core.instrumentation.InstrumentedDialogFragment

class AccessibilityServiceWarningDialogFragment : InstrumentedDialogFragment() {
    private var source: Int = 0
    private lateinit var requestKey: String
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

        source = requireArguments().getInt(ARG_SOURCE)
        requestKey = requireArguments().getString(ARG_REQUEST_KEY, "")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context: Context = requireContext()
        val allowListener =
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    setFragmentResult(
                        requestKey,
                        Bundle().apply {
                            putString(RESULT_STATUS, RESULT_STATUS_ALLOW)
                            putInt(RESULT_DIALOG_CONTEXT, source)
                        },
                    )
                    dismiss()
                }
            }
        val denyListener =
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    setFragmentResult(
                        requestKey,
                        Bundle().apply {
                            putString(RESULT_STATUS, RESULT_STATUS_DENY)
                            putInt(RESULT_DIALOG_CONTEXT, source)
                        },
                    )
                    dismiss()
                }
            }
        val uninstallListener =
            object : View.OnClickListener {
                override fun onClick(v: View?) {
                    uninstallAccessibilityService()
                }
            }

        val alertDialog =
            AlertDialog.Builder(context)
                .setView(
                    AccessibilityServiceWarning.createAccessibilityServiceWarningDialogContentView(
                        context,
                        serviceInfo,
                        allowListener,
                        denyListener,
                        uninstallListener,
                    )
                )
                .setCancelable(true)
                .create()
        alertDialog.window?.run {
            val params = attributes
            params.privateFlags =
                params.privateFlags or
                    WindowManager.LayoutParams.SYSTEM_FLAG_HIDE_NON_SYSTEM_OVERLAY_WINDOWS
            attributes = params
        }
        return alertDialog
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.DIALOG_ACCESSIBILITY_SERVICE_ENABLE
    }

    private fun uninstallAccessibilityService() {
        val appInfo = serviceInfo.resolveInfo.serviceInfo.applicationInfo
        val packageUri = ("package:${appInfo.packageName}").toUri()
        val uninstallIntent = Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri)
        startActivity(uninstallIntent)
        dismiss()
    }

    companion object {
        private const val ARG_FEATURE_COMPONENT_NAME = "serviceComponentName"
        private const val ARG_SOURCE = "source"
        private const val ARG_REQUEST_KEY = "requestKey"

        @VisibleForTesting const val RESULT_STATUS = "status"
        const val RESULT_STATUS_ALLOW = "allow"
        const val RESULT_STATUS_DENY = "deny"
        @VisibleForTesting const val RESULT_DIALOG_CONTEXT = "dialogContext"

        @JvmStatic
        fun showDialog(
            fragmentManager: FragmentManager,
            componentName: ComponentName,
            @DialogEnums source: Int,
            requestKey: String,
        ) {
            val bundle =
                Bundle().apply {
                    putString(ARG_FEATURE_COMPONENT_NAME, componentName.flattenToString())
                    putInt(ARG_SOURCE, source)
                    putString(ARG_REQUEST_KEY, requestKey)
                }

            AccessibilityServiceWarningDialogFragment().apply {
                arguments = bundle
                show(
                    fragmentManager,
                    /* tag= */ AccessibilityServiceWarningDialogFragment::class.simpleName,
                )
            }
        }

        /** Returns [RESULT_STATUS_ALLOW] or [RESULT_STATUS_DENY] */
        @JvmStatic
        fun getResultStatus(bundle: Bundle): String? {
            return bundle.getString(RESULT_STATUS)
        }

        /** Returns the source dialog enum used when showing the dialog */
        @JvmStatic
        fun getResultDialogContext(bundle: Bundle): Int {
            return bundle.getInt(RESULT_DIALOG_CONTEXT)
        }
    }
}
