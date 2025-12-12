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

package com.android.settings.accessibility.flashnotifications.ui

import android.content.Context
import android.provider.Settings
import android.provider.Settings.System.SCREEN_FLASH_NOTIFICATION_COLOR
import androidx.fragment.app.FragmentManager
import androidx.preference.Preference
import androidx.preference.Preference.OnPreferenceClickListener
import com.android.settings.R
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settings.accessibility.ScreenFlashNotificationColorDialogFragment
import com.android.settingslib.PrimarySwitchPreferenceBinding
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference

class ScreenFlashSwitchPreference : SwitchPreference(KEY, R.string.screen_flash_notification_title),
    PreferenceSummaryProvider, PrimarySwitchPreferenceBinding, PreferenceLifecycleProvider,
    OnPreferenceClickListener, KeyedObserver<String?> {

    private lateinit var lifecycleContext: PreferenceLifecycleContext
    private lateinit var fragmentManager: FragmentManager

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
        fragmentManager = context.fragmentManager
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        storage(context).addObserver(SCREEN_FLASH_NOTIFICATION_COLOR, this, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        storage(context).removeObserver(this)
    }

    override fun storage(context: Context): KeyValueStore = SettingsSystemStore.get(context)

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getWritePermit(
        context: Context,
        value: Boolean?,
        callingPid: Int,
        callingUid: Int
    ) = ReadWritePermit.ALLOW

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        preference.onPreferenceClickListener = this
    }

    override fun getSummary(context: Context): CharSequence? {
        return FlashNotificationsUtil.getColorDescriptionText(
            context,
            storage(context).getInt(SCREEN_FLASH_NOTIFICATION_COLOR)
                ?: FlashNotificationsUtil.DEFAULT_SCREEN_FLASH_COLOR
        )
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val initialColor =
            storage(preference.context).getInt(SCREEN_FLASH_NOTIFICATION_COLOR)
                ?: FlashNotificationsUtil.DEFAULT_SCREEN_FLASH_COLOR

        ScreenFlashNotificationColorDialogFragment.getInstance(initialColor).show(
            fragmentManager,
            ScreenFlashNotificationColorDialogFragment::class.java.getSimpleName()
        )
        return true
    }

    override fun onKeyChanged(key: String?, reason: Int) {
        lifecycleContext.notifyPreferenceChange(KEY)
    }

    companion object {
        const val KEY = Settings.System.SCREEN_FLASH_NOTIFICATION
    }
}