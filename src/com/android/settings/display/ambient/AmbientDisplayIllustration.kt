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
package com.android.settings.display.ambient

import android.content.Context
import android.provider.Settings.Secure.DOZE_ALWAYS_ON
import android.provider.Settings.Secure.DOZE_ALWAYS_ON_WALLPAPER_ENABLED
import androidx.preference.Preference
import com.android.internal.R.bool.config_dozeSupportsAodWallpaper
import com.android.settings.R
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.IllustrationPreference
import com.android.systemui.shared.Flags.ambientAod

class AmbientDisplayIllustration(context: Context) : PreferenceMetadata, PreferenceBinding {

    private lateinit var illustration: IllustrationPreference
    private val storage = SettingsSecureStore.get(context)
    private val dozeAlwaysOnDataStore = AmbientDisplayStorage(context)

    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override fun createWidget(context: Context) = IllustrationPreference(context)

    override fun dependencies(context: Context) =
        arrayOf(AmbientWallpaperPreference.KEY, AmbientDisplayMainSwitchPreference.KEY)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        illustration = preference as IllustrationPreference
        preference.isSelectable = false
        updateImage(preference.context)
    }

    private fun updateImage(context: Context) {
        val isWallpaperEnabled = storage.getBoolean(DOZE_ALWAYS_ON_WALLPAPER_ENABLED) == true
        val isAmbientDisplayEnabled = dozeAlwaysOnDataStore.getBoolean(DOZE_ALWAYS_ON)!!
        val drawable =
            when {
                isWallpaperEnabled &&
                    isAmbientDisplayEnabled &&
                    context.isAmbientWallpaperOptionsAvailable ->
                    R.drawable.ambient_display_with_wallpaper
                isAmbientDisplayEnabled -> R.drawable.ambient_display_no_wallpaper
                else -> R.drawable.ambient_display_off
            }
        illustration.imageDrawable = context.getDrawable(drawable)
    }

    companion object {
        const val KEY = "ambient_display_illustration"

        private val Context.isAmbientWallpaperOptionsAvailable: Boolean
            get() = ambientAod() && resources.getBoolean(config_dozeSupportsAodWallpaper)
    }
}
