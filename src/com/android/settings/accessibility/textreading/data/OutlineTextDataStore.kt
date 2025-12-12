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

package com.android.settings.accessibility.textreading.data

import android.content.Context
import android.provider.Settings
import android.util.Log
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.HighContrastTextMigrationReceiver
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settings.core.instrumentation.SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED
import com.android.settings.core.instrumentation.SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_HIGH_CONTRAST_TEXT
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore

/** DataStore for outline text preference. */
class OutlineTextDataStore(
    context: Context,
    @EntryPoint private val entryPoint: Int = EntryPoint.UNKNOWN_ENTRY,
    private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore
        get() = settingsStore

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return false as? T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean?) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }
        val newValue = value as? Boolean ?: getDefaultValue(key, Boolean::class.java)!!
        keyValueStoreDelegate.setBoolean(
            Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED,
            newValue,
        )

        SettingsStatsLog.write(
            ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
            ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_HIGH_CONTRAST_TEXT,
            if (newValue) 1 else 0,
            AccessibilityStatsLogUtils.convertToEntryPoint(entryPoint),
        )

        // Set PROMPT_UNNECESSARY when the user modifies the HighContrastText setting
        // This is needed for the following scenario:
        // On Android 16, create secondary user, ACTION_PRE_BOOT_COMPLETED won't be sent to
        // the secondary user. The user enables HCT.
        // When updating OS to Android 17, ACTION_PRE_BOOT_COMPLETED will be sent to the
        // secondary user when switch to the secondary user.
        // If the prompt status is not updated in Android 16, we would automatically disable
        // HCT and show the HCT prompt, which is an undesired behavior.
        settingsStore.setInt(
            Settings.Secure.ACCESSIBILITY_HCT_RECT_PROMPT_STATUS,
            HighContrastTextMigrationReceiver.PromptState.PROMPT_UNNECESSARY,
        )
    }

    companion object {
        private const val LOG_TAG = "OutlineTextDataStore"
    }
}
