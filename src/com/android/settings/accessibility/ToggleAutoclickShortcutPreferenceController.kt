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

import com.android.server.accessibility.Flags

import android.content.Context

/**
 * ShortcutPreference's controller on [ToggleAutoclickPreferenceFragment].
 */
class ToggleAutoclickShortcutPreferenceController(context: Context, preferenceKey: String) :
    ToggleShortcutPreferenceController(context, preferenceKey) {

        override fun getAvailabilityStatus(): Int {
        // Note: when the flag is shipped, remove this class and replace its reference
        // in accessibility_autoclick_settings xml
        return if (Flags.enableAutoclickIndicator()) {
            AVAILABLE
        } else {
            CONDITIONALLY_UNAVAILABLE
        }
    }
}
