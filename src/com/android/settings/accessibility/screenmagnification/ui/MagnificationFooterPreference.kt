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

import android.content.Context
import android.content.pm.PackageManager
import android.icu.text.MessageFormat
import android.text.Html
import com.android.server.accessibility.Flags
import com.android.settings.R
import com.android.settings.accessibility.screenmagnification.OneFingerPanningPreferenceController
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceBinding
import com.android.settings.accessibility.shared.ui.AccessibilityFooterPreferenceMetadata
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceTitleProvider

// LINT.IfChange
class MagnificationFooterPreference :
    AccessibilityFooterPreferenceMetadata,
    AccessibilityFooterPreferenceBinding,
    PreferenceTitleProvider,
    PreferenceLifecycleProvider {
    private var mSettingsKeyedObserver: KeyedObserver<String>? = null

    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override val introductionTitle: Int
        get() = R.string.accessibility_screen_magnification_about_title

    override val helpResource: Int
        get() = R.string.help_url_magnification

    override val learnMoreText: Int
        get() = R.string.accessibility_screen_magnification_footer_learn_more_content_description

    override fun onStart(context: PreferenceLifecycleContext) {
        val observer = KeyedObserver<String> { _, _ -> context.notifyPreferenceChange(KEY) }
        mSettingsKeyedObserver = observer
        val storage = SettingsSecureStore.get(context)
        storage.addObserver(OneFingerPanningSwitchPreference.KEY, observer, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        mSettingsKeyedObserver?.let {
            val storage = SettingsSecureStore.get(context)
            storage.removeObserver(OneFingerPanningSwitchPreference.KEY, it)
            mSettingsKeyedObserver = null
        }
    }

    override fun getTitle(context: Context): CharSequence? {
        val hasTouch =
            context.packageManager.run {
                hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN) ||
                    hasSystemFeature(PackageManager.FEATURE_FAKETOUCH)
            }

        val hasKeyboard = InputPeripheralsSettingsUtils.isHardKeyboard()

        var summary: String
        if (hasTouch && hasKeyboard) {
            summary = getKeyboardTouchSummary(context)
        } else if (hasKeyboard) {
            summary = getKeyboardOnlySummary(context)
        } else { // hasTouch only, which is the default
            summary = getTouchOnlySummary(context)
        }

        return Html.fromHtml(
            summary,
            Html.FROM_HTML_MODE_COMPACT,
            /* imageGetter= */ null,
            /* tagHandler= */ null,
        )
    }

    private fun getTouchOnlySummary(context: Context): String {
        if (Flags.enableMagnificationOneFingerPanningGesture()) {
            val isOneFingerPanningOn =
                OneFingerPanningPreferenceController.isOneFingerPanningEnabled(context)
            return MessageFormat.format(
                context.getString(
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
                context.getString(R.string.accessibility_screen_magnification_summary),
                1,
                2,
                3,
                4,
                5,
            )
        }
    }

    private fun getKeyboardOnlySummary(context: Context): String {
        val meta: String? = context.getString(R.string.modifier_keys_meta)
        val alt: String? = context.getString(R.string.modifier_keys_alt)
        if (com.android.hardware.input.Flags.enableTalkbackAndMagnifierKeyGestures()) {
            return MessageFormat.format(
                context.getString(
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
                context.getString(
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

    private fun getKeyboardTouchSummary(context: Context): String {
        val meta: String? = context.getString(R.string.modifier_keys_meta)
        val alt: String? = context.getString(R.string.modifier_keys_alt)
        if (com.android.hardware.input.Flags.enableTalkbackAndMagnifierKeyGestures()) {
            return MessageFormat.format(
                context.getString(
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
                    context.getString(
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
            stringBuilder.append(getTouchOnlySummary(context))
            return stringBuilder.toString()
        }
    }

    companion object {
        const val KEY = "html_description"
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/screenmagnification/FooterPreferenceController.java)
