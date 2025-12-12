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
package com.android.settings.safetycenter.ui

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.LifecycleOwner
import com.android.settings.R
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModel
import com.android.settings.safetycenter.ui.model.LiveSafetyCenterViewModelFactory
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.search.SearchIndexable

/**
 * Fragment for the Safety Center UI.
 *
 * This fragment hosts the preferences for the Security & privacy settings page and is searchable
 * when the feature flag is enabled.
 */
@SearchIndexable
class SafetyCenterFragment : DashboardFragment() {

    private val viewModel: LiveSafetyCenterViewModel by viewModels {
        LiveSafetyCenterViewModelFactory(requireActivity().application)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSubpagePreferenceControllers(viewLifecycleOwner)
    }

    private fun setupSubpagePreferenceControllers(owner: LifecycleOwner) {
        Log.d(TAG, "Setting Up the sub-page preference controllers")
        val allControllers: List<AbstractPreferenceController> = preferenceControllers.flatten()

        for (controller in allControllers) {
            if (controller is SubpagePreferenceController) {
                when (controller.preferenceKey) {
                    DEVICE_UNLOCK_SUBPAGE_KEY -> {
                        controller.setRelatedSafetySources(DEVICE_UNLOCK_SAFETY_SOURCE_IDS)
                        controller.setRelatedIssueOnlySafetySources(emptyList())
                        controller.setDefaultSummaryResId(
                            R.string.device_unlock_subpage_default_summary
                        )
                        controller.setViewModelAndLifecycle(viewModel, owner)
                    }
                }
            }
        }
    }

    protected override fun getPreferenceScreenResId(): Int {
        return R.xml.safety_center_main_page
    }

    override fun getLogTag(): String {
        return TAG
    }

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SAFETY_CENTER
    }

    companion object {
        private const val TAG = "SafetyCenterFragment"
        private const val ANDROID_LOCK_SCREEN_SOURCE_ID = "AndroidLockScreen"
        private const val DEVICE_UNLOCK_SUBPAGE_KEY = "device_unlock_subpage"
        private val DEVICE_UNLOCK_SAFETY_SOURCE_IDS = listOf(ANDROID_LOCK_SCREEN_SOURCE_ID)

        @JvmField
        val SEARCH_INDEX_DATA_PROVIDER: BaseSearchIndexProvider =
            object : BaseSearchIndexProvider(R.xml.safety_center_main_page) {
                protected override fun isPageSearchEnabled(context: Context?): Boolean {
                    return Flags.enableSafetyCenterNewUi()
                }
            }
    }
}
