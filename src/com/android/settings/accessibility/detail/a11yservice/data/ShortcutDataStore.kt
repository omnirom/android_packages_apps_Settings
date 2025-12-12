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

package com.android.settings.accessibility.detail.a11yservice.data

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.os.Build
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE
import com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.extensions.targetSdkIsAtLeast
import com.android.settings.accessibility.shared.data.AccessibilityShortcutDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class ShortcutDataStore(
    context: Context,
    private val serviceInfo: AccessibilityServiceInfo,
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) :
    AccessibilityShortcutDataStore(
        context,
        serviceInfo.componentName,
        coroutineScope,
        settingsStore,
    ) {

    init {
        if (!serviceInfo.targetSdkIsAtLeast(Build.VERSION_CODES.R)) {
            // When the targetSdk is less than R, we only support hardware shortcut
            PreferredShortcuts.saveUserShortcutType(
                context,
                PreferredShortcut(serviceInfo.componentName.flattenToString(), HARDWARE),
            )
        }
    }

    override fun getDefaultShortcutTypes(): Int {
        val hasQsTile = serviceInfo.tileServiceName?.isNotEmpty() == true
        val isAccessibilityTool = serviceInfo.isAccessibilityTool
        return if (serviceInfo.targetSdkIsAtLeast(Build.VERSION_CODES.R)) {
            if (isAccessibilityTool && hasQsTile) {
                QUICK_SETTINGS
            } else {
                super.getDefaultShortcutTypes()
            }
        } else {
            HARDWARE
        }
    }
}
