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

fun getModesIntents() =
    StaticIntents(
        GET_UNCATEGORIZED,
        listOf(
            StaticIntent(
                description =
                    "Open Do not disturb settings to configure manual rules. The Do Not Disturb screen helps users quiet their device by limiting how they are notified. Typically, notification sounds and vibrations are switched off. Users can customize Do Not Disturb to set exceptions for specific people and apps, and change how notifications are displayed.",
                intentUri =
                    "intent:#Intent;action=android.settings.AUTOMATIC_ZEN_RULE_SETTINGS;package=com.android.settings;S.android.provider.extra.AUTOMATIC_ZEN_RULE_ID=MANUAL_RULE;end",
            ),
            StaticIntent(
                description =
                    "Open DND Display settings to configure how DND affects the display. This screen allows users to adjust various display settings to match \"Do Not Disturb\" mode by setting the color to grayscale, making the screen dark, and dimming the wallpaper.",
                intentUri =
                    "intent:#Intent;action=com.android.settings.DND_DISPLAY_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description = "Open DND People settings to configure which people can bypass DND",
                intentUri =
                    "intent:#Intent;action=com.android.settings.DND_PEOPLE_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description = "Open DND Calls settings to configure which calls can bypass DND",
                intentUri =
                    "intent:#Intent;action=com.android.settings.DND_CALLS_SETTINGS;package=com.android.settings;end",
            ),
        ),
    )
