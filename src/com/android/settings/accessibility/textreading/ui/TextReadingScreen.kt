/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TextReadingPreferenceFragmentForSetupWizard
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

abstract class BaseTextReadingScreen : PreferenceScreenMixin {
    @EntryPoint abstract val entryPoint: Int
    override val title: Int
        get() = R.string.accessibility_text_reading_options_title

    override val summary: Int
        get() = R.string.accessibility_text_reading_options_subtext

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS

    override val highlightMenuKey
        get() = R.string.menu_key_display

    // There are multi-entrypoint to this screen. We only want the [TextReadingScreen] searchable to
    // prevent showing duplicate entries in the search results.
    override val indexable
        get() = false

    override fun isFlagEnabled(context: Context) = Flags.catalystTextReadingScreen()

    override fun fragmentClass(): Class<out Fragment>? = TextReadingPreferenceFragment::class.java

    // LINT.IfChange(ui_hierarchy)
    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val fontSizePreference = FontSizePreference(context, entryPoint)
            val displaySizePreference = DisplaySizePreference(context, entryPoint)
            +TextReadingPreview(
                displaySizeProvider = { displaySizePreference.displaySizePreview },
                fontSizeProvider = { fontSizePreference.fontSizePreview },
            )

            +PreferenceCategory(
                key = "display_text_size",
                title = R.string.category_title_display_text_size,
            ) +=
                {
                    +fontSizePreference
                    +displaySizePreference
                }
            +PreferenceCategory(key = "text_style", title = R.string.category_title_text_style) += {
                +BoldTextPreference(context, entryPoint)
                +OutlineTextPreference(context, entryPoint)
            }
            +ResetPreference(entryPoint)
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
        }
    // LINT.ThenChange()
}

@ProvidePreferenceScreen(TextReadingScreen.KEY)
open class TextReadingScreen : BaseTextReadingScreen() {
    override val entryPoint: Int
        get() = EntryPoint.DISPLAY_SETTINGS

    override val key: String = KEY

    override val indexable
        get() = true

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? {
        return makeLaunchIntent(
            context,
            Settings.TextReadingSettingsActivity::class.java,
            metadata?.key,
        )
    }

    companion object {
        const val KEY = "text_reading_options"
    }
}

@ProvidePreferenceScreen(TextReadingScreenOnAccessibility.KEY)
open class TextReadingScreenOnAccessibility : BaseTextReadingScreen() {
    override val entryPoint: Int
        get() = EntryPoint.ACCESSIBILITY_SETTINGS

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String = KEY

    override val icon: Int
        get() = R.drawable.ic_adaptive_font_download

    companion object {
        const val KEY = "text_reading_options_in_a11y"
    }
}

// TODO(b/407080818): Remove this catalyst screen once we decouple SUW and Settings
@ProvidePreferenceScreen(TextReadingScreenInSuw.KEY)
open class TextReadingScreenInSuw : BaseTextReadingScreen() {
    override val entryPoint: Int
        get() = EntryPoint.SUW_VISION_SETTINGS

    override val highlightMenuKey: Int
        get() = R.string.menu_key_accessibility

    override val key: String = KEY

    override val icon: Int
        get() = R.drawable.ic_adaptive_font_download

    override fun getMetricsCategory(): Int = SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS

    override fun fragmentClass(): Class<out Fragment>? {
        return TextReadingPreferenceFragmentForSetupWizard::class.java
    }

    companion object {
        const val KEY = "text_reading_options_in_suw"
    }
}

// TODO(b/407080818): Remove this catalyst screen once we decouple SUW and Settings
@ProvidePreferenceScreen(TextReadingScreenInAnythingElse.KEY)
open class TextReadingScreenInAnythingElse : BaseTextReadingScreen() {
    override val entryPoint: Int
        get() = EntryPoint.SUW_ANYTHING_ELSE

    override val key: String = KEY

    override val icon: Int
        get() = R.drawable.ic_font_download

    override val title: Int
        get() = R.string.accessibility_text_reading_options_suggestion_title

    override fun getMetricsCategory(): Int = SettingsEnums.SUW_ACCESSIBILITY_TEXT_READING_OPTIONS

    override fun fragmentClass(): Class<out Fragment>? {
        return TextReadingPreferenceFragmentForSetupWizard::class.java
    }

    companion object {
        const val KEY = "text_reading_options_in_anything_else"
    }
}

@ProvidePreferenceScreen(TextReadingScreenFromNotification.KEY)
open class TextReadingScreenFromNotification : BaseTextReadingScreen() {
    override val entryPoint: Int
        get() = EntryPoint.HIGH_CONTRAST_TEXT_NOTIFICATION

    override val key: String = KEY

    companion object {
        const val KEY = "text_reading_options_in_outline_text_notification"
    }
}
