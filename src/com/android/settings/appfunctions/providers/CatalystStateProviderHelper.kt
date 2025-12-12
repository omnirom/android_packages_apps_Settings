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

import android.content.Context
import com.android.settings.appfunctions.CatalystConfig
import com.android.settings.appfunctions.DeviceStateAppFunctionType
import com.android.settings.appfunctions.DeviceStateItemConfig
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceHierarchyNode
import com.android.settingslib.metadata.PreferenceScreenCoordinate
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceScreenRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.toList

/**
 * A generic helper function to process preferences for a given screen. It handles the common logic
 * of fetching, validating, and iterating over preferences.
 *
 * @param screenKey The key of the preference screen to process.
 * @param additionalConfigCheck An optional lambda to perform extra validation on the screen's
 *   config.
 * @param itemProcessor A suspendable lambda that takes a preference's metadata and config, and
 *   returns a processed item of type T, or null to skip it.
 * @return A Pair containing the screen's metadata and the list of processed items, or null if the
 *   screen is invalid or disabled.
 */
suspend fun CoroutineScope.getEnabledPreferencesHierarchy(
    config: CatalystConfig,
    context: Context,
    appFunctionType: DeviceStateAppFunctionType? = null,
    screenKey: String,
): Map<PreferenceScreenMetadata, List<PreferenceHierarchyNode>> {
    val settingConfigMap = config.deviceStateItems.associateBy { it.settingKey }
    val perScreenConfigMap = config.screenConfigs.associateBy { it.screenKey }
    val perScreenConfig = perScreenConfigMap[screenKey]
    if (
        perScreenConfig == null ||
            !perScreenConfig.enabled ||
            (appFunctionType != null && appFunctionType !in perScreenConfig.appFunctionTypes)
    ) {
        return mapOf()
    }

    val hierarchies =
        if (PreferenceScreenRegistry.isParameterized(context, screenKey)) {
            PreferenceScreenRegistry.getParameters(context, screenKey).toList().map {
                getPreferenceHierarchy(
                    context,
                    PreferenceScreenCoordinate(screenKey, it),
                    settingConfigMap,
                )
            }
        } else {
            listOf(
                getPreferenceHierarchy(
                    context,
                    PreferenceScreenCoordinate(screenKey, null),
                    settingConfigMap,
                )
            )
        }

    return hierarchies.filterNotNull().toMap()
}

private suspend fun CoroutineScope.getPreferenceHierarchy(
    context: Context,
    coordinate: PreferenceScreenCoordinate,
    settingConfigMap: Map<String, DeviceStateItemConfig>,
): Pair<PreferenceScreenMetadata, List<PreferenceHierarchyNode>>? {
    val screenMetaData = PreferenceScreenRegistry.create(context, coordinate) ?: return null
    if (screenMetaData is PreferenceAvailabilityProvider && !screenMetaData.isAvailable(context)) {
        return null
    }
    val preferenceHierarchy = mutableListOf<PreferenceHierarchyNode>()
    // TODO if child node is PreferenceScreen, recursively process it
    screenMetaData.getPreferenceHierarchy(context, this).forEachRecursivelyAsync {
        val metadata = it.metadata
        val config = settingConfigMap[metadata.key]
        // Skip over explicitly disabled preferences
        if (config?.enabled == false) return@forEachRecursivelyAsync

        preferenceHierarchy.add(it)
    }
    return screenMetaData to preferenceHierarchy
}
