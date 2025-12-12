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
import android.content.ComponentName
import android.content.Context
import android.text.TextUtils
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.internal.accessibility.common.NotificationConstants.EXTRA_SOURCE
import com.android.internal.accessibility.common.NotificationConstants.SOURCE_START_SURVEY
import com.android.settings.R
import com.android.settings.accessibility.BaseSupportFragment
import com.android.settings.accessibility.FeedbackButtonPreferenceController
import com.android.settings.accessibility.FeedbackManager
import com.android.settings.accessibility.Flags
import com.android.settings.accessibility.MagnificationSurveyButtonPreferenceController
import com.android.settings.accessibility.SurveyManager
import com.android.settings.accessibility.screenmagnification.CursorFollowingModePreferenceController
import com.android.settings.accessibility.screenmagnification.ModePreferenceController
import com.android.settings.accessibility.screenmagnification.ToggleMagnificationShortcutPreferenceController
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.search.SearchIndexable

/** Displays the detail screen of the screen magnification feature */
@SearchIndexable(forTarget = SearchIndexable.ALL and SearchIndexable.ARC.inv())
open class MagnificationPreferenceFragment : BaseSupportFragment() {

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (Flags.catalystMagnification()) {
            return
        }

        use(ModePreferenceController::class.java)?.setFragmentManager(getChildFragmentManager())
        use(CursorFollowingModePreferenceController::class.java)
            ?.setFragmentManager(getChildFragmentManager())
        use(FeedbackButtonPreferenceController::class.java)
            .initialize(FeedbackManager(context, metricsCategory))
        initMagnificationSurvey(context)
        getShortcutPreferenceController()?.apply {
            initialize(
                getFeatureComponentName(),
                childFragmentManager,
                getFeatureName(),
                metricsCategory,
            )
        }
    }

    fun getShortcutPreferenceController(): ToggleMagnificationShortcutPreferenceController? {
        return if (Flags.catalystMagnification()) {
            null
        } else {
            use(ToggleMagnificationShortcutPreferenceController::class.java)
        }
    }

    private fun initMagnificationSurvey(context: Context) {
        val surveyManager = SurveyManager(this, context, MAGNIFICATION_SURVEY_KEY, metricsCategory)
        val intent = getIntent()
        if (
            intent != null &&
                intent.getStringExtra(EXTRA_SOURCE) != null &&
                TextUtils.equals(intent.getStringExtra(EXTRA_SOURCE), SOURCE_START_SURVEY)
        ) {
            surveyManager.startSurvey()
        } else {
            use(MagnificationSurveyButtonPreferenceController::class.java).initialize(surveyManager)
        }
    }

    private fun getFeatureName(): CharSequence {
        return getText(R.string.accessibility_screen_magnification_title)
    }

    private fun getFeatureComponentName(): ComponentName {
        return AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.accessibility_magnification_screen
    }

    override fun getLogTag(): String? = TAG

    override fun getMetricsCategory(): Int {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION
    }

    override fun getHelpResource(): Int {
        return R.string.help_url_magnification
    }

    override fun getPreferenceScreenBindingKey(context: Context): String? = MagnificationScreen.KEY

    companion object {
        private val TAG = MagnificationPreferenceFragment::class.simpleName
        const val MAGNIFICATION_SURVEY_KEY: String = "A11yMagnificationUser"
        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            BaseSearchIndexProvider(
                if (Flags.catalystMagnification()) 0 else R.xml.accessibility_magnification_screen
            )
    }
}
