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

package com.android.settings.accessibility.colorcorrection.ui

import android.content.Context
import android.provider.Settings
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.shared.data.ToggleFeatureDataStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.widget.MainSwitchPreferenceBinding

class ColorCorrectionMainSwitchPreference(context: Context) :
    SwitchPreference(key = KEY, title = R.string.accessibility_daltonizer_primary_switch_title),
    MainSwitchPreferenceBinding {

    private val storage by lazy {
        ToggleFeatureDataStore(
            AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME,
            SettingsSecureStore.get(context),
        )
    }

    override fun storage(context: Context) = storage

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED
    }
}
