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
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.accessibility.colorcorrection.data.ColorCorrectionModeDataStore
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.BooleanValuePreferenceBinding
import com.android.settingslib.widget.SelectorWithWidgetPreference

/**
 * Represents a preference for a specific color correction mode.
 *
 * This sealed class serves as a base for different color correction mode preferences (e.g.,
 * Deuteranomaly, Protanomaly). It handles the common logic for creating and managing the preference
 * widget, interacting with the data store, and managing permissions.
 *
 * @property storage The [ColorCorrectionModeDataStore] used to persist the selected mode.
 */
sealed class ModePreference(private val storage: ColorCorrectionModeDataStore) :
    BooleanValuePreference,
    BooleanValuePreferenceBinding,
    SelectorWithWidgetPreference.OnClickListener {

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context): Permissions =
        SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context): Permissions =
        SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun createWidget(context: Context): Preference =
        SelectorWithWidgetPreference(context).apply {
            // We don't want to truncate the text on the detail page,
            // since that's the only place the user can see it.
            setTitleMaxLines(4)
            setOnClickListener(this@ModePreference)
        }

    override fun onRadioButtonClicked(emiter: SelectorWithWidgetPreference) {
        emiter.isChecked = true
    }
}

class DeuteranomalyModePreference(storage: ColorCorrectionModeDataStore) : ModePreference(storage) {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.daltonizer_mode_deuteranomaly_title

    override val summary: Int
        get() = R.string.daltonizer_mode_deuteranomaly_summary

    companion object {
        private const val KEY = "daltonizer_mode_deuteranomaly"
    }
}

class ProtanomalyModePreference(storage: ColorCorrectionModeDataStore) : ModePreference(storage) {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.daltonizer_mode_protanomaly_title

    override val summary: Int
        get() = R.string.daltonizer_mode_protanomaly_summary

    companion object {
        private const val KEY = "daltonizer_mode_protanomaly"
    }
}

class TritanomalyModePreference(storage: ColorCorrectionModeDataStore) : ModePreference(storage) {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.daltonizer_mode_tritanomaly_title

    override val summary: Int
        get() = R.string.daltonizer_mode_tritanomaly_summary

    companion object {
        const val KEY = "daltonizer_mode_tritanomaly"
    }
}

class GrayscaleModePreference(storage: ColorCorrectionModeDataStore) : ModePreference(storage) {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.daltonizer_mode_grayscale_title

    companion object {
        private const val KEY = "daltonizer_mode_grayscale"
    }
}
