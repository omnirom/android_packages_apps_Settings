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

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.inflateViewHolder
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.widget.IllustrationPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
class ImageUriPreferenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun isAvailable_nullImageUri_returnFalse() {
        val preferenceMetadata = NullImageUriPreference()
        assertThat(preferenceMetadata.isAvailable(context)).isFalse()
    }

    @Test
    fun isAvailable_hasImageUri_returnTrue() {
        val preferenceMetadata = AnimatedImageUriPreference()
        assertThat(preferenceMetadata.isAvailable(context)).isTrue()
    }

    @Test
    fun isIndexable_returnFalse() {
        val preferenceMetadata = AnimatedImageUriPreference()
        assertThat(preferenceMetadata.indexable).isFalse()
    }

    @Test
    fun getKey() {
        val preferenceMetadata = AnimatedImageUriPreference()
        assertThat(preferenceMetadata.key).isEqualTo(ImageUriPreference.KEY)
    }

    @Test
    fun createWidget_hasAnimatedImage_setsContentDescription() {
        val preferenceMetadata = AnimatedImageUriPreference()
        val preferenceWidget =
            preferenceMetadata.createAndBindWidget<IllustrationPreference>(context)
        preferenceWidget.inflateViewHolder()
        ReflectionHelpers.setField(preferenceWidget, "mIsAnimatable", true)
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(preferenceWidget.contentDescription.toString())
            .isEqualTo(preferenceMetadata.getContentDescription(context).toString())
    }

    @Test
    fun createWidget_hasStaticImage_dontSetContentDescription() {
        val preferenceMetadata = StaticImageUriPreference()
        val preferenceWidget =
            preferenceMetadata.createAndBindWidget<IllustrationPreference>(context)
        preferenceWidget.inflateViewHolder()
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()

        assertThat(preferenceWidget.contentDescription).isNull()
    }

    @Test
    fun createWidget_hasNullImage_widgetIsInvisible() {
        val preferenceMetadata = NullImageUriPreference()
        val preferenceWidget =
            preferenceMetadata.createAndBindWidget<IllustrationPreference>(context)

        assertThat(preferenceWidget.isVisible).isFalse()
    }
}

private class AnimatedImageUriPreference() : ImageUriPreference() {
    override fun getImageUri(context: Context): Uri? {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(java.lang.String.valueOf(R.raw.accessibility_color_inversion_banner))
            .build()
    }

    override fun getContentDescription(context: Context): CharSequence? {
        return context.getString(
            R.string.accessibility_illustration_content_description,
            context.getText(R.string.accessibility_display_inversion_preference_title),
        )
    }
}

private class StaticImageUriPreference() : ImageUriPreference() {
    override fun getImageUri(context: Context): Uri? {
        return Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(context.packageName)
            .appendPath(java.lang.String.valueOf(R.drawable.flash_notifications_illustration))
            .build()
    }

    override fun getContentDescription(context: Context): CharSequence? {
        return "static illustration"
    }
}

private class NullImageUriPreference() : ImageUriPreference() {
    override fun getImageUri(context: Context): Uri? {
        return null
    }

    override fun getContentDescription(context: Context): CharSequence? {
        return null
    }
}
