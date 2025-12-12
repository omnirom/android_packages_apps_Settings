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
import android.app.NotificationManager.ACTION_EFFECTS_SUPPRESSOR_CHANGED
import android.app.settings.SettingsEnums.ACTION_NOTIFICATION_VOLUME
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION
import android.media.AudioManager.RINGER_MODE_NORMAL
import android.media.AudioManager.RINGER_MODE_SILENT
import android.media.AudioManager.RINGER_MODE_VIBRATE
import android.media.AudioManager.STREAM_NOTIFICATION
import android.os.UserManager.DISALLOW_ADJUST_VOLUME
import android.os.Vibrator
import android.view.View.ACCESSIBILITY_LIVE_REGION_NONE
import android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.contract.KEY_NOTIFICATION_VOLUME
import com.android.settings.metrics.PreferenceActionMetricsProvider
import com.android.settings.restriction.PreferenceRestrictionMixin
import com.android.settings.sound.VolumeSliderPreference
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.datastore.and
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceIconProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.preference.PreferenceBinding

// LINT.IfChange
class NotificationVolumePreference(private val audioHelper: AudioHelper) :
    IntRangeValuePreference,
    PreferenceBinding,
    PreferenceActionMetricsProvider,
    PreferenceAvailabilityProvider,
    PreferenceIconProvider,
    PreferenceRestrictionMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.notification_volume_option_title

    override fun getIcon(context: Context) = context.getIconRes()

    override val preferenceActionMetrics: Int
        get() = ACTION_NOTIFICATION_VOLUME

    override fun tags(context: Context) = arrayOf(KEY_NOTIFICATION_VOLUME)

    override fun isAvailable(context: Context) =
        context.resources.getBoolean(R.bool.config_show_notification_volume) &&
            !audioHelper.isSingleVolume

    override fun isEnabled(context: Context) =
        super<PreferenceRestrictionMixin>.isEnabled(context) &&
            audioHelper.ringerModeInternal == RINGER_MODE_NORMAL

    override val restrictionKeys
        get() = arrayOf(DISALLOW_ADJUST_VOLUME)

    override fun storage(context: Context): KeyValueStore =
        object : AbstractKeyedDataObservable<String>(), KeyValueStore {
            private var broadcastReceiver: BroadcastReceiver? = null

            override fun contains(key: String) = key == KEY

            @Suppress("UNCHECKED_CAST")
            override fun <T : Any> getValue(key: String, valueType: Class<T>) =
                audioHelper.getStreamVolume(STREAM_NOTIFICATION) as T

            override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
                audioHelper.setStreamVolume(STREAM_NOTIFICATION, value as Int)
            }

            override fun onFirstObserverAdded() {
                val receiver =
                    object : BroadcastReceiver() {
                        override fun onReceive(receiverContext: Context, intent: Intent) {
                            notifyChange(KEY, PreferenceChangeReason.STATE)
                        }
                    }
                context.registerReceiver(
                    receiver,
                    IntentFilter().apply {
                        addAction(ACTION_EFFECTS_SUPPRESSOR_CHANGED)
                        addAction(INTERNAL_RINGER_MODE_CHANGED_ACTION)
                    },
                )
                broadcastReceiver = receiver
            }

            override fun onLastObserverRemoved() {
                broadcastReceiver?.let { context.unregisterReceiver(it) }
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

    override fun getWritePermit(context: Context, myUid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override fun getMinValue(context: Context) = audioHelper.getMinVolume(STREAM_NOTIFICATION)

    override fun getMaxValue(context: Context) = audioHelper.getMaxVolume(STREAM_NOTIFICATION)

    override fun createWidget(context: Context) = VolumeSliderPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        (preference as VolumeSliderPreference).apply {
            setStream(STREAM_NOTIFICATION)
            setMuteIcon(context.getIconRes())
            setListener { updateContentDescription() }
            setSuppressionText(context.getSuppressionText())
            updateContentDescription()
        }
    }

    private fun VolumeSliderPreference.updateContentDescription() {
        when (context.getEffectiveRingerMode()) {
            RINGER_MODE_VIBRATE -> {
                setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE)
                updateContentDescription(
                    context.getString(R.string.notification_volume_content_description_vibrate_mode)
                )
            }
            RINGER_MODE_SILENT -> {
                setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_POLITE)
                updateContentDescription(
                    context.getString(R.string.volume_content_description_silent_mode, title)
                )
            }
            else -> {
                setAccessibilityLiveRegion(ACCESSIBILITY_LIVE_REGION_NONE)
                updateContentDescription(title)
            }
        }
    }

    private fun Context.getIconRes() =
        when (getEffectiveRingerMode()) {
            RINGER_MODE_NORMAL -> R.drawable.ic_notifications
            RINGER_MODE_VIBRATE -> R.drawable.ic_volume_ringer_vibrate
            else -> R.drawable.ic_notifications_off_24dp
        }

    private fun Context.getEffectiveRingerMode(): Int {
        val hasVibrator = getSystemService(Vibrator::class.java)?.hasVibrator() == true
        val ringerMode = audioHelper.ringerModeInternal
        return when {
            !hasVibrator && ringerMode == RINGER_MODE_VIBRATE -> RINGER_MODE_SILENT
            ringerMode == RINGER_MODE_NORMAL &&
                audioHelper.getStreamVolume(STREAM_NOTIFICATION) == 0 -> RINGER_MODE_SILENT
            else -> ringerMode
        }
    }

    companion object {
        const val KEY = "notification_volume"
    }
}
// LINT.ThenChange(NotificationVolumePreferenceController.java)
