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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import com.android.settings.R
import com.android.settings.core.SubSettingLauncher
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider

class DaltonizerPreference : PreferenceMetadata, PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = PREFERENCE_KEY

    override val title: Int
        get() = com.android.settingslib.R.string.accessibility_display_daltonizer_preference_title

    override val icon: Int
        get() = R.drawable.ic_daltonizer

    override val keywords: Int
        get() = R.string.keywords_color_correction

    private var mSettingsKeyedObserver: KeyedObserver<String>? = null

    override fun intent(context: Context) =
        SubSettingLauncher(context)
            .setDestination(ToggleDaltonizerPreferenceFragment::class.java.name)
            .setSourceMetricsCategory(SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION)
            .toIntent()

    override fun getSummary(context: Context): CharSequence? {
        return AccessibilityUtil.getSummary(
            context,
            SETTING_KEY,
            R.string.daltonizer_state_on, R.string.daltonizer_state_off
        )
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        val observer =
            KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(PREFERENCE_KEY) }
        mSettingsKeyedObserver = observer
        val storage = SettingsSecureStore.get(context)
        storage.addObserver(SETTING_KEY, observer, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        mSettingsKeyedObserver?.let {
            val storage = SettingsSecureStore.get(context)
            storage.removeObserver(SETTING_KEY, it)
            mSettingsKeyedObserver = null
        }
    }

    companion object {
        const val PREFERENCE_KEY = "daltonizer_preference"
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED
    }
}