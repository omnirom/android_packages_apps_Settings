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
package com.android.settings.supervision

import android.text.Html
import android.view.View
import androidx.preference.Preference
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settings.widget.FooterPreferenceBinding
import com.android.settings.widget.FooterPreferenceMetadata
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.widget.FooterPreference
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Displays a footnote for Age of Consent (AoC) information. */
class SupervisionAocFooterPreference(
    private val preferenceDataProvider: PreferenceDataProvider,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : FooterPreferenceMetadata, FooterPreferenceBinding, PreferenceLifecycleProvider {
    override val key: String
        get() = KEY

    private var preferenceData: PreferenceData? = null

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val footerPreference = preference as FooterPreference
        val context = preference.context

        preference.isVisible = (preferenceData?.isVisible ?: false) && preferenceData?.title != null
        preference.title =
            Html.fromHtml(preferenceData?.title.toString(), Html.FROM_HTML_MODE_COMPACT)
        footerPreference.setIconVisibility(View.GONE)
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.lifecycleScope.launch {
            val cachedData = preferenceDataProvider.getCachedPreferenceData(listOf(KEY))[KEY]

            if (cachedData != null) {
                preferenceData = cachedData
                context.notifyPreferenceChange(KEY)
            }

            // Asynchronously fetch fresh data and update the UI
            val freshPreferenceData =
                withContext(coroutineDispatcher) {
                    preferenceDataProvider.getPreferenceData(listOf(KEY))[KEY]
                }

            if (preferenceData != freshPreferenceData) {
                preferenceData = freshPreferenceData
                context.notifyPreferenceChange(KEY)
            }
        }
    }

    companion object {
        const val KEY = "aoc_footer"
    }
}
