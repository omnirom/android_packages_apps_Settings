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

package com.android.settings.notification.modes.devicestate

import android.content.Context
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.notification.modes.ZenMode

/**
 * This DND Button preference is dedicated for device state. It functions via a virtual key and is
 * separate from the current Settings user interface, which it does not affect. Obviously, this is
 * not fully migrated item.
 */
// LINT.IfChange
class ZenModeButtonPreference(val zenMode: ZenMode) :
    PreferenceMetadata,
    BooleanValuePreference,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider {
    override val key: String
        get() = KEY

    override val summary: Int
        get() =
            if (zenMode.isActive) R.string.zen_mode_action_deactivate
            else R.string.zen_mode_action_activate

    override fun tags(context: Context) = arrayOf(TAG_DEVICE_STATE_PREFERENCE)

    override fun isAvailable(context: Context) =
        zenMode.isEnabled && (zenMode.isActive || zenMode.isManualInvocationAllowed)

    override fun getTitle(context: Context): CharSequence? = zenMode.name

    override fun storage(context: Context): KeyValueStore =
        object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                zenMode.isActive as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}
        }

    companion object {
        const val KEY = "device_state_activate" // only for device state.
    }
}
// LINT.ThenChange(../ZenModeButtonPreferenceController.java)
