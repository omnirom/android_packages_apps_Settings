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
import android.provider.Settings
import com.android.server.accessibility.Flags
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
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class OneFingerPanningSwitchPreference :
    SwitchPreference(KEY, R.string.accessibility_magnification_one_finger_panning_title),
    PreferenceSummaryProvider,
    PreferenceAvailabilityProvider,
    PreferenceLifecycleProvider,
    KeyedObserver<String?> {

    private lateinit var lifecycleContext: PreferenceLifecycleContext

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

    override fun storage(context: Context): KeyValueStore = context.dataStore

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

    override fun isAvailable(context: Context): Boolean {
        return Flags.enableMagnificationOneFingerPanningGesture() &&
            !context.isInSetupWizard() &&
            context.isWindowMagnificationSupported()
    }

    override fun isEnabled(context: Context): Boolean {
        @MagnificationCapabilities.MagnificationMode
        val mode = MagnificationCapabilities.getCapabilities(context)
        return mode == FULLSCREEN || mode == ALL
    }

    override fun getSummary(context: Context): CharSequence? {
        return context.getText(
            if (isEnabled(context)) {
                R.string.accessibility_magnification_one_finger_panning_summary
            } else {
                R.string.accessibility_magnification_one_finger_panning_summary_unavailable
            }
        )
    }

    override fun onKeyChanged(key: String?, reason: Int) {
        lifecycleContext.notifyPreferenceChange(KEY)
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED
        private val Context.dataStore: KeyValueStore
            get() =
                SettingsSecureStore.get(this).apply {
                    setDefaultValue(
                        KEY,
                        this@dataStore.getResources()
                            .getBoolean(
                                com.android.internal.R.bool
                                    .config_enable_a11y_magnification_single_panning
                            ),
                    )
                }
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/OneFingerPanningPreferenceController.java)
