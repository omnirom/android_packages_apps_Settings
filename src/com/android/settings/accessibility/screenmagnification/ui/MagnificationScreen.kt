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

import android.app.settings.SettingsEnums
import android.content.Context
import android.content.Intent
import androidx.fragment.app.Fragment
import com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
import com.android.settings.R
import com.android.settings.Settings.MagnificationActivity
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.shared.ui.AccessibilityShortcutPreference
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(MagnificationScreen.KEY)
open class MagnificationScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val indexable
        get() = true

    override val keywords: Int
        get() = R.string.keywords_magnification

    override val title: Int
        get() = R.string.accessibility_screen_magnification_title

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override val icon: Int
        get() = R.drawable.ic_accessibility_magnification

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION

    override fun isFlagEnabled(context: Context) = Flags.catalystMagnification()

    override fun hasCompleteHierarchy() = Flags.catalystMagnification()

    override fun fragmentClass(): Class<out Fragment>? = MagnificationPreferenceFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?): Intent? =
        makeLaunchIntent(context, MagnificationActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +MagnificationTopIntroPreference()
            +MagnificationIllustrationPreference()
            +PreferenceCategory("general_categories", R.string.accessibility_screen_option) += {
                +AccessibilityShortcutPreference(
                    context = context,
                    key = "magnification_shortcut_preference",
                    title = R.string.accessibility_screen_magnification_shortcut_title,
                    componentName = MAGNIFICATION_COMPONENT_NAME,
                    featureName = R.string.accessibility_screen_magnification_title,
                    metricsCategory = metricsCategory,
                )
                +MagnificationModePreference()
                +MagnifyKeyboardSwitchPreference()
                +FollowTypingSwitchPreference()
                +FollowKeyboardSwitchPreference()
                +CursorFollowingPreference()
                +OneFingerPanningSwitchPreference()
                +AlwaysOnSwitchPreference()
                +JoystickSwitchPreference()
            }
            +MagnificationFooterPreference()
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
            +MagnificationSurveyButtonPreference(metricsCategory)
        }

    companion object {
        const val KEY = "magnification_preference_screen"
    }
}
