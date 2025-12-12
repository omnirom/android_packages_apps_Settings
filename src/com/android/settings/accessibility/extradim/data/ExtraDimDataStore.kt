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

package com.android.settings.accessibility.extradim.data

import android.Manifest
import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.provider.Settings
import android.util.Log
import com.android.internal.accessibility.AccessibilityShortcutController.REDUCE_BRIGHT_COLORS_COMPONENT_NAME
import com.android.settings.accessibility.AccessibilityStatsLogUtils
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore

/**
 * Data store for Extra Dim feature.
 *
 * This class provides access to the Extra Dim feature's activation status. It allows getting and
 * setting the status, as well as observing changes.
 */
class ExtraDimDataStore(private val context: Context) :
    AbstractKeyedDataObservable<String>(), KeyValueStore, KeyedObserver<String> {

    private val colorDisplayManager = context.getSystemService(ColorDisplayManager::class.java)
    private val settingsSecureStore by lazy { SettingsSecureStore.get(context) }

    override fun contains(key: String): Boolean = settingsSecureStore.contains(SETTING_KEY)

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? {
        return colorDisplayManager?.isReduceBrightColorsActivated as? T
    }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (value !is Boolean) {
            Log.w(LOG_TAG, "Unsupported $valueType for $key: $value")
            return
        }
        value as Boolean
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(
            REDUCE_BRIGHT_COLORS_COMPONENT_NAME,
            value,
        )
        colorDisplayManager?.isReduceBrightColorsActivated = value
    }

    override fun onFirstObserverAdded() {
        settingsSecureStore.addObserver(SETTING_KEY, this, HandlerExecutor.main)
    }

    override fun onLastObserverRemoved() {
        settingsSecureStore.removeObserver(SETTING_KEY, this)
    }

    override fun onKeyChanged(key: String, reason: Int) {
        notifyChange(reason)
    }

    companion object {
        internal const val SETTING_KEY = Settings.Secure.REDUCE_BRIGHT_COLORS_ACTIVATED
        private const val LOG_TAG = "ExtraDimDataStore"

        fun getReadPermissions() = Permissions.EMPTY

        fun getWritePermissions() =
            Permissions.allOf(Manifest.permission.CONTROL_DISPLAY_COLOR_TRANSFORMS)
    }
}
