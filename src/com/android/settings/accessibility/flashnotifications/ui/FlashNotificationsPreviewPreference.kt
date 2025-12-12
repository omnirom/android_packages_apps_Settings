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
import android.content.Intent
import android.os.UserHandle
import android.provider.Settings.System.CAMERA_FLASH_NOTIFICATION
import android.provider.Settings.System.SCREEN_FLASH_NOTIFICATION
import android.view.View
import com.android.settings.R
import com.android.settings.accessibility.FlashNotificationsUtil
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.ButtonPreference
import com.android.settingslib.widget.ButtonPreference.SIZE_EXTRA_LARGE
import com.android.settingslib.widget.ButtonPreference.TYPE_FILLED

class FlashNotificationsPreviewPreference :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider,
    View.OnClickListener,
    KeyedObserver<String?> {

    private lateinit var lifecycleContext: PreferenceLifecycleContext
    private lateinit var dataStore: KeyValueStore

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.flash_notifications_preview

    override val indexable
        get() = false

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        lifecycleContext = context
        dataStore = SettingsSystemStore.get(context)
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        dataStore.addObserver(CAMERA_FLASH_NOTIFICATION, this, HandlerExecutor.main)
        dataStore.addObserver(SCREEN_FLASH_NOTIFICATION, this, HandlerExecutor.main)
    }

    override fun onStop(context: PreferenceLifecycleContext) {
        super.onStop(context)
        dataStore.removeObserver(CAMERA_FLASH_NOTIFICATION, this)
        dataStore.removeObserver(SCREEN_FLASH_NOTIFICATION, this)
    }

    override fun createWidget(context: Context) =
        ButtonPreference(context).apply {
            setButtonStyle(TYPE_FILLED, SIZE_EXTRA_LARGE)
            setOnClickListener(this@FlashNotificationsPreviewPreference)
        }

    override fun isAvailable(context: Context): Boolean {
        val currentState = FlashNotificationsUtil.getFlashNotificationsState(context)
        return currentState != FlashNotificationsUtil.State.OFF
    }

    override fun onClick(v: View?) {
        val intent = Intent(FlashNotificationsUtil.ACTION_FLASH_NOTIFICATION_START_PREVIEW)
        intent.putExtra(
            FlashNotificationsUtil.EXTRA_FLASH_NOTIFICATION_PREVIEW_TYPE,
            FlashNotificationsUtil.TYPE_SHORT_PREVIEW,
        )
        v?.context?.sendBroadcastAsUser(intent, UserHandle.SYSTEM)
    }

    override fun onKeyChanged(key: String?, reason: Int) {
        lifecycleContext.notifyPreferenceChange(KEY)
    }

    companion object {
        const val KEY = "flash_notifications_preview"
    }
}
