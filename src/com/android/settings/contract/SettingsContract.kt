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

package com.android.settings.contract

// NOTES:
//   - Once a key is added, never modify the literal string.
//   - Remove a key might cause backward compatibility issues.
//   - Avoid importing other class to reduce dependency whenever possible.

/**
 * Contract key for the "Airplane Mode" setting, identical to
 * [android.provider.SettingsSlicesContract.KEY_AIRPLANE_MODE].
 */
const val KEY_AIRPLANE_MODE = "airplane_mode"

/**
 * Contract key for the "Battery Saver" setting, identical to
 * [android.provider.SettingsSlicesContract.KEY_BATTERY_SAVER].
 */
const val KEY_BATTERY_SAVER = "battery_saver"

/**
 * Contract key for the "Bluetooth" setting, identical to
 * [android.provider.SettingsSlicesContract.KEY_BLUETOOTH].
 */
const val KEY_BLUETOOTH = "bluetooth"

/**
 * Contract key for the "Location" setting, identical to
 * [android.provider.SettingsSlicesContract.KEY_LOCATION].
 */
const val KEY_LOCATION = "location"

/**
 * Contract key for the "Wi-fi" setting, identical to
 * [android.provider.SettingsSlicesContract.KEY_WIFI].
 */
const val KEY_WIFI = "wifi"

/** Contract key for the "Use Wi-Fi calling" setting. */
const val KEY_WIFI_CALLING = "wifi_calling"

/** Contract key for the "Use Data Saver" setting. */
const val KEY_DATA_SAVER = "data_saver"

/** Contract key for the "Mobile data" setting. */
const val KEY_MOBILE_DATA = "mobile_data"

/** Contract key for the "Adaptive brightness" setting. */
const val KEY_ADAPTIVE_BRIGHTNESS = "auto_brightness"

/** Contract key for the "Screen attention" setting. */
const val KEY_SCREEN_ATTENTION = "screen_attention"

/** Contract key for the "Use adaptive connectivity" setting. */
const val KEY_ADAPTIVE_CONNECTIVITY = "adaptive_connectivity"

/** Contract key for the "Auto-switch Wi-Fi to Cellular" setting. */
const val KEY_ADAPTIVE_WIFI_SCORER = "adaptive_wifi_scorer"

/** Contract key for the " Auto-switch mobile network for battery life" setting. */
const val KEY_ADAPTIVE_MOBILE_NETWORK = "adaptive_mobile_network"

/** Contract key for the "WiFi hotspot" setting. */
const val KEY_WIFI_HOTSPOT = "enable_wifi_ap"

/** Contract key for the "Battery Gauge Slider" setting. */
const val KEY_BATTERY_LEVEL = "battery_level"

/** Contract key for the "Battery Percentage" setting. */
const val KEY_BATTERY_PERCENTAGE = "battery_percentage"

/** Contract key for the "Brightness level" setting. */
const val KEY_BRIGHTNESS_LEVEL = "brightness_level"

/** Contract key for the "Smooth display" setting. */
const val KEY_SMOOTH_DISPLAY = "smooth_display"

/** Contract key for the "Dark theme" setting. */
const val KEY_DARK_THEME = "dark_theme"

/** Contract key for the "Always show time and info" setting. */
const val KEY_AMBIENT_DISPLAY_ALWAYS_ON = "ambient_display_always_on"

/** Contract key for the "Use vibration & haptics" setting. */
const val KEY_VIBRATION_HAPTICS = "vibration_haptics"

/** Contract key for the "Media volume" setting. */
const val KEY_MEDIA_VOLUME = "media_volume"

/** Contract key for the "Call volume" setting. */
const val KEY_CALL_VOLUME = "call_volume"

/** Contract key for the "Ring volume" setting. */
const val KEY_RING_VOLUME = "separate_ring_volume"

/** Contract key for the "Remove animation" setting. */
const val KEY_REMOVE_ANIMATION = "remove_animation"

/** Contract key for the "Pin media player. */
const val KEY_PIN_MEDIA_PLAYER = "pin_media_player"

/** Contract key for the "Show media on lock screen. */
const val KEY_SHOW_MEDIA_ON_LOCK_SCREEN = "show_media_on_lock_screen"
