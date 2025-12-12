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
import android.content.Context
import android.content.Intent
import android.os.BaseBundle
import android.os.Binder
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateMetadataProviderExecutorResult
import com.android.settingslib.graph.PreferenceGetterFlags
import com.android.settingslib.graph.toProto
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceTitle
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItemMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenMetadata
import com.google.android.appfunctions.schema.common.v1.devicestate.Sensitivity
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/* A [DeviceStateExecutor] that provides device state metadata information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateMetadataProviderExecutor(
    val config: CatalystConfig,
    private val context: Context,
    private val englishContext: Context,
) : DeviceStateExecutor {
    private val settingConfigMap = config.deviceStateItems.associateBy { it.settingKey }
    private val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    private val screenKeyList = perScreenConfigMap.keys.toList()

    override suspend fun execute(
        appFunctionType: DeviceStateAppFunctionType,
        params: GenericDocument?,
    ): DeviceStateMetadataProviderExecutorResult {
        val perScreenDeviceStatesList = mutableListOf<PerScreenMetadata>()
        coroutineScope {
            val semaphore = Semaphore(MAX_PARALLELISM)
            val deferredList =
                screenKeyList.map { screenKey ->
                    async {
                        try {
                            withTimeout(PER_SCREEN_TIMEOUT_MS) {
                                semaphore.withPermit {
                                    try {
                                        buildPerScreenDeviceStatesMetadata(screenKey)
                                    } catch (e: Exception) {
                                        Log.e(TAG, "error building $screenKey", e)
                                        null
                                    }
                                }
                            }
                        } catch (e: TimeoutCancellationException) {
                            Log.e(TAG, "Timed out building screen: $screenKey", e)
                            null
                        }
                    }
                }
            val results = deferredList.awaitAll()
            perScreenDeviceStatesList.addAll(results.filterNotNull().flatten())
        }
        return DeviceStateMetadataProviderExecutorResult(states = perScreenDeviceStatesList)
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStatesMetadata(
        screenKey: String
    ): List<PerScreenMetadata> {
        val hierarchy =
            getEnabledPreferencesHierarchy(config, context, appFunctionType = null, screenKey)

        return hierarchy.map { entry ->
            val screenMetaData = entry.key
            val preferencesHierarchy = entry.value
            buildPerScreenDeviceStatesMetadata(screenMetaData, preferencesHierarchy)
        }
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStatesMetadata(
        screenMetaData: PreferenceScreenMetadata,
        preferencesHierarchy: List<PreferenceHierarchyNode>,
    ): PerScreenMetadata {
        val deviceStateItemMetadataList = mutableListOf<DeviceStateItemMetadata>()
        preferencesHierarchy.forEach {
            val metadata = it.metadata
            val config = settingConfigMap[metadata.key]
            // skip over explicitly disabled preferences
            val metadataProto =
                metadata.toProto(
                    context,
                    Binder.getCallingPid(),
                    Binder.getCallingUid(),
                    screenMetaData,
                    isRoot = false,
                    flags = PreferenceGetterFlags.METADATA or PreferenceGetterFlags.VALUE_DESCRIPTOR,
                )

            val sensitivityLevel =
                when (metadataProto.sensitivityLevel) {
                    SensitivityLevel.LOW_SENSITIVITY -> Sensitivity.MUST_PROVIDE_UNDO
                    SensitivityLevel.MEDIUM_SENSITIVITY -> Sensitivity.REQUIRES_CONFIRMATION
                    else -> null
                }
            deviceStateItemMetadataList.add(
                DeviceStateItemMetadata(
                    // TODO: Expose parameterization
                    key = metadataProto.key,
                    purpose = metadataProto.key,
                    name =
                        LocalizedString(
                            english = metadata.getPreferenceTitle(englishContext).toString(),
                            localized = metadata.getPreferenceTitle(context).toString(),
                        ),
                    sensitivity = sensitivityLevel,
                    writable = metadataProto.readWritePermit == ReadWritePermit.ALLOW,
                    // TODO: properly expose possible values
                    possibleValues = metadataProto.valueDescriptor.toString(),
                    hintText = config?.hintText(englishContext, metadata),
                )
            )
        }

        val launchingIntent = screenMetaData.getLaunchIntent(context, null)

        // This is hack because in general parameters are not human readable. We remove known
        // internal keys then just dump the rest in the description.
        val basicDescription = screenMetaData.getPreferenceScreenTitle(context)?.toString() ?: ""
        val arguments = screenMetaData.arguments?.clone() as? BaseBundle
        arguments?.remove("source")
        val descriptionSuffix =
            if (arguments == null) {
                ""
            } else {
                ". " + arguments.keySet().joinToString(", ") { "$it=${arguments.get(it)}" }
            }
        val description = basicDescription + descriptionSuffix
        return PerScreenMetadata(
            description = description,
            deviceStateItemsMetadata = deviceStateItemMetadataList,
            intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME),
        )
    }

    companion object {
        private const val TAG = "CatalystStateMetadataProviderExecutor"
        private const val MAX_PARALLELISM = 3
        private val PER_SCREEN_TIMEOUT_MS = 5.seconds
    }
}
