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

fun getSecurityIntents() =
    StaticIntents(
        GET_UNCATEGORIZED,
        listOf(
            StaticIntent(
                description =
                    "Security & privacy: This intent opens the main security settings page.",
                intentUri =
                    "intent:#Intent;action=android.settings.SECURITY_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description =
                    "Device Unlock: This intent opens the settings page for device unlock options (e.g., PIN, password, fingerprint).",
                intentUri =
                    "intent:#Intent;action=android.intent.action.SAFETY_CENTER;S.android.safetycenter.extra.SAFETY_SOURCES_GROUP_ID=AndroidLockScreenSources;end",
            ),
            StaticIntent(
                description =
                    "System & Updates: This intent opens the settings page for system updates and security patches.",
                intentUri =
                    "intent:#Intent;action=android.intent.action.SAFETY_CENTER;S.android.safetycenter.extra.SAFETY_SOURCES_GROUP_ID=GoogleUpdateSources;end",
            ),
        ),
    )
