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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.view.ViewGroup
import androidx.preference.PreferenceScreen
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.R
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.core.BasePreferenceController
import com.android.settingslib.utils.ThreadUtils
import com.android.settingslib.widget.IllustrationPreference
import com.android.settingslib.widget.SettingsThemeHelper

// LINT.IfChange
/** BasePreferenceController for [IllustrationPreference] */
open class IllustrationPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey) {

    private var imageUri: Uri? = null
    private var contentDescription: CharSequence? = null

    /** Initialize the image and content description of the image */
    open fun initialize(imageUri: Uri?, contentDescription: CharSequence?) {
        this.imageUri = imageUri
        this.contentDescription = contentDescription
    }

    override fun getAvailabilityStatus(): Int =
        if (imageUri != null) AVAILABLE_UNSEARCHABLE else CONDITIONALLY_UNAVAILABLE

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        val illustrationPref: IllustrationPreference? = screen?.findPreference(preferenceKey)
        if (illustrationPref == null || imageUri == null) return

        val displayHalfHeight =
            AccessibilityUtil.getDisplayBounds(illustrationPref.context).height() / 2
        illustrationPref.apply {
            imageUri = this@IllustrationPreferenceController.imageUri
            isSelectable = false
            setMaxHeight(displayHalfHeight)
            setOnBindListener { view: LottieAnimationView? ->
                // IllustrationPreference changes the illustrationFrame's width to the shortest
                // device side.
                // This potentially breaks the image when shown in SetupWizard. Sets the width to
                // MATCH_PARENT solves the image cutoff issue on SUW
                if (SettingsThemeHelper.isExpressiveTheme(mContext) && mContext.isInSetupWizard()) {
                    if (view?.parent is ViewGroup) {
                        val illustrationFrame = view.parent as ViewGroup
                        val lp: ViewGroup.LayoutParams = illustrationFrame.layoutParams
                        lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                        illustrationFrame.layoutParams = lp
                    }
                }

                // isAnimatable is decided in
                // [IllustrationPreference#onBindViewHolder(PreferenceViewHolder)]. Therefore, we
                // wait until the view is bond to set the content description for it.
                // The content description is added for an animation illustration only.
                // Since the static images are decorative.
                ThreadUtils.getUiThreadHandler()
                    .post(
                        Runnable {
                            if (this.isAnimatable) {
                                this.contentDescription =
                                    this@IllustrationPreferenceController.contentDescription
                            }
                        }
                    )
            }
        }
    }
}

// LINT.ThenChange(shared/ui/ImageUriPreference.kt)

class ColorInversionIllustrationPreferenceController(context: Context, prefKey: String) :
    IllustrationPreferenceController(context, prefKey) {
    init {
        val imageUri =
            Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.packageName)
                .appendPath(java.lang.String.valueOf(R.raw.accessibility_color_inversion_banner))
                .build()
        val contentDescription =
            context.getString(
                R.string.accessibility_illustration_content_description,
                context.getText(R.string.accessibility_display_inversion_preference_title),
            )
        initialize(imageUri, contentDescription)
    }
}
