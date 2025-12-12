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
package com.android.settings.notification

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.R
import com.android.settings.Settings.SoundSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.sound.MediaControlsScreen
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.metadata.PreferenceCategory
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.widget.SettingsThemeHelper.isExpressiveTheme
import com.android.settingslib.widget.UntitledPreferenceCategoryMetadata
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(SoundScreen.KEY)
open class SoundScreen : PreferenceScreenMixin, PreferenceIconProvider {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.sound_settings

    override val keywords: Int
        get() = R.string.keywords_sounds

    override fun getIcon(context: Context) =
        when {
            isExpressiveTheme(context) -> R.drawable.ic_homepage_sound
            else -> R.drawable.ic_volume_up_filled
        }

    override val highlightMenuKey: Int
        get() = R.string.menu_key_sound

    override fun getMetricsCategory() = SettingsEnums.SOUND

    override fun isFlagEnabled(context: Context): Boolean = Flags.catalystSoundScreen()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = SoundSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            val audioHelper = AudioHelper(context)
            +UntitledPreferenceCategoryMetadata(VOLUME_CONTROLS_CATEGORY) order -160 += {
                +MediaVolumePreference(audioHelper) order -180
                +CallVolumePreference(audioHelper) order -170
                +SeparateRingVolumePreference(audioHelper) order -155
                if (Flags.catalystSoundScreen25q3()) {
                    +NotificationVolumePreference(audioHelper) order -150
                    +AlarmVolumePreference(audioHelper) order -140
                }
            }
            +PreferenceCategory(AUDIO_CATEGORY, R.string.sound_audio_category_title) order -120 += {
                +MediaControlsScreen.KEY order -100
            }
            +PreferenceCategory(
                SOUNDS_AND_VIBRATIONS_CATEGORY,
                R.string.system_sounds_and_vibrations_category_title,
            ) order -111 +=
                {
                    +DialPadTonePreference() order -50
                    // TODO(b/432624175): rollback to Flags.catalystSoundScreen25q4
                    if (Flags.deeplinkSoundAndVibration25q4()) {
                        +ScreenLockSoundPreference() order -45
                        +ChargingSoundPreference() order -40
                        +DockingSoundPreference() order -35
                        +TouchSoundPreference(context) order -30
                    }
                }
        }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, SoundSettingsActivity::class.java, metadata?.key)

    companion object {
        const val KEY = "sound_screen"
        internal const val VOLUME_CONTROLS_CATEGORY = "volume_controls_category"
        internal const val AUDIO_CATEGORY = "audio_category"
        internal const val SOUNDS_AND_VIBRATIONS_CATEGORY = "system_sounds_and_vibrations_category"
    }
}
