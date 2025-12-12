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

package com.android.settings.accessibility.detail.a11yactivity.ui

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.accessibility.shared.ui.ShortcutFeatureNameProvider
import com.android.settingslib.metadata.PreferenceTitleProvider

class ShortcutPreference(
    context: Context,
    private val shortcutInfo: AccessibilityShortcutInfo,
    metricCategory: Int,
) :
    AccessibilityShortcutPreference(
        context = context,
        key = KEY,
        componentName = shortcutInfo.componentName,
        metricsCategory = metricCategory,
    ),
    PreferenceTitleProvider,
    ShortcutFeatureNameProvider {

    override fun getTitle(context: Context): CharSequence? {
        return context.getString(R.string.accessibility_shortcut_title, getFeatureName(context))
    }

    override fun getFeatureName(context: Context): CharSequence {
        return shortcutInfo.activityInfo.loadLabel(context.packageManager)
    }

    companion object {
        private const val KEY = "shortcut_preference_key"
    }
}
