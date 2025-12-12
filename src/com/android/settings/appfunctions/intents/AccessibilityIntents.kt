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

fun getAccessibilityIntents() =
    StaticIntents(
        GET_UNCATEGORIZED,
        listOf(
            StaticIntent(
                description = "Accessibility: Intent to open Accessibility settings.",
                intentUri =
                    "intent:#Intent;action=android.settings.ACCESSIBILITY_SETTINGS;package=com.android.settings;end",
            ),
            StaticIntent(
                description = "Talkback: Intent to open Talkback accessibility service settings.",
                intentUri =
                    "intent:#Intent;action=android.settings.ACCESSIBILITY_DETAILS_SETTINGS;S.android.intent.extra.COMPONENT_NAME=com.google.android.marvin.talkback/.TalkBackService;end",
            ),
            StaticIntent(
                description =
                    "Select to Speak: Intent to open Select to Speak accessibility service settings.",
                intentUri =
                    "intent:#Intent;action=android.settings.ACCESSIBILITY_DETAILS_SETTINGS;S.android.intent.extra.COMPONENT_NAME=com.google.android.marvin.talkback/com.google.android.accessibility.selecttospeak.SelectToSpeakService;end",
            ),
            StaticIntent(
                description =
                    "Live Transcribe: Intent to open Live Transcribe accessibility service settings.",
                intentUri =
                    "intent:#Intent;action=android.settings.ACCESSIBILITY_DETAILS_SETTINGS;S.android.intent.extra.COMPONENT_NAME=com.google.audio.hearing.visualization.accessibility.scribe/.MainActivity;end",
            ),
        ),
    )
