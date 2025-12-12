/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.spa.search

import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex
import com.android.settingslib.search.SearchIndexableData
import com.android.settingslib.spa.search.SearchIndexableDataConverter
import com.android.settingslib.spa.search.SpaSearchRepository

class SettingsSpaSearchRepository() {
    private val spaSearchRepository = SpaSearchRepository()
    private val searchIndexableDataConverter =
        SearchIndexableDataConverter(
            intentAction = SEARCH_LANDING_ACTION,
            intentTargetClass = SettingsSpaSearchLandingActivity::class.qualifiedName!!,
        )

    fun getSearchIndexableDataList(): List<SearchIndexableData> {
        val pages =
            spaSearchRepository.getSearchIndexablePageList() +
                MobileNetworkSettingsSearchIndex().getSearchIndexablePage()
        return pages.map(searchIndexableDataConverter::toSearchIndexableData)
    }

    companion object {
        private const val SEARCH_LANDING_ACTION = "android.settings.SPA_SEARCH_LANDING"
    }
}
