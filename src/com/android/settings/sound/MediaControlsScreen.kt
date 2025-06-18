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

package com.android.settings.sound

import android.content.Context
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.PreferenceScreenCreator

// LINT.IfChange
@ProvidePreferenceScreen(MediaControlsScreen.KEY)
class MediaControlsScreen(context: Context) :
    AbstractKeyedDataObservable<String>(), PreferenceScreenCreator, PreferenceSummaryProvider {

    private val observer =
        KeyedObserver<String> { _, _ -> notifyChange(KEY, PreferenceChangeReason.STATE) }

    private val mediaControlsStore = MediaControlsStore(SettingsSecureStore.get(context))

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.media_controls_title

    override val keywords: Int
        get() = R.string.keywords_media_controls

    override fun onFirstObserverAdded() {
        mediaControlsStore.addObserver(
            MediaControlsSwitchPreference.KEY,
            observer,
            HandlerExecutor.main,
        )
    }

    override fun onLastObserverRemoved() {
        mediaControlsStore.removeObserver(MediaControlsSwitchPreference.KEY, observer)
    }

    override fun isFlagEnabled(context: Context) = Flags.catalystMediaControls()

    override fun fragmentClass() = MediaControlsSettings::class.java

    override fun getPreferenceHierarchy(context: Context) =
        preferenceHierarchy(context, this) {
            +MediaControlsSwitchPreference(mediaControlsStore)
            +MediaControlsLockscreenSwitchPreference()
        }

    override fun getSummary(context: Context): CharSequence? =
        if (mediaControlsStore.getBoolean(MediaControlsSwitchPreference.KEY) == false) {
            context.getString(R.string.media_controls_hide_player)
        } else {
            context.getString(R.string.media_controls_show_player)
        }

    @Suppress("UNCHECKED_CAST")
    class MediaControlsStore(private val settingsStore: KeyValueStore) : KeyValueStoreDelegate {

        override val keyValueStoreDelegate
            get() = settingsStore

        override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) = true as T
    }

    companion object {
        const val KEY = "media_controls"
    }
}
// LINT.ThenChange(MediaControlsSettings.java)
