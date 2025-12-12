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

package com.android.settings.wifi.details2

import android.content.Context
import com.android.settings.R
import com.android.settings.core.TogglePreferenceController
import com.android.wifitrackerlib.WifiEntry

class WifiEditConfigPreferenceController(
    context: Context,
    preferenceKey: String,
    private val wifiEntry: WifiEntry,
) : TogglePreferenceController(context, preferenceKey) {

    override fun getAvailabilityStatus(): Int {
        return if (com.android.settings.connectivity.Flags.wifiMultiuser()) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }

    override fun isChecked(): Boolean {
        return wifiEntry.isModifiableByOtherUsers()
    }

    override fun setChecked(isChecked: Boolean): Boolean {
        wifiEntry.setModifiableByOtherUsers(isChecked)
        return true
    }

    override fun getSliceHighlightMenuRes(): Int {
        return R.string.menu_key_network
    }
}
