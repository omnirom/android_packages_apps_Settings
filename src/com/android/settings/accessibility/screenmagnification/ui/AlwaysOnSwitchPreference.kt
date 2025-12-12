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

package com.android.settings.accessibility.screenmagnification.ui

import android.content.Context
import android.provider.DeviceConfig
import android.provider.Settings
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.ShortcutConstants
import com.android.internal.accessibility.util.ShortcutUtils
import com.android.settings.R
import com.android.settings.accessibility.MagnificationCapabilities
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode.ALL
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode.FULLSCREEN
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIndexableProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class AlwaysOnSwitchPreference :
    SwitchPreference(KEY, R.string.accessibility_screen_magnification_always_on_title),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceIndexableProvider,
    PreferenceLifecycleProvider,
    KeyedObserver<String?> {

    private lateinit var lifecycleContext: PreferenceLifecycleContext

    override fun storage(context: Context): KeyValueStore = SettingsSecureStore.get(context)

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int? = ReadWritePermit.ALLOW

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        storage(context)
            .addObserver(MagnificationCapabilities.KEY_CAPABILITY, this, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        storage(context).removeObserver(this)
    }

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() &&
            context.isWindowMagnificationSupported() &&
            isAlwaysOnSupported(context)
    }

    override fun isIndexable(context: Context): Boolean {
        // This preference's title "Keep on while switching apps" does not
        // mention magnification so it may confuse users who search a term
        // like "Keep on".
        // So we hide it from search if the user has no magnification shortcut enabled.
        return if (
            ShortcutUtils.getEnabledShortcutTypes(
                context,
                AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME,
            ) == ShortcutConstants.UserShortcutType.DEFAULT
        ) {
            false
        } else {
            true
        }
    }

    override fun isEnabled(context: Context): Boolean {
        @MagnificationCapabilities.MagnificationMode
        val mode = MagnificationCapabilities.getCapabilities(context)
        return mode == FULLSCREEN || mode == ALL
    }

    override fun getSummary(context: Context): CharSequence? {
        return context.getText(
            if (isEnabled(context)) {
                R.string.accessibility_screen_magnification_always_on_summary
            } else {
                R.string.accessibility_screen_magnification_always_on_unavailable_summary
            }
        )
    }

    override fun onKeyChanged(key: String?, reason: Int) {
        lifecycleContext.notifyPreferenceChange(KEY)
    }

    private fun isAlwaysOnSupported(context: Context): Boolean {
        val defaultValue =
            context
                .getResources()
                .getBoolean(com.android.internal.R.bool.config_magnification_always_on_enabled)
        return DeviceConfig.getBoolean(
            DeviceConfig.NAMESPACE_WINDOW_MANAGER,
            "AlwaysOnMagnifier__enable_always_on_magnifier",
            defaultValue,
        )
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/AlwaysOnPreferenceController.java)
