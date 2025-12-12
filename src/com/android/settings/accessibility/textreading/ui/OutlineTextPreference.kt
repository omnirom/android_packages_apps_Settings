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

package com.android.settings.accessibility.textreading.ui

import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.textreading.data.OutlineTextDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding

/** Preference metadata for the outline text toggle preference. */
class OutlineTextPreference(context: Context, @EntryPoint private val entryPoint: Int) :
    SwitchPreference(
        key = KEY,
        title = R.string.accessibility_toggle_maximize_text_contrast_preference_title,
        summary = R.string.accessibility_toggle_maximize_text_contrast_preference_summary,
    ),
    SwitchPreferenceBinding {

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    private val storage by lazy { OutlineTextDataStore(context, entryPoint) }

    override val keywords: Int
        get() = R.string.keywords_maximize_text_contrast

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun storage(context: Context): KeyValueStore = storage

    companion object {
        const val KEY = Settings.Secure.ACCESSIBILITY_HIGH_TEXT_CONTRAST_ENABLED
    }
}
