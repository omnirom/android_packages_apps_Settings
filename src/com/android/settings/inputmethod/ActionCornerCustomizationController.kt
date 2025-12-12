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

package com.android.settings.inputmethod

import android.app.ActivityManager
import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_HOME
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_LOCKSCREEN
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NONE
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_NOTIFICATIONS
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_OVERVIEW
import android.provider.Settings.Secure.ACTION_CORNER_ACTION_QUICK_SETTINGS
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_LEFT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_BOTTOM_RIGHT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_TOP_LEFT_ACTION
import android.provider.Settings.Secure.ACTION_CORNER_TOP_RIGHT_ACTION
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.settings.flags.Flags.actionCornerCustomization
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isMouse
import com.android.settings.inputmethod.InputPeripheralsSettingsUtils.isTouchpad
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider

/** The controller that handles the customization of each action corner. */
class ActionCornerCustomizationController(context: Context, preferenceKey: String) :
    BasePreferenceController(context, preferenceKey), Preference.OnPreferenceChangeListener {
    private lateinit var listPreference: ListPreference

    private val corner: Corner = prefKeyToCorner[preferenceKey]!!

    private val metricsFeatureProvider: MetricsFeatureProvider =
        FeatureFactory.featureFactory.metricsFeatureProvider

    override fun getAvailabilityStatus(): Int {
        return if (actionCornerCustomization() && (isTouchpad() || isMouse())) AVAILABLE
        else CONDITIONALLY_UNAVAILABLE
    }

    override fun displayPreference(screen: PreferenceScreen) {
        super.displayPreference(screen)
        listPreference = screen.findPreference(preferenceKey)!!
        val cornerName = mContext.getString(corner.nameId)
        listPreference.dialogTitle =
            mContext.getString(R.string.action_corner_action_dialog_title, cornerName)
        listPreference.entryValues = entryValues
    }

    override fun updateState(preference: Preference?) {
        updateListPreference()
    }

    private fun updateListPreference() {
        listPreference.value = getCurrentAction()
        listPreference.summary = summary
    }

    private fun getCurrentAction(): String {
        val current =
            Settings.Secure.getIntForUser(
                mContext.contentResolver,
                corner.target,
                ACTION_CORNER_ACTION_NONE,
                ActivityManager.getCurrentUser(),
            )
        return current.toString()
    }

    override fun getSummary(): CharSequence? = listPreference.entry

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        val action = newValue.toString().toInt()
        Settings.Secure.putIntForUser(
            mContext.contentResolver,
            corner.target,
            action,
            ActivityManager.getCurrentUser(),
        )
        metricsFeatureProvider.action(mContext, corner.metrics, action)
        updateListPreference()
        return true
    }

    companion object {
        val prefKeyToCorner =
            mapOf(
                "action_corner_bottom_left" to Corner.BOTTOM_LEFT,
                "action_corner_bottom_right" to Corner.BOTTOM_RIGHT,
                "action_corner_top_left" to Corner.TOP_LEFT,
                "action_corner_top_right" to Corner.TOP_RIGHT,
            )

        val entryValues =
            arrayOf<CharSequence>(
                ACTION_CORNER_ACTION_NONE.toString(),
                ACTION_CORNER_ACTION_HOME.toString(),
                ACTION_CORNER_ACTION_OVERVIEW.toString(),
                ACTION_CORNER_ACTION_NOTIFICATIONS.toString(),
                ACTION_CORNER_ACTION_QUICK_SETTINGS.toString(),
                ACTION_CORNER_ACTION_LOCKSCREEN.toString(),
            )
    }

    enum class Corner(val nameId: Int, val target: String, val metrics: Int) {
        BOTTOM_LEFT(
            nameId = R.string.action_corner_bottom_left_name,
            target = ACTION_CORNER_BOTTOM_LEFT_ACTION,
            metrics = SettingsEnums.ACTION_BOTTOM_LEFT_ACTION_CORNER_SHORTCUT_CHANGED,
        ),
        BOTTOM_RIGHT(
            nameId = R.string.action_corner_bottom_right_name,
            target = ACTION_CORNER_BOTTOM_RIGHT_ACTION,
            metrics = SettingsEnums.ACTION_BOTTOM_RIGHT_ACTION_CORNER_SHORTCUT_CHANGED,
        ),
        TOP_LEFT(
            nameId = R.string.action_corner_top_left_name,
            target = ACTION_CORNER_TOP_LEFT_ACTION,
            metrics = SettingsEnums.ACTION_TOP_LEFT_ACTION_CORNER_SHORTCUT_CHANGED,
        ),
        TOP_RIGHT(
            nameId = R.string.action_corner_top_right_name,
            target = ACTION_CORNER_TOP_RIGHT_ACTION,
            metrics = SettingsEnums.ACTION_TOP_RIGHT_ACTION_CORNER_SHORTCUT_CHANGED,
        ),
    }
}
