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
package com.android.settings.notification

import android.app.settings.SettingsEnums.ACTION_TOUCH_SOUND
import android.content.Context
import android.media.AudioManager
import android.provider.Settings.System.SOUND_EFFECTS_ENABLED
import androidx.annotation.VisibleForTesting
import com.android.settings.R
import com.android.settings.contract.KEY_TOUCH_SOUNDS
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyValueStoreDelegate
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.SwitchPreference
import com.android.settingslib.preference.SwitchPreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

// LINT.IfChange
class TouchSoundPreference(context: Context) :
    SwitchPreference(KEY, R.string.touch_sounds_title),
    SwitchPreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider {

    private val storage = TouchSoundStorage(context)

    override val preferenceActionMetrics: Int
        get() = ACTION_TOUCH_SOUND

    override fun tags(context: Context) = arrayOf(KEY_TOUCH_SOUNDS)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_touch_sounds)

    override fun storage(context: Context): KeyValueStore = storage

    override fun getReadPermissions(context: Context) = SettingsSystemStore.getReadPermissions()

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context) = SettingsSystemStore.getWritePermissions()

    override fun getWritePermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    companion object {
        const val KEY = SOUND_EFFECTS_ENABLED
    }
}

internal class TouchSoundStorage(private val context: Context) : KeyValueStoreDelegate {
    // use limitedParallelism to prevent race condition and achieve sequential semantics
    private val coroutineScope = CoroutineScope(Dispatchers.IO.limitedParallelism(1))
    @VisibleForTesting internal var job: Job? = null

    override val keyValueStoreDelegate: KeyValueStore =
        SettingsSystemStore.get(context).apply { setDefaultValue(TouchSoundPreference.KEY, true) }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        super.setValue(key, valueType, value)
        val isChecked = getBoolean(key)
        job?.cancel()
        job =
            coroutineScope.launch {
                val audioManager = context.getSystemService(AudioManager::class.java)!!
                if (isChecked == true) {
                    audioManager.loadSoundEffects()
                } else {
                    audioManager.unloadSoundEffects()
                }
            }
    }
}
// LINT.ThenChange(TouchSoundPreferenceController.java)
