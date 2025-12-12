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
import android.util.Log
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateProviderExecutorResult
import com.android.settings.deviceinfo.imei.ImeiPreference
import com.android.settingslib.metadata.PersistentPreference
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.getPreferenceScreenTitle
import com.android.settingslib.metadata.getPreferenceSummary
import com.android.settingslib.metadata.getPreferenceTitle
import com.android.settingslib.spaprivileged.model.app.AppListRepositoryImpl
import com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateItem
import com.google.android.appfunctions.schema.common.v1.devicestate.LocalizedString
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeout

/* A [DeviceStateProvider] that provides device state information for Settings that are
exposed using Catalyst framework. Configured in [CatalystStateProviderConfig]. */
class CatalystStateProviderExecutor(
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
    ): DeviceStateProviderExecutorResult {
        // Cache the app list as it is used for multiple screens and is expensive to compute.
        AppListRepositoryImpl.useCaching = true
        try {
            val perScreenDeviceStatesList = mutableListOf<PerScreenDeviceStates>()
            coroutineScope {
                val semaphore = Semaphore(MAX_PARALLELISM)
                val deferredList =
                    screenKeyList.map { screenKey ->
                        async {
                            try {
                                withTimeout(PER_SCREEN_TIMEOUT_MS) {
                                    semaphore.withPermit {
                                        try {
                                            buildPerScreenDeviceStates(
                                                screenKey,
                                                appFunctionType,
                                                perScreenConfigMap[screenKey]?.additionalDescription,
                                            )
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
            return DeviceStateProviderExecutorResult(states = perScreenDeviceStatesList)
        } finally {
            // Disable caching for the next execution to avoid stale data.
            AppListRepositoryImpl.useCaching = false
        }
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStates(
        screenKey: String,
        appFunctionType: DeviceStateAppFunctionType,
        additionalDescription: String?,
    ): List<PerScreenDeviceStates> {
        Log.v(TAG, "Building per screen device states for $screenKey")
        val hierarchy = getEnabledPreferencesHierarchy(config, context, appFunctionType, screenKey)

        return hierarchy.map { entry ->
            val screenMetaData = entry.key
            val preferencesHierarchy = entry.value
            val states =
                buildPerScreenDeviceStates(
                    screenMetaData,
                    preferencesHierarchy,
                    additionalDescription,
                )
            Log.v(TAG, "Built per screen device states for $screenKey")
            states
        }
    }

    private suspend fun CoroutineScope.buildPerScreenDeviceStates(
        screenMetaData: PreferenceScreenMetadata,
        preferencesHierarchy: List<PreferenceHierarchyNode>,
        additionalDescription: String?,
    ): PerScreenDeviceStates {
        val deviceStateItemList = mutableListOf<DeviceStateItem>()
        preferencesHierarchy.forEach {
            val metadata = it.metadata
            val config = settingConfigMap[metadata.key]
            val jsonValue =
                when {
                    // TODO(b/444419242): Handle IME redaction properly.
                    isImePreference(metadata.key) -> "REDACTED"
                    metadata is PersistentPreference<*> ->
                        metadata
                            .storage(context)
                            .getValue(metadata.key, metadata.valueType as Class<Any>)
                            ?.toString()
                    else -> metadata.getPreferenceSummary(context)?.toString()
                }
            jsonValue?.let {
                deviceStateItemList.add(
                    DeviceStateItem(
                        // Binding key is either equal to the key or contains the package name or
                        // other item specific id necessary to distinguish the items.
                        key = metadata.bindingKey,
                        purpose = metadata.key,
                        name =
                            LocalizedString(
                                english = metadata.getPreferenceTitle(englishContext).toString(),
                                localized = metadata.getPreferenceTitle(context).toString(),
                            ),
                        jsonValue = it,
                        hintText = config?.hintText(englishContext, metadata),
                    )
                )
            }
        }

        // This is hack because in general parameters are not human readable. We remove known
        // internal keys then just dump the rest in the description.
        val basicDescription =
            (screenMetaData.getPreferenceScreenTitle(context)?.toString() ?: "") +
                (additionalDescription ?: "")
        val arguments = screenMetaData.arguments?.clone() as? BaseBundle
        arguments?.remove("source")
        val descriptionSuffix =
            if (arguments == null) {
                ""
            } else {
                ". " + arguments.keySet().joinToString(", ") { "$it=${arguments.get(it)}" }
            }
        val description = basicDescription + descriptionSuffix

        val launchingIntent = screenMetaData.getLaunchIntent(context, null)
        val states =
            PerScreenDeviceStates(
                description = description,
                deviceStateItems = deviceStateItemList,
                intentUri = launchingIntent?.toUri(Intent.URI_INTENT_SCHEME),
            )
        return states
    }

    private fun isImePreference(prefKey: String): Boolean {
        return prefKey.startsWith(ImeiPreference.KEY_PREFIX)
    }

    companion object {
        private const val TAG = "CatalystStateProviderExecutor"
        private const val MAX_PARALLELISM = 3
        private val PER_SCREEN_TIMEOUT_MS = 5.seconds
    }
}
