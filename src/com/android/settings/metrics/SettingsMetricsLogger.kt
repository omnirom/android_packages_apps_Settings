/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.metrics

import android.content.Context
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.core.instrumentation.Instrumentable
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceScreenMetadata
import com.android.settingslib.metadata.PreferenceUiActionMetricsLogger

/** [PreferenceUiActionMetricsLogger] of Settings app. */
class SettingsMetricsLogger
@JvmOverloads
constructor(
    private val context: Context,
    private val metricsFeatureProvider: MetricsFeatureProvider =
        FeatureFactory.featureFactory.metricsFeatureProvider,
) : PreferenceUiActionMetricsLogger {

    override fun logPreferenceValueChange(
        screen: PreferenceScreenMetadata,
        preference: PreferenceMetadata,
        value: Any?,
    ) {
        val metricsCategory =
            (screen as? Instrumentable)?.metricsCategory ?: Instrumentable.METRICS_CATEGORY_UNKNOWN
        val intValue =
            when (value) {
                is Boolean -> if (value == true) 1 else 0
                is Int -> value
                else -> return
            }
        metricsFeatureProvider.changed(metricsCategory, preference.key, intValue)
    }
}
