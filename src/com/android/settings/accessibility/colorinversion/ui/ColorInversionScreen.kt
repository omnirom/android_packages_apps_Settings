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

package com.android.settings.accessibility.colorinversion.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Settings.ACTION_COLOR_INVERSION_SETTINGS
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.ToggleColorInversionPreferenceFragment
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.highlightPreference
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ColorInversionScreen.KEY)
open class ColorInversionScreen :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceLifecycleProvider {

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_display_inversion_preference_title

    override val icon: Int
        get() = R.drawable.ic_color_inversion

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.keywords_color_inversion

    private var settingsKeyedObserver: KeyedObserver<String>? = null

    override fun fragmentClass(): Class<out Fragment>? =
        ToggleColorInversionPreferenceFragment::class.java

    override fun isFlagEnabled(context: Context) = Flags.catalystColorInversion()

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_COLOR_INVERSION_SETTINGS

    override fun getSummary(context: Context): CharSequence? =
        AccessibilityUtil.getSummary(
            context,
            SETTING_KEY,
            R.string.color_inversion_state_on,
            R.string.color_inversion_state_off,
        )

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            val observer =
                KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(bindingKey) }
            val storage = SettingsSecureStore.get(context)
            storage.addObserver(SETTING_KEY, observer, HandlerExecutor.main)
            settingsKeyedObserver = observer
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            settingsKeyedObserver?.let {
                val storage = SettingsSecureStore.get(context)
                storage.removeObserver(SETTING_KEY, it)
                settingsKeyedObserver = null
            }
        }
    }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_COLOR_INVERSION_SETTINGS).apply { highlightPreference(metadata?.key) }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +IntroPreference()
            +ColorInversionIllustrationPreference()
            +ColorInversionMainSwitchPreference(context)
            +PreferenceCategory(
                key = "general_categories",
                title = R.string.accessibility_screen_option,
            ) +=
                {
                    +AccessibilityShortcutPreference(
                        context = context,
                        key = "color_inversion_shortcut_key",
                        title = R.string.accessibility_display_inversion_shortcut_title,
                        componentName = COLOR_INVERSION_COMPONENT_NAME,
                        featureName = R.string.accessibility_display_inversion_preference_title,
                        metricsCategory = metricsCategory,
                    )
                }
            +FooterPreference()
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
        }

    companion object {
        const val KEY = "toggle_inversion_preference"
        const val SETTING_KEY = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED
    }
}
