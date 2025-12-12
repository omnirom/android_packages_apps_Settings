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

package com.android.settings.accessibility

import android.app.settings.SettingsEnums
import android.content.Context
import android.hardware.display.ColorDisplayManager
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.ColorAndMotionActivity
import com.android.settings.accessibility.colorcorrection.ui.ColorCorrectionScreen
import com.android.settings.accessibility.colorinversion.ui.ColorInversionScreen
import com.android.settings.accessibility.shared.ui.FeedbackButtonPreference
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.darkmode.DarkModeScreenOnAccessibility
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(ColorAndMotionScreen.KEY)
open class ColorAndMotionScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.accessibility_color_and_motion_title

    override val summary: Int
        get() = R.string.accessibility_color_and_motion_subtext

    override val icon: Int
        get() = R.drawable.ic_color_and_motion

    override val indexable
        get() = true

    override fun getMetricsCategory() = SettingsEnums.ACCESSIBILITY_COLOR_AND_MOTION

    override val highlightMenuKey
        get() = R.string.menu_key_accessibility

    override fun fragmentClass(): Class<out Fragment>? = ColorAndMotionFragment::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // LINT.IfChange(ui_hierarchy)
            if (ColorDisplayManager.isColorTransformAccelerated(context)) {
                +ColorCorrectionScreen.KEY
                +ColorInversionScreen.KEY
                +DarkModeScreenOnAccessibility.KEY
                +BlurSwitchPreference()
                +RemoveAnimationsPreference()
            } else {
                +ColorInversionScreen.KEY
                +DarkModeScreenOnAccessibility.KEY
                +BlurSwitchPreference()
                +PreferenceCategory(
                    "experimental_category",
                    R.string.experimental_category_title,
                ) +=
                    {
                        +ColorCorrectionScreen.KEY
                        +RemoveAnimationsPreference()
                    }
            }
            +FeedbackButtonPreference { FeedbackManager(context, metricsCategory) }
            // LINT.ThenChange(/res/xml/accessibility_color_and_motion.xml)
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, ColorAndMotionActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "accessibility_color_and_motion"
    }
}
