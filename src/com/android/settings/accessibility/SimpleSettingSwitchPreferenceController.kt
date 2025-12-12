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

import android.content.ComponentName
import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.preference.Preference
import androidx.preference.PreferenceScreen
import androidx.preference.TwoStatePreference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.core.BasePreferenceController

/**
 * A base preference controller that triggers a Settings to be on/off when toggling the preference
 * UI.
 */
abstract class SimpleSettingSwitchPreferenceController(context: Context, prefKey: String) :
    BasePreferenceController(context, prefKey),
    DefaultLifecycleObserver,
    Preference.OnPreferenceChangeListener {

    private var preference: TwoStatePreference? = null
    private var contentObserver: ContentObserver =
        object : ContentObserver(Looper.myLooper()?.run { Handler(/* async= */ true) }) {
            override fun onChange(selfChange: Boolean) {
                preference?.let { updateState(it) }
            }
        }

    abstract fun getSettingKey(): String

    abstract fun getComponentName(): ComponentName

    override fun getAvailabilityStatus(): Int = AVAILABLE

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        mContext.contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(getSettingKey()),
            /* notifyForDescendants= */ false,
            contentObserver,
        )
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        mContext.contentResolver.unregisterContentObserver(contentObserver)
    }

    override fun displayPreference(screen: PreferenceScreen?) {
        super.displayPreference(screen)
        preference = screen?.findPreference(preferenceKey)
    }

    override fun updateState(preference: Preference?) {
        super.updateState(preference)
        if (preference is TwoStatePreference) {
            preference.isChecked = isChecked()
        }
    }

    private fun setChecked(preference: TwoStatePreference, checked: Boolean) {
        AccessibilityStatsLogUtils.logAccessibilityServiceEnabled(getComponentName(), checked)
        Settings.Secure.putInt(
            mContext.contentResolver,
            getSettingKey(),
            if (checked) AccessibilityUtil.State.ON else AccessibilityUtil.State.OFF,
        )
        updateState(preference)
    }

    private fun isChecked(): Boolean {
        return Settings.Secure.getInt(
            mContext.contentResolver,
            getSettingKey(),
            AccessibilityUtil.State.OFF,
        ) == AccessibilityUtil.State.ON
    }

    override fun onPreferenceChange(preference: Preference, newValue: Any?): Boolean {
        if (preference is TwoStatePreference && newValue is Boolean) {
            setChecked(preference, newValue)
        }
        return false
    }
}

class ColorInversionMainSwitchPreferenceController(context: Context, prefKey: String) :
    SimpleSettingSwitchPreferenceController(context, prefKey) {
    override fun getSettingKey(): String = Settings.Secure.ACCESSIBILITY_DISPLAY_INVERSION_ENABLED

    override fun getComponentName(): ComponentName =
        AccessibilityShortcutController.COLOR_INVERSION_COMPONENT_NAME
}

class DaltonizerMainSwitchPreferenceController(context: Context, prefKey: String) :
    SimpleSettingSwitchPreferenceController(context, prefKey) {
    override fun getSettingKey(): String = Settings.Secure.ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED

    override fun getComponentName(): ComponentName =
        AccessibilityShortcutController.DALTONIZER_COMPONENT_NAME
}
