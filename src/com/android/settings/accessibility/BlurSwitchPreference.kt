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

import android.content.Context
import android.provider.Settings
import android.os.PowerManager
import android.view.CrossWindowBlurListeners.CROSS_WINDOW_BLUR_SUPPORTED

import com.android.settings.R
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.SettingsGlobalStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.SwitchPreference
import com.android.systemui.Flags

class BlurSwitchPreference :
    SwitchPreference(key = KEY, title = R.string.blur_switch),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    override fun isAvailable(context: Context) =
        CROSS_WINDOW_BLUR_SUPPORTED && Flags.blurSettingsToggle()

    override fun isEnabled(context: Context) = !context.isPowerSaveMode()

    override val icon: Int
        get() = R.drawable.ic_blur

    override val keywords: Int
        get() = R.string.keywords_blur_switch

    override fun storage(context: Context): KeyValueStore = SettingsGlobalStore.get(context)

    override fun getSummary(context: Context): CharSequence? {
        return context.getString(
            if (!isEnabled(context)) R.string.blur_switch_disabled_summary
            else R.string.blur_switch_summary
        )
    }

    companion object {
        const val KEY = Settings.Global.DISABLE_WINDOW_BLURS

        private fun Context.isPowerSaveMode() =
            getSystemService(PowerManager::class.java)?.isPowerSaveMode == true

    }
}
