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
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.extensions.isWindowMagnificationSupported
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference

// LINT.IfChange
class FollowTypingSwitchPreference :
    SwitchPreference(
        KEY,
        R.string.accessibility_screen_magnification_follow_typing_title,
        R.string.accessibility_screen_magnification_follow_typing_summary,
    ),
    PreferenceAvailabilityProvider {

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

    override fun isAvailable(context: Context): Boolean {
        return !context.isInSetupWizard() && context.isWindowMagnificationSupported()
    }

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/FollowTypingPreferenceController.java)
