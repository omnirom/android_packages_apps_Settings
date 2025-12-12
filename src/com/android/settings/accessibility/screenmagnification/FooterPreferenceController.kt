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

package com.android.settings.accessibility.screenmagnification

import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.icu.text.MessageFormat
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityFooterPreference
import com.android.settings.accessibility.HtmlFooterPreferenceController
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils

/** PreferenceController for Screen Magnification detail page's footer */
// LINT.IfChange
class FooterPreferenceController(context: Context, prefKey: String) :
    HtmlFooterPreferenceController(context, prefKey), DefaultLifecycleObserver {
    init {
        introductionTitle =
            context.getString(R.string.accessibility_screen_magnification_about_title)
        setupHelpLink(
            R.string.help_url_magnification,
            context.getString(
                R.string.accessibility_screen_magnification_footer_learn_more_content_description
            ),
        )

        updateSummary()
    }

    private var preference: AccessibilityFooterPreference? = null
    private var contentObserver: ContentObserver =
        object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                updateState(preference)
            }
        }

    override fun onStart(owner: LifecycleOwner) {
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(OneFingerPanningPreferenceController.SETTING_KEY),
            /* notifyForDescendants= */ false,
            contentObserver,
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        mContext.contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference?) {
        (preference as? AccessibilityFooterPreference)?.let {
            updateSummary()
            updateFooterPreferences(it)
        }
    }

    private fun updateSummary() {
        val hasTouch =
            mContext.packageManager.run {
                hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ||
                    hasSystemFeature(PackageManager.FEATURE_FAKETOUCH)
            }

        val hasKeyboard = InputPeripheralsSettingsUtils.isHardKeyboard()

        if (hasTouch && hasKeyboard) {
            setSummary(getKeyboardTouchSummary())
        } else if (hasKeyboard) {
            setSummary(getKeyboardOnlySummary())
        } else { // hasTouch only, which is the default
            setSummary(getTouchOnlySummary())
        }
    }

    private fun getTouchOnlySummary(): String {
        if (Flags.enableMagnificationOneFingerPanningGesture()) {
            val isOneFingerPanningOn =
                OneFingerPanningPreferenceController.isOneFingerPanningEnabled(mContext)
            return MessageFormat.format(
                mContext.getString(
                    if (isOneFingerPanningOn)
                        R.string.accessibility_screen_magnification_summary_one_finger_panning_on
                    else R.string.accessibility_screen_magnification_summary_one_finger_panning_off
                ),
                1,
                2,
                3,
                4,
                5,
            )
        } else {
            return MessageFormat.format(
                mContext.getString(R.string.accessibility_screen_magnification_summary),
                1,
                2,
                3,
                4,
                5,
            )
        }
    }

    private fun getKeyboardOnlySummary(): String {
        val meta: String? = mContext.getString(R.string.modifier_keys_meta)
        val alt: String? = mContext.getString(R.string.modifier_keys_alt)
        if (com.android.hardware.input.Flags.enableTalkbackAndMagnifierKeyGestures()) {
            return MessageFormat.format(
                mContext.getString(
                    R.string.accessibility_screen_magnification_keyboard_summary,
                    meta,
                    alt,
                ),
                1,
                2,
                3,
                4,
            )
        } else {
            return MessageFormat.format(
                mContext.getString(
                    R.string.accessibility_screen_magnification_keyboard_shortcuts_flag_off_summary,
                    meta,
                    alt,
                ),
                1,
                2,
                3,
                4,
            )
        }
    }

    private fun getKeyboardTouchSummary(): String {
        val meta: String? = mContext.getString(R.string.modifier_keys_meta)
        val alt: String? = mContext.getString(R.string.modifier_keys_alt)
        if (com.android.hardware.input.Flags.enableTalkbackAndMagnifierKeyGestures()) {
            return MessageFormat.format(
                mContext.getString(
                    R.string.accessibility_screen_magnification_keyboard_touch_summary,
                    meta,
                    alt,
                ),
                1,
                2,
                3,
                4,
                5,
            )
        } else {
            val stringBuilder = StringBuilder()
            stringBuilder.append(
                MessageFormat.format(
                    mContext.getString(
                        R.string
                            .accessibility_screen_magnification_keyboard_shortcuts_flag_off_summary,
                        meta,
                        alt,
                    ),
                    1,
                    2,
                    3,
                    4,
                )
            )

            stringBuilder.append("<br/><br/>")
            stringBuilder.append(getTouchOnlySummary())
            return stringBuilder.toString()
        }
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/ui/MagnificationFooterPreference.kt)
