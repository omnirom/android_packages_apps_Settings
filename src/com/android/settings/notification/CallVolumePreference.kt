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

package com.android.settings.notification

import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED
import android.Manifest.permission.MODIFY_PHONE_STATE
import android.app.settings.SettingsEnums.ACTION_CALL_VOLUME
import android.content.Context
import android.content.pm.PackageManager.FEATURE_AUTOMOTIVE
import android.media.AudioManager
import android.media.AudioManager.STREAM_BLUETOOTH_SCO
import android.media.AudioManager.STREAM_VOICE_CALL
import android.os.UserManager
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.contract.KEY_CALL_VOLUME
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.sound.VolumeSliderPreference
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class CallVolumePreference(private val audioHelper: AudioHelper) :
    IntRangeValuePreference,
    PreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceIconProvider,
    PreferenceRestrictionMixin {

    constructor(context: Context) : this(AudioHelper(context))

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.call_volume_option_title

    override val preferenceActionMetrics: Int
        get() = ACTION_CALL_VOLUME

    override fun tags(context: Context) = arrayOf(KEY_CALL_VOLUME)

    override fun getIcon(context: Context) = R.drawable.ic_local_phone_24_lib

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_call_volume) && !audioHelper.isSingleVolume

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys
        get() = arrayOf(UserManager.DISALLOW_ADJUST_VOLUME)

    override fun storage(context: Context): KeyValueStore =
        object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                audioHelper.getStreamVolume(getAudioStream(context)) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                audioHelper.setStreamVolume(getAudioStream(context), value as Int)
            }
        }

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context): Permissions? {
        var permissions = Permissions.allOf(MODIFY_AUDIO_SETTINGS, MODIFY_PHONE_STATE)
        if (context.packageManager.hasSystemFeature(FEATURE_AUTOMOTIVE)) {
            permissions = permissions and MODIFY_AUDIO_SETTINGS_PRIVILEGED
        }
        return permissions
    }

    override fun getWritePermit(context: Context, value: Int?, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getMinValue(context: Context) = audioHelper.getMinVolume(getAudioStream(context))

    override fun getMaxValue(context: Context) = audioHelper.getMaxVolume(getAudioStream(context))

    override fun createWidget(context: Context) = VolumeSliderPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSliderPreference).setStream(getAudioStream(preference.context))
    }

    @Suppress("DEPRECATION")
    fun getAudioStream(context: Context): Int {
        val audioManager = context.getSystemService(AudioManager::class.java)!!
        return when {
            audioManager.isBluetoothScoOn() -> STREAM_BLUETOOTH_SCO
            else -> STREAM_VOICE_CALL
        }
    }

    companion object {
        const val KEY = "call_volume"
    }
}
// LINT.ThenChange(CallVolumePreferenceController.java)
