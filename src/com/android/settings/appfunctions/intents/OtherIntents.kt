/*
 * Copyright 2025 The Android Open Source Project
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
package com.android.settings.appfunctions.intents

import com.android.settings.appfunctions.DeviceStateAppFunctionType.GET_UNCATEGORIZED
import com.android.settings.appfunctions.providers.StaticIntent
import com.android.settings.appfunctions.providers.StaticIntents

fun getOtherIntents() =
    StaticIntents(
        GET_UNCATEGORIZED,
        listOf(
            StaticIntent(
                description = "Pair new device: Settings to pair a new Bluetooth device.",
                intentUri =
                    "intent:#Intent;action=android.settings.BLUETOOTH_PAIRING_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Saved devices: Settings to show the list of previously connected Bluetooth devices.",
                intentUri =
                    "intent:#Intent;action=com.android.settings.PREVIOUSLY_CONNECTED_DEVICE;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Cast: Settings to configure Cast options to send screen or media to other devices.",
                intentUri =
                    "intent:#Intent;action=android.settings.CAST_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Wallpaper & Style: Settings to change the wallpaper and style of the device.",
                intentUri =
                    "intent:#Intent;component=com.android.settings/.wallpaper.StyleSuggestionActivity;end",
            ),
            StaticIntent(
                description = "Backup: Settings to configure the backup options for the device.",
                intentUri =
                    "intent:#Intent;action=com.android.settings.BACKUP_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Send feedback about this device: Intent to generate a report that you can share with the device manufacturer.",
                intentUri = "intent:#Intent;action=android.intent.action.BUG_REPORT;end",
            ),
        ),
    )
