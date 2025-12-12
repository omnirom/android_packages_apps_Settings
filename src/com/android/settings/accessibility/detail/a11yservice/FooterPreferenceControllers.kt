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

package com.android.settings.accessibility.detail.a11yservice

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.text.TextUtils
import com.android.settings.R
import com.android.settings.accessibility.HtmlFooterPreferenceController
import com.android.settings.accessibility.extensions.getFeatureName
import com.android.settings.accessibility.extensions.isServiceEnabled

class AccessibilityServiceHtmlFooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey) {

    fun initialize(serviceInfo: AccessibilityServiceInfo) {
        val packageManager = mContext.packageManager!!
        serviceInfo.loadHtmlDescription(packageManager)?.let {
            setSummary(it, isHtml = true)

            introductionTitle =
                mContext.getString(
                    R.string.accessibility_introduction_title,
                    serviceInfo.getFeatureName(mContext),
                )
        }
    }
}

class AccessibilityServiceFooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey) {

    fun initialize(serviceInfo: AccessibilityServiceInfo) {
        val packageManager = mContext.packageManager!!
        val description =
            if (shouldShowCrashDescription(serviceInfo)) {
                mContext.getText(R.string.accessibility_description_state_stopped)
            } else {
                serviceInfo.loadDescription(packageManager)
            }

        if (!TextUtils.isEmpty(description)) {
            setSummary(description, isHtml = false)
            introductionTitle =
                mContext.getString(
                    R.string.accessibility_introduction_title,
                    serviceInfo.getFeatureName(mContext),
                )
        }
    }

    private fun shouldShowCrashDescription(serviceInfo: AccessibilityServiceInfo): Boolean {
        return serviceInfo.isServiceEnabled(mContext) && serviceInfo.crashed
    }
}
