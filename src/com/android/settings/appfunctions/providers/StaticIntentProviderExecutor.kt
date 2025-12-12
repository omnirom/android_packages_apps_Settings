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

package com.android.settings.appfunctions.providers

import android.app.appsearch.GenericDocument
import androidx.annotation.Keep
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateProviderExecutorResult
import com.android.settings.appfunctions.intents.getAccessibilityIntents
import com.android.settings.appfunctions.intents.getAppsIntents
import com.android.settings.appfunctions.intents.getModesIntents
import com.android.settings.appfunctions.intents.getNotificationsIntents
import com.android.settings.appfunctions.intents.getOtherIntents
import com.android.settings.appfunctions.intents.getSecurityIntents
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates

/** A simple data class to represent a static Intent URI + description. */
@Keep data class StaticIntent(val description: String, val intentUri: String)

/**
 * A data class to store a [StaticIntent] list and the [DeviceStateAppFunctionType] this list is
 * associated with.
 */
@Keep
data class StaticIntents(
    val appFunctionType: DeviceStateAppFunctionType,
    val intents: List<StaticIntent>,
)

/**
 * A [DeviceStateExecutor] that provides a static list of device states from a list of
 * [StaticIntent]s.
 *
 * This provider is useful for adding simple, non-dynamic states that consist only of a description
 * and an intent URI. It will only provide states if the [requestAppFunctionType] matches the app
 * function this provider is configured with.
 *
 * @param staticIntents The list of [StaticIntents] to provide.
 */
@Keep
class StaticIntentProviderExecutor(val staticIntents: List<StaticIntents>) : DeviceStateExecutor {
    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateProviderExecutorResult {
        val states =
            staticIntents
                .filter { appFunctionType == it.appFunctionType }
                .flatMap { staticIntents: StaticIntents ->
                    staticIntents.intents.map {
                        PerScreenDeviceStates(
                            description = it.description,
                            intentUri = it.intentUri,
                        )
                    }
                }
        return DeviceStateProviderExecutorResult(states)
    }
}

fun getAllIntents(): List<StaticIntents> =
    listOf(
        getAppsIntents(),
        getNotificationsIntents(),
        getModesIntents(),
        getSecurityIntents(),
        getAccessibilityIntents(),
        getOtherIntents(),
    )
