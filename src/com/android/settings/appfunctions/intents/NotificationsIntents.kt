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

import com.android.settings.appfunctions.DeviceStateAppFunctionType.GET_NOTIFICATIONS
import com.android.settings.appfunctions.providers.StaticIntent
import com.android.settings.appfunctions.providers.StaticIntents

fun getNotificationsIntents() =
    StaticIntents(
        GET_NOTIFICATIONS,
        listOf(
            StaticIntent(
                description = "Notifications: Settings to manage and customize notifications.",
                intentUri =
                    "intent:#Intent;action=android.settings.NOTIFICATION_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Notification history: Settings to see the history of notifications received.",
                intentUri =
                    "intent:#Intent;action=android.settings.NOTIFICATION_HISTORY;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Conversations: Settings to manage and customize how conversations are displayed in notifications.",
                intentUri =
                    "intent:#Intent;action=android.settings.CONVERSATION_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Bubbles: Settings to manage and customize how bubbles are displayed for notifications.",
                intentUri =
                    "intent:#Intent;action=android.settings.NOTIFICATION_BUBBLE_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Notification read, reply & control: Settings to manage apps that can read, reply and control notifications.",
                intentUri =
                    "intent:#Intent;action=android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS;package=com.android.settings;end",
            ),
        ),
    )
