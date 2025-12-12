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
import com.android.settings.R
import com.android.settings.accessibility.HtmlFooterPreferenceController

open class AccessibilityActivityHtmlFooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey) {

    open fun initialize(shortcutInfo: AccessibilityShortcutInfo) {
        val packageManager = mContext.packageManager!!
        shortcutInfo.loadHtmlDescription(packageManager)?.let {
            setSummary(it, isHtml = true)

            introductionTitle =
                mContext.getString(
                    R.string.accessibility_introduction_title,
                    shortcutInfo.activityInfo?.loadLabel(mContext.packageManager),
                )
        }
    }
}

class AccessibilityActivityFooterPreferenceController(context: Context, prefKey: String) :
    AccessibilityActivityHtmlFooterPreferenceController(context, prefKey) {

    override fun initialize(shortcutInfo: AccessibilityShortcutInfo) {
        val packageManager = mContext.packageManager!!
        shortcutInfo.loadDescription(packageManager)?.let {
            setSummary(it, isHtml = false)

            introductionTitle =
                mContext.getString(
                    R.string.accessibility_introduction_title,
                    shortcutInfo.activityInfo?.loadLabel(mContext.packageManager),
                )
        }
    }
}
