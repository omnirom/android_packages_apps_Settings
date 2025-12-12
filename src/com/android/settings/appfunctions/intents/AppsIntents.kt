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

fun getAppsIntents() =
    StaticIntents(
        GET_UNCATEGORIZED,
        listOf(
            StaticIntent(
                description =
                    "App permissions: Intent to open the app permissions screen for a specific app. The Intent uri is the following (replace \${package_name} with the app package name)",
                intentUri =
                    "intent:#Intent;action=android.intent.action.MANAGE_APP_PERMISSIONS;S.android.intent.extra.PACKAGE_NAME=\${package_name};B.hideInfoButton=true;end",
            ),
            StaticIntent(
                description =
                    "App screen time: Intent to open the app screen time screen for a specific app. The Intent uri is the following (replace \${package_name} with the app package name). This screen includes the daily and hourly breakdown of screen time for a particular app.",
                intentUri =
                    "intent:#Intent;action=android.settings.APP_USAGE_SETTINGS;S.android.intent.extra.PACKAGE_NAME=\${package_name};end",
            ),
            StaticIntent(
                description =
                    "Full screen notification: Settings to manage the full screen notification.",
                intentUri =
                    "intent:#Intent;component=com.android.settings/.ManageFullScreenIntent;end",
            ),
            StaticIntent(
                description = "Unused apps: Settings to manage unused apps.",
                intentUri =
                    "intent:#Intent;action=android.intent.action.MANAGE_UNUSED_APPS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Connected work & personal apps: Settings to manage the connection between work and personal apps.",
                intentUri =
                    "intent:#Intent;action=android.settings.MANAGE_CROSS_PROFILE_ACCESS;package=com.android.settings;end",
            ),
        ),
    )
