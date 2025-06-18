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

import android.util.Log
import android.view.View
import com.android.settings.R
import com.android.wifitrackerlib.WifiEntry.SECURITY_NONE
import com.android.wifitrackerlib.WifiEntry.SECURITY_PSK
import com.android.wifitrackerlib.WifiEntry.SECURITY_SAE
import com.android.wifitrackerlib.WifiEntry.SECURITY_WEP

/**
 * The Wi-Fi password {@code TextInputGroup} that supports input validation.
 */
class WifiPasswordInput(
    view: View,
    var security: Int = SECURITY_NONE,
) : TextInputGroup(view, R.id.password_input_layout, R.id.password, R.string.wifi_field_required) {

    var canBeEmpty: Boolean = false

    override fun validate(): Boolean {
        if (!editText.isShown) return true
        if (canBeEmpty && text.isEmpty()) return true

        return when (security) {
            SECURITY_WEP -> super.validate()
            SECURITY_PSK -> super.validate() && isValidPsk(text).also { valid ->
                if (!valid) {
                    error = view.context.getString(R.string.wifi_password_invalid)
                    Log.w(TAG, "validate failed in ${layout.hint ?: "unknown"} for PSK")
                }
            }

            SECURITY_SAE -> super.validate() && isValidSae(text).also { valid ->
                if (!valid) {
                    error = view.context.getString(R.string.wifi_password_invalid)
                    Log.w(TAG, "validate failed in ${layout.hint ?: "unknown"} for SAE")
                }
            }

            else -> true
        }
    }

    companion object {
        const val TAG = "WifiPasswordInput"

        fun isValidPsk(password: String): Boolean {
            return (password.length == 64 && password.matches("[0-9A-Fa-f]{64}".toRegex())) ||
                    (password.length in 8..63)
        }

        fun isValidSae(password: String): Boolean {
            return password.length in 1..63
        }
    }
}