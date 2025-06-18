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
package com.android.settings.security

import android.content.Context
import android.provider.Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS
import android.provider.Settings.Secure.LOCK_SCREEN_SHOW_NOTIFICATIONS
import com.android.settings.R
import com.android.settings.Settings.LockScreenSettingsActivity
import com.android.settings.display.AmbientDisplayAlwaysOnPreference
import com.android.settings.flags.Flags
import com.android.settings.notification.LockScreenNotificationPreferenceController
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

@ProvidePreferenceScreen(LockScreenPreferenceScreen.KEY)
open class LockScreenPreferenceScreen(private val context: Context) :
    AbstractKeyedDataObservable<String>(), PreferenceScreenCreator, PreferenceSummaryProvider {

    private val observer =
        KeyedObserver<String> { _, _ -> notifyChange(KEY, PreferenceChangeReason.STATE) }

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.lockscreen_settings_title

    override val keywords: Int
        get() = R.string.keywords_ambient_display_screen

    override fun onFirstObserverAdded() {
        val store = SettingsSecureStore.get(context)
        val executor = HandlerExecutor.main
        // update summary when lock screen notification settings are changed
        store.addObserver(LOCK_SCREEN_SHOW_NOTIFICATIONS, observer, executor)
        store.addObserver(LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, observer, executor)
    }

    override fun onLastObserverRemoved() {
        val store = SettingsSecureStore.get(context)
        store.removeObserver(LOCK_SCREEN_SHOW_NOTIFICATIONS, observer)
        store.removeObserver(LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, observer)
    }

    override fun getSummary(context: Context): CharSequence? =
        context.getString(LockScreenNotificationPreferenceController.getSummaryResource(context))

    override fun isFlagEnabled(context: Context) = Flags.catalystLockscreenFromDisplaySettings()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass() = LockscreenDashboardFragment::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, LockScreenSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +AmbientDisplayAlwaysOnPreference()
        }

    companion object {
        const val KEY = "lockscreen_from_display_settings"
    }
}
