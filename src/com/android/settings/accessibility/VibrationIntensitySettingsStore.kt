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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.VibrationAttributes.USAGE_NOTIFICATION
import android.os.VibrationAttributes.USAGE_RINGTONE
import android.os.VibrationAttributes.Usage
import android.os.Vibrator
import android.os.Vibrator.VIBRATION_INTENSITY_OFF
import android.provider.Settings.System.HAPTIC_FEEDBACK_ENABLED
import android.provider.Settings.System.HAPTIC_FEEDBACK_INTENSITY
import android.provider.Settings.System.HARDWARE_HAPTIC_FEEDBACK_INTENSITY
import android.provider.Settings.System.RING_VIBRATION_INTENSITY
import android.provider.Settings.System.VIBRATE_ON
import android.provider.Settings.System.VIBRATE_WHEN_RINGING
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityUtil.State
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSystemStore
import com.android.settingslib.metadata.PreferenceChangeReason
import kotlin.math.min

/** SettingsStore for vibration intensity preferences with custom default value. */
class VibrationIntensitySettingsStore(
    private val context: Context,
    private val preferenceKey: String,
    private val settingsProviderKey: String,
    @Usage vibrationUsage: Int,
    private val keyValueStoreDelegate: KeyValueStore = SettingsSystemStore.get(context),
    private val defaultIntensity: Int = context.getDefaultVibrationIntensity(vibrationUsage),
    private val supportedIntensityLevels: Int = context.getSupportedVibrationIntensityLevels(),
) : AbstractKeyedDataObservable<String>(), KeyedObserver<String>, KeyValueStore {

    private val hasRingerModeDependency: Boolean =
        when (vibrationUsage) {
            USAGE_RINGTONE,
            USAGE_NOTIFICATION -> true
            else -> false
        }

    private var ringerModeBroadcastReceiver: BroadcastReceiver? = null

    fun isPreferenceEnabled(): Boolean {
        if (keyValueStoreDelegate.getBoolean(VIBRATE_ON) == false) {
            return false
        }
        return !isDisabledByRingerMode()
    }

    fun isDisabledByRingerMode(): Boolean {
        return (keyValueStoreDelegate.getBoolean(VIBRATE_ON) != false) &&
            (hasRingerModeDependency && context.isRingerModeSilent())
    }

    fun getSummary(): CharSequence? =
        // Only display summary if ringer mode silent is the one disabling this preference.
        if (isDisabledByRingerMode()) {
            context.getString(
                R.string.accessibility_vibration_setting_disabled_for_silent_mode_summary
            )
        } else {
            null
        }

    fun isIntensityOff(): Boolean = getInt(settingsProviderKey) == VIBRATION_INTENSITY_OFF

    override fun contains(key: String) = keyValueStoreDelegate.contains(settingsProviderKey)

    override fun <T : Any> getDefaultValue(key: String, valueType: Class<T>) =
        intensityToValue(valueType, defaultIntensity)

    override fun <T : Any> getValue(key: String, valueType: Class<T>) =
        if (isPreferenceEnabled()) {
            getFromDeprecatedValue(valueType)
                ?: intensityToValue(
                    valueType,
                    keyValueStoreDelegate.getInt(settingsProviderKey) ?: defaultIntensity,
                )
        } else {
            // Preference must show intensity off when disabled, but value stored must be preserved.
            intensityToValue(valueType, VIBRATION_INTENSITY_OFF)
        }

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {
        val intensity = value?.let { valueToIntensity(valueType, it) }
        keyValueStoreDelegate.setInt(settingsProviderKey, intensity)
        setDependentValues(intensity)
    }

    override fun onFirstObserverAdded() {
        // observe the underlying storage key
        keyValueStoreDelegate.addObserver(settingsProviderKey, this, HandlerExecutor.main)
        if (!hasRingerModeDependency) {
            return
        }
        ringerModeBroadcastReceiver =
            object : BroadcastReceiver() {
                override fun onReceive(broadcastContext: Context?, intent: Intent?) {
                    notifyChange(PreferenceChangeReason.STATE)
                }
            }
        val intentFilter = IntentFilter(AudioManager.INTERNAL_RINGER_MODE_CHANGED_ACTION)
        context.registerReceiver(
            ringerModeBroadcastReceiver,
            intentFilter,
            Context.RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onLastObserverRemoved() {
        keyValueStoreDelegate.removeObserver(settingsProviderKey, this)
        ringerModeBroadcastReceiver?.let { context.unregisterReceiver(it) }
    }

    override fun onKeyChanged(key: String, reason: Int) {
        // forward data change to preference hierarchy key
        notifyChange(preferenceKey, reason)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> intensityToValue(valueType: Class<T>, intensity: Int): T? =
        when (valueType) {
            Boolean::class.javaObjectType -> intensityToBooleanValue(intensity)
            Int::class.javaObjectType -> intensityToIntValue(intensity)
            else -> null
        }
            as T?

    private fun intensityToBooleanValue(intensity: Int): Boolean? =
        intensity != VIBRATION_INTENSITY_OFF

    private fun intensityToIntValue(intensity: Int): Int? = min(intensity, supportedIntensityLevels)

    private fun <T : Any> valueToIntensity(valueType: Class<T>, value: T): Int? =
        when (valueType) {
            Boolean::class.javaObjectType -> booleanValueToIntensity(value as Boolean)
            Int::class.javaObjectType -> intValueToIntensity(value as Int)
            else -> null
        }

    private fun booleanValueToIntensity(value: Boolean): Int? =
        if (value) defaultIntensity else VIBRATION_INTENSITY_OFF

    private fun intValueToIntensity(value: Int): Int? =
        if (value == VIBRATION_INTENSITY_OFF) {
            VIBRATION_INTENSITY_OFF
        } else if (supportedIntensityLevels == 1) {
            // If there is only one intensity available besides OFF, then use the device default
            // intensity to ensure no scaling will ever happen in the platform.
            defaultIntensity
        } else if (value < supportedIntensityLevels) {
            // If value is within supported intensity levels then return the raw value as intensity.
            value
        } else {
            // If the settings granularity is lower than the platform's then map the max position to
            // the highest vibration intensity, skipping intermediate values in the scale.
            Vibrator.VIBRATION_INTENSITY_HIGH
        }

    /**
     * Load intensity based on deprecated settings for given key.
     *
     * <p>This is required to support users that have only set this preference before its
     * deprecation, to make sure the settings are preserved after its deprecation.
     */
    @Suppress("DEPRECATION") // Loading deprecated settings key to maintain support.
    private fun <T : Any> getFromDeprecatedValue(valueType: Class<T>): T? {
        when (settingsProviderKey) {
            HAPTIC_FEEDBACK_INTENSITY -> {
                if (keyValueStoreDelegate.getInt(HAPTIC_FEEDBACK_ENABLED) == State.OFF) {
                    // This is deprecated but should still be applied if the user has turned it off.
                    return intensityToValue(valueType, VIBRATION_INTENSITY_OFF)
                }
            }
        }
        return null
    }

    /** Set dependent/deprecated settings based on new intensity value being set for given key. */
    @Suppress("DEPRECATION") // Updating deprecated settings key to maintain support.
    private fun setDependentValues(intensity: Int?) {
        when (settingsProviderKey) {
            RING_VIBRATION_INTENSITY -> {
                // This is deprecated but should still reflect the intensity setting.
                // Ramping ringer is independent of the ring intensity and should not be affected.
                keyValueStoreDelegate.setBoolean(
                    VIBRATE_WHEN_RINGING,
                    intensity?.let { it != VIBRATION_INTENSITY_OFF },
                )
            }
            HAPTIC_FEEDBACK_INTENSITY -> {
                // This is dependent on this setting, but should not be disabled by it.
                keyValueStoreDelegate.setInt(
                    HARDWARE_HAPTIC_FEEDBACK_INTENSITY,
                    intensity?.let { if (it == VIBRATION_INTENSITY_OFF) defaultIntensity else it },
                )
                // This is deprecated but should still reflect the intensity setting.
                keyValueStoreDelegate.setInt(
                    HAPTIC_FEEDBACK_ENABLED,
                    intensity?.let { if (it == VIBRATION_INTENSITY_OFF) State.OFF else State.ON },
                )
            }
        }
    }
}

/** Returns the device default vibration intensity for given usage. */
private fun Context.getDefaultVibrationIntensity(@Usage vibrationUsage: Int): Int =
    getSystemService(Vibrator::class.java).getDefaultVibrationIntensity(vibrationUsage)

/** Returns the number of vibration intensity levels supported by this device. */
fun Context.getSupportedVibrationIntensityLevels(): Int =
    resources.getInteger(R.integer.config_vibration_supported_intensity_levels)

/** Returns true if the device ringer mode is silent. */
private fun Context.isRingerModeSilent() =
    // AudioManager.isSilentMode() also returns true when ringer mode is RINGER_MODE_VIBRATE.
    // The vibration preferences are only disabled when the ringer mode is RINGER_MODE_SILENT.
    // Use getRingerModeInternal() method to check the actual ringer mode.
    getSystemService(AudioManager::class.java)?.ringerModeInternal ==
        AudioManager.RINGER_MODE_SILENT
