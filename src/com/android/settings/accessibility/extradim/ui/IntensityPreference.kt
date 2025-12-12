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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import com.android.settings.R
import com.android.settings.accessibility.extradim.data.ExtraDimDataStore
import com.android.settings.accessibility.extradim.data.IntensityDataStore
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding

class IntensityPreference(
    context: Context,
    private val extraDimStorage: ExtraDimDataStore = ExtraDimDataStore(context),
) : IntRangeValuePreference, SliderPreferenceBinding, PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.reduce_bright_colors_intensity_preference_title

    private val storage by lazy { IntensityDataStore(context) }
    private var settingsKeyedObserver: KeyedObserver<String?>? = null

    override fun createWidget(context: Context): SliderPreference =
        super.createWidget(context).apply {
            setTextStart(R.string.brightness_intensity_start_label)
            setTextEnd(R.string.brightness_intensity_end_label)
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
            updatesContinuously = true
        }

    override fun getMinValue(context: Context): Int = IntensityDataStore.MIN_VALUE

    override fun getMaxValue(context: Context): Int = IntensityDataStore.MAX_VALUE

    override fun isEnabled(context: Context): Boolean = extraDimStorage.getBoolean(KEY) == true

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context) = SettingsSecureStore.getReadPermissions()

    override fun getReadPermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int = ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSecureStore.getWritePermissions()

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int? = ReadWritePermit.ALLOW

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (settingsKeyedObserver == null) {
            settingsKeyedObserver = KeyedObserver { _, _ ->
                context.notifyPreferenceChange(bindingKey)
            }
        }
        settingsKeyedObserver?.let { observer ->
            extraDimStorage.addObserver(observer, HandlerExecutor.main)
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        settingsKeyedObserver?.let { observer -> extraDimStorage.removeObserver(observer) }
        settingsKeyedObserver = null
    }

    companion object {
        private const val KEY = IntensityDataStore.SETTING_KEY
    }
}
