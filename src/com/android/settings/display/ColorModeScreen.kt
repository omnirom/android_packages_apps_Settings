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

package com.android.settings.display

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.provider.Settings.System.DISPLAY_COLOR_MODE
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ColorModeActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.ColorModeUtils.getActiveColorModeName
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ColorModeScreen.KEY)
open class ColorModeScreen :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.color_mode_title

    override val keywords
        get() = R.string.keywords_color_mode

    override fun isFlagEnabled(context: Context) = Flags.deeplinkDisplayAndTouch25q4()

    override fun getMetricsCategory() = SettingsEnums.COLOR_MODE_SETTINGS

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = ColorModePreferenceFragment::class.java

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    private var settingsKeyedObserver: KeyedObserver<String>? = null

    override fun isAvailable(context: Context): Boolean {
        val colorManager = context.getSystemService(ColorDisplayManager::class.java) ?: return false
        return colorManager.isDeviceColorManaged &&
            !ColorDisplayManager.areAccessibilityTransformsEnabled(context)
    }

    override fun getSummary(context: Context): CharSequence? = getActiveColorModeName(context)

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ColorModeActivity::class.java, metadata?.key)

    override fun onStart(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
            settingsKeyedObserver = observer
            val storage = SettingsSystemStore.get(context)
            storage.addObserver(DISPLAY_COLOR_MODE, observer, HandlerExecutor.main)
        }
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            settingsKeyedObserver?.let {
                val storage = SettingsSystemStore.get(context)
                storage.removeObserver(DISPLAY_COLOR_MODE, it)
                settingsKeyedObserver = null
            }
        }
    }

    companion object {
        const val KEY = "color_mode"
    }
}
// LINT.ThenChange(ColorModePreferenceFragment.java, ColorModePreferenceController.java)
