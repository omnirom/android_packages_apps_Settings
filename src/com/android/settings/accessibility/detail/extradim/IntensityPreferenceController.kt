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

package com.android.settings.accessibility.detail.extradim

import android.content.Context
import android.database.ContentObserver
import android.hardware.display.ColorDisplayManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.core.SliderPreferenceController
import com.android.settingslib.widget.SliderPreference

class IntensityPreferenceController(override val context: Context, prefKey: String) :
    SliderPreferenceController(context, prefKey), ExtraDimSettingDependent {

    private val colorDisplayManager: ColorDisplayManager? =
        context.getSystemService(ColorDisplayManager::class.java)
    private var preference: SliderPreference? = null
    override val contentObserver: ContentObserver =
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ false) }) {
            override fun onChange(selfChange: Boolean) {
                preference?.let { updateState(it) }
            }
        }

    override fun getAvailabilityStatus(): Int {
        if (!ColorDisplayManager.isReduceBrightColorsAvailable(mContext)) {
            return UNSUPPORTED_ON_DEVICE
        }
        if (colorDisplayManager?.isReduceBrightColorsActivated == true) {
            return AVAILABLE
        }
        return DISABLED_DEPENDENT_SETTING
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
        preference?.apply {
            updatesContinuously = true
            setMax(this@IntensityPreferenceController.max)
            setMin(this@IntensityPreferenceController.min)
            setHapticFeedbackMode(SliderPreference.HAPTIC_FEEDBACK_MODE_ON_ENDS)
        }
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        preference?.isEnabled = colorDisplayManager?.isReduceBrightColorsActivated == true
    }

    override fun getSliderPosition(): Int {
        val settingValue =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
                /* def= */ 0,
            )

        return getMax() - settingValue
    }

    override fun setSliderPosition(position: Int): Boolean {
        return Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.REDUCE_BRIGHT_COLORS_LEVEL,
            getMax() - position,
        )
    }

    override fun getMax(): Int = 100

    override fun getMin(): Int = 0
}
