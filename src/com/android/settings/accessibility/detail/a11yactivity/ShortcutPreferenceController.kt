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

package com.android.settings.accessibility.detail.a11yactivity

import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.Context
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.accessibility.ToggleShortcutPreferenceController

class ShortcutPreferenceController(context: Context, prefKey: String) :
    ToggleShortcutPreferenceController(context, prefKey) {
    private var shortcutTitle: CharSequence? = null

    fun initialize(
        shortcutInfo: AccessibilityShortcutInfo,
        fragmentManager: FragmentManager,
        featureName: CharSequence,
        sourceMetricsCategory: Int,
    ) {
        super.initialize(
            shortcutInfo.componentName,
            fragmentManager,
            featureName,
            sourceMetricsCategory,
        )
        shortcutTitle =
            mContext.getString(
                R.string.accessibility_shortcut_title,
                shortcutInfo.activityInfo?.loadLabel(mContext.packageManager!!),
            )
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        shortcutTitle?.let { screen?.findPreference<Preference>(preferenceKey)?.title = it }
    }
}
