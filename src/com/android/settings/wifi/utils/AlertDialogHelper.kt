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

package com.android.settings.wifi.utils

import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_POSITIVE
import android.util.Log
import androidx.appcompat.app.AlertDialog

open class AlertDialogHelper(
    val alertDialog: AlertDialog,
    val onClickListener: DialogInterface.OnClickListener? = null,
) {

    init {
        alertDialog.setOnShowListener {
            alertDialog.getButton(BUTTON_POSITIVE)?.setOnClickListener {
                onPositiveButtonClicked()
            } ?: Log.e(TAG, "Can't get the positive button!")
        }
    }

    open fun onPositiveButtonClicked() {
        if (!canDismiss()) {
            Log.w(TAG, "Can't dismiss dialog!")
            return
        }
        onClickListener?.onClick(alertDialog, BUTTON_POSITIVE)
        alertDialog.dismiss()
    }

    open fun canDismiss() = true

    companion object {
        const val TAG = "AlertDialogHelper"
    }
}
