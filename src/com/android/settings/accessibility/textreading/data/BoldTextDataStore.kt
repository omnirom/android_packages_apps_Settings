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
import android.graphics.fonts.FontStyle
import android.provider.Settings
import android.util.Log
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.textreading.ui.BoldTextPreference
import com.android.settings.core.instrumentation.SettingsStatsLog
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSecureStore

/** Datastore for [BoldTextPreference] */
class BoldTextDataStore(
    context: Context,
    @EntryPoint private val entryPoint: Int = EntryPoint.UNKNOWN_ENTRY,
    private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) : KeyValueStoreDelegate {
    override val keyValueStoreDelegate: KeyValueStore
        get() = settingsStore

    override fun contains(key: String): Boolean {
        return keyValueStoreDelegate.contains(KEY)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>): T? {
        return false as? T?
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return (keyValueStoreDelegate.getInt(KEY) == BOLD_TEXT_ADJUSTMENT) as? T?
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean?) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }

        val newValue = value as? Boolean ?: getDefaultValue(key, Boolean::class.javaObjectType)!!
        keyValueStoreDelegate.setInt(KEY, if (newValue) BOLD_TEXT_ADJUSTMENT else 0)

        SettingsStatsLog.write(
            SettingsStatsLog.ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED,
            SettingsStatsLog
                .ACCESSIBILITY_TEXT_READING_OPTIONS_CHANGED__NAME__TEXT_READING_BOLD_TEXT,
            if (newValue) 1 else 0,
            AccessibilityStatsLogUtils.convertToEntryPoint(entryPoint),
        )
    }

    companion object {
        internal const val BOLD_TEXT_ADJUSTMENT =
            FontStyle.FONT_WEIGHT_BOLD - FontStyle.FONT_WEIGHT_NORMAL
        const val KEY = Settings.Secure.FONT_WEIGHT_ADJUSTMENT
        private const val LOG_TAG = "BoldTextDataStore"
    }
}
