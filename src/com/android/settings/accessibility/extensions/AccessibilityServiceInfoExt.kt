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

package com.android.settings.accessibility.extensions

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.text.BidiFormatter
import android.view.accessibility.AccessibilityManager
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settingslib.accessibility.AccessibilityUtils

fun AccessibilityServiceInfo.getFeatureName(context: Context): CharSequence {
    val locale = context.resources.configuration.getLocales().get(0)
    return resolveInfo?.loadLabel(context.packageManager)?.let {
        BidiFormatter.getInstance(locale).unicodeWrap(it)
    } ?: ""
}

fun AccessibilityServiceInfo.isServiceWarningRequired(context: Context): Boolean {
    val a11yManager: AccessibilityManager? =
        context.getSystemService(AccessibilityManager::class.java)
    return if (a11yManager != null) {
        return a11yManager.isAccessibilityServiceWarningRequired(this)
    } else {
        return true
    }
}

fun AccessibilityServiceInfo.targetSdkIsAtLeast(sdkVersionCode: Int): Boolean {
    val targetSdk = resolveInfo?.serviceInfo?.applicationInfo?.targetSdkVersion ?: 0
    return targetSdk >= sdkVersionCode
}

fun AccessibilityServiceInfo.useService(context: Context, enabled: Boolean) {
    AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(componentName, enabled)
    AccessibilityUtils.setAccessibilityServiceState(context, componentName, enabled)
}

fun AccessibilityServiceInfo.isServiceEnabled(context: Context): Boolean {
    return AccessibilityUtils.getEnabledServicesFromSettings(context).contains(componentName)
}
