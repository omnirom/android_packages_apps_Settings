/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.network

import android.app.settings.SettingsEnums
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.preference.Preference
import com.android.settings.R
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.network.telephony.SimRepository
import com.android.settings.network.telephony.euicc.EuiccRepository
import com.android.settings.search.BaseSearchIndexProvider
import com.android.settings.spa.SpaActivity.Companion.startSpaActivity
import com.android.settings.spa.network.NetworkCellularGroupProvider
import com.android.settingslib.search.SearchIndexable

@SearchIndexable(forTarget = SearchIndexable.ALL and SearchIndexable.ARC.inv())
class MobileNetworkListFragment : DashboardFragment() {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onCreate(icicle: Bundle?) {
        super.onCreate(icicle)

        if (Flags.isDualSimOnboardingEnabled()) {
            context?.startSpaActivity(
                destination = NetworkCellularGroupProvider.fileName,
                highlightItemKey = arguments?.getString(EXTRA_FRAGMENT_ARG_KEY),
            )
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Disable the animation of the preference list
        listView.itemAnimator = null

        findPreference<Preference>(KEY_ADD_SIM)!!.isVisible =
            EuiccRepository(requireContext()).showEuiccSettings()
    }

    override fun getPreferenceScreenResId() = R.xml.network_provider_sims_list

    override fun getLogTag() = LOG_TAG

    override fun getMetricsCategory() = SettingsEnums.MOBILE_NETWORK_LIST

    companion object {
        private const val LOG_TAG = "NetworkListFragment"
        private const val KEY_ADD_SIM = "add_sim"

        @JvmField val SEARCH_INDEX_DATA_PROVIDER = SearchIndexProvider()

        @VisibleForTesting
        class SearchIndexProvider(
            private val simRepositoryFactory: (Context) -> SimRepository = ::SimRepository
        ) : BaseSearchIndexProvider(R.xml.network_provider_sims_list) {
            public override fun isPageSearchEnabled(context: Context): Boolean =
                simRepositoryFactory(context).canEnterMobileNetworkPage()
        }
    }

    override fun getPreferenceScreenBindingKey(context: Context) = MobileNetworkListScreen.KEY
}
