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

package com.android.settings.accessibility.screenmagnification

import android.content.Context
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.settings.accessibility.ToggleShortcutPreferenceController

/**
 * The preference controller for the ShortcutPreference on the Magnification screen. The only
 * difference is that we allow more shortcut options for Magnifications.
 */
class ToggleMagnificationShortcutPreferenceController(context: Context, key: String) :
    ToggleShortcutPreferenceController(context, key) {

    override val shortcutSettingsKey = ShortcutConstants.MAGNIFICATION_SHORTCUT_SETTINGS.toList()

    override fun getComponentNameAsString(): String {
        return AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
    }
}
