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

package com.android.settings.accessibility.shared.utils

import android.view.ViewGroup
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.widget.SettingsThemeHelper

// TODO(b/407080818): Delete this function when we no longer need to adjust the layout
/**
 * Adjusts the layout of the given [LottieAnimationView] for the Setup Wizard.
 *
 * This function is a workaround for an issue where `IllustrationPreference` changes the
 * illustrationFrame's width to the shortest device side, which can cause the image to be cut off
 * when displayed in the Setup Wizard. This function sets the width of the illustrationFrame to
 * `MATCH_PARENT` to resolve this issue.
 */
fun adjustIllustrationLayoutForSetupWizard(view: LottieAnimationView) {
    // IllustrationPreference changes the illustrationFrame's width to the shortest
    // device side.
    // This potentially breaks the image when shown in SetupWizard. Sets the width to
    // MATCH_PARENT solves the image cutoff issue on SUW
    if (SettingsThemeHelper.isExpressiveTheme(view.context) && view.context.isInSetupWizard()) {
        if (view.parent is ViewGroup) {
            val illustrationFrame = view.parent as ViewGroup
            val lp: ViewGroup.LayoutParams = illustrationFrame.layoutParams
            lp.width = ViewGroup.LayoutParams.MATCH_PARENT
            illustrationFrame.layoutParams = lp
        }
    }
}
