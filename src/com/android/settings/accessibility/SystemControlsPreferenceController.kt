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

package com.android.settings.accessibility

import android.content.Context
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.gestures.OneHandedSettingsUtils

/**
 * Preference controller for the System Controls entry point in Accessibility settings.
 *
 * This controller manages the display and summary text for the "System Controls" preference.
 */
class SystemControlsPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {
    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun getSummary(): CharSequence? {
        return if (OneHandedSettingsUtils.isSupportOneHandedMode()) {
            mContext.getString(R.string.accessibility_system_controls_subtext)
        } else {
            mContext.getString(
                R.string.accessibility_system_controls_subtext_one_handed_not_supported
            )
        }
    }
}
