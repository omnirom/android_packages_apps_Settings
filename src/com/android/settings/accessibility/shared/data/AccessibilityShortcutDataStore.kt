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

package com.android.settings.accessibility.shared.data

import android.content.ComponentName
import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.PreferredShortcut
import com.android.settings.accessibility.PreferredShortcuts
import com.android.settings.accessibility.shared.utils.debounce
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.DataChangeReason
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

open class AccessibilityShortcutDataStore(
    private val context: Context,
    private val componentName: ComponentName,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val settingsStore: KeyValueStore = SettingsSecureStore.get(context),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    private val shortcutSettingKeys =
        if (componentName == MAGNIFICATION_COMPONENT_NAME) {
            ShortcutConstants.MAGNIFICATION_SHORTCUT_SETTINGS.toList()
        } else {
            ShortcutConstants.GENERAL_SHORTCUT_SETTINGS.toList()
        }
    private val debounceUpdateData: () -> Unit by lazy {
        debounce(100.milliseconds, coroutineScope) {
            updatePreferredShortcuts()
            notifyChange(DataChangeReason.UPDATE)
        }
    }

    init {
        // Always update the shortcut data when data store is created, because the user could
        // change the shortcut types outside of this data store.
        updatePreferredShortcuts()
    }

    override fun contains(key: String): Boolean = true

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T? =
        ((AccessibilityUtil.getUserShortcutTypesFromSettings(context, componentName) !=
            ShortcutConstants.UserShortcutType.DEFAULT)
            as? T?)

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        if (valueType == Boolean::class.javaObjectType) {
            val enabled = value as? Boolean ?: false
            val shortcutTypes =
                PreferredShortcuts.retrieveUserShortcutType(
                    context,
                    getComponentNameAsString(),
                    getDefaultShortcutTypes(),
                )
            context
                .getSystemService(AccessibilityManager::class.java)
                ?.enableShortcutsForTargets(
                    enabled,
                    shortcutTypes,
                    setOf(getComponentNameAsString()),
                    context.userId,
                )
        }
    }

    override fun onFirstObserverAdded() {
        for (settingsKey in shortcutSettingKeys) {
            settingsStore.addObserver(settingsKey, this, HandlerExecutor.main)
        }
    }

    override fun onLastObserverRemoved() {
        for (settingsKey in shortcutSettingKeys) {
            settingsStore.removeObserver(settingsKey, this)
        }
    }

    override fun onKeyChanged(key: String, reason: Int) {
        debounceUpdateData.invoke()
    }

    @ShortcutConstants.UserShortcutType
    protected open fun getDefaultShortcutTypes(): Int = ShortcutConstants.UserShortcutType.SOFTWARE

    /** Returns the user preferred shortcut types or the default shortcut types if not set */
    @ShortcutConstants.UserShortcutType
    fun getUserShortcutTypes(): Int {
        // We must use PreferredShortcuts.retrieveUserShortcutType() here instead of reading from
        // Settings data by AccessibilityUtil.getUserShortcutTypesFromSettings()
        //
        // A one-way (asynchronous) binder call is used to update the shortcuts in Settings. This
        // creates a race condition where the value is retrieved before the setting is persisted,
        // causing a crash or wrong value. Reading from PreferredShortcut avoids this, as it's
        // updated synchronously with the user's selection, making it safer and more reliable.
        return PreferredShortcuts.retrieveUserShortcutType(
            context,
            getComponentNameAsString(),
            getDefaultShortcutTypes(),
        )
    }

    // TODO(b/147990389): Delete this function after we migrated to MAGNIFICATION_COMPONENT_NAME.
    private fun getComponentNameAsString() =
        if (componentName == MAGNIFICATION_COMPONENT_NAME) {
            MAGNIFICATION_CONTROLLER_NAME
        } else {
            componentName.flattenToString()
        }

    private fun updatePreferredShortcuts() {
        val shortcutTypes =
            AccessibilityUtil.getUserShortcutTypesFromSettings(context, componentName)
        if (shortcutTypes != ShortcutConstants.UserShortcutType.DEFAULT) {
            val shortcut = PreferredShortcut(getComponentNameAsString(), shortcutTypes)
            PreferredShortcuts.saveUserShortcutType(context, shortcut)
        }
    }
}
