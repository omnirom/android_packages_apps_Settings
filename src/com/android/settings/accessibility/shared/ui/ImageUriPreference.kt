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

package com.android.settings.accessibility.shared.ui

import android.content.Context
import android.net.Uri
import androidx.preference.Preference
import com.airbnb.lottie.LottieAnimationView
import com.android.settings.accessibility.AccessibilityUtil
import com.android.settings.accessibility.shared.utils.adjustIllustrationLayoutForSetupWizard
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.utils.ThreadUtils
import com.android.settingslib.widget.IllustrationPreference

/**
 * PreferenceMetadata for showing image uri illustration with the [IllustrationPreference] widget.
 * This is useful for when we don't know the content of the image (for example, images provided by
 * other apps)
 */
abstract class ImageUriPreference :
    PreferenceMetadata, PreferenceBinding, PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    override val indexable
        get() = false

    abstract fun getImageUri(context: Context): Uri?

    override fun isAvailable(context: Context): Boolean = getImageUri(context) != null

    abstract fun getContentDescription(context: Context): CharSequence?

    override fun createWidget(context: Context): Preference {
        return IllustrationPreference(context).apply {
            isSelectable = false
            imageUri = getImageUri(context)
            val displayHalfHeight = AccessibilityUtil.getDisplayBounds(context).height() / 2
            setMaxHeight(displayHalfHeight)
            setOnBindListener { view: LottieAnimationView? ->
                if (view == null) return@setOnBindListener
                adjustIllustrationLayoutForSetupWizard(view)

                // isAnimatable is decided in
                // [IllustrationPreference#onBindViewHolder(PreferenceViewHolder)]. Therefore, we
                // wait until the view is bond to set the content description for it.
                // The content description is added for an animation illustration only.
                // Since the static images are decorative.
                ThreadUtils.getUiThreadHandler().post {
                    if (this.isAnimatable) {
                        this.contentDescription = getContentDescription(context)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY = "animated_image"
    }
}
