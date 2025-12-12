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

import android.Manifest.permission.MODIFY_AUDIO_SETTINGS
import android.Manifest.permission.MODIFY_AUDIO_SETTINGS_PRIVILEGED
import android.app.settings.SettingsEnums.ACTION_ALARM_VOLUME
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager.STREAM_ALARM
import android.os.UserManager.DISALLOW_ADJUST_VOLUME
import androidx.preference.Preference
import com.android.internal.R as AndroidR
import com.android.settings.R
import com.android.settings.contract.KEY_ALARM_VOLUME
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
class AlarmVolumePreference(private val audioHelper: AudioHelper) :
    IntRangeValuePreference,
    PreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceIconProvider,
    PreferenceRestrictionMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.alarm_volume_option_title

    override val preferenceActionMetrics: Int
        get() = ACTION_ALARM_VOLUME

    override fun tags(context: Context) = arrayOf(KEY_ALARM_VOLUME)

    override fun getIcon(context: Context) =
        when {
            VolumeHelper.isMuted(context, STREAM_ALARM) -> AndroidR.drawable.ic_audio_alarm_mute
            else -> AndroidR.drawable.ic_audio_alarm
        }

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_alarm_volume) && !audioHelper.isSingleVolume

    override fun isEnabled(context: Context) = super<PreferenceRestrictionMixin>.isEnabled(context)

    override val restrictionKeys: Array<String>
        get() = arrayOf(DISALLOW_ADJUST_VOLUME)

    override fun storage(context: Context): KeyValueStore =
        object : NoOpKeyedObservable<String>(), KeyValueStore {
            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                audioHelper.getStreamVolume(STREAM_ALARM) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                audioHelper.setStreamVolume(STREAM_ALARM, value as Int)
            }
        }

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getReadPermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermissions(context: Context): Permissions? {
        var permissions = Permissions.allOf(MODIFY_AUDIO_SETTINGS)
        if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            permissions = permissions and MODIFY_AUDIO_SETTINGS_PRIVILEGED
        }
        return permissions
    }

    override fun getWritePermit(context: Context, value: Int?, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getMinValue(context: Context) = audioHelper.getMinVolume(STREAM_ALARM)

    override fun getMaxValue(context: Context) = audioHelper.getMaxVolume(STREAM_ALARM)

    override fun createWidget(context: Context) = VolumeSliderPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSliderPreference).apply {
            setStream(STREAM_ALARM)
            setMuteIcon(AndroidR.drawable.ic_audio_alarm_mute)
        }
    }

    companion object {
        const val KEY = "alarm_volume"
    }
}
// LINT.ThenChange(AlarmVolumePreferenceController.java)
