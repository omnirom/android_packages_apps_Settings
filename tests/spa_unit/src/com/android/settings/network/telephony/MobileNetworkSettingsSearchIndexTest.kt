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

package com.android.settings.network.telephony

import android.content.Context
import android.content.pm.PackageManager
import android.os.UserManager
import android.provider.Settings
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.Companion.isMobileNetworkSettingsSearchable
import com.android.settings.network.telephony.MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchResult
import com.android.settingslib.spa.search.SpaSearchLanding.BundleValue
import com.android.settingslib.spa.search.SpaSearchLanding.SpaSearchLandingFragment
import com.android.settingslib.spa.search.SpaSearchLanding.SpaSearchLandingKey
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class MobileNetworkSettingsSearchIndexTest {

    private val mockUserManager = mock<UserManager> { on { isAdminUser } doReturn true }

    private val mockPackageManager = mock<PackageManager>()

    private val mockSubscriptionManager =
        mock<SubscriptionManager> {
            on { activeSubscriptionInfoList } doReturn listOf(SUB_INFO_1, SUB_INFO_2)
        }

    private val context: Context =
        spy(ApplicationProvider.getApplicationContext()) {
            on { getSystemService(UserManager::class.java) } doReturn mockUserManager
            on { getSystemService(SubscriptionManager::class.java) } doReturn
                mockSubscriptionManager
            on { packageManager } doReturn mockPackageManager
        }

    private val resources =
        spy(context.resources) { on { getBoolean(R.bool.config_show_sim_info) } doReturn true }

    private val mobileNetworkSettingsSearchIndex = MobileNetworkSettingsSearchIndex {
        listOf(
            object : MobileNetworkSettingsSearchIndex.MobileNetworkSettingsSearchItem {
                override fun getSearchResult(subId: Int): MobileNetworkSettingsSearchResult? =
                    if (subId == SUB_ID_1) {
                        MobileNetworkSettingsSearchResult(key = KEY, title = TITLE)
                    } else {
                        null
                    }
            })
    }

    @Before
    fun setUp() {
        context.stub { on { resources } doReturn resources }

        // By default, searchable
        mockUserManager.stub { on { isAdminUser } doReturn true }
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn true
        }
    }

    @Test
    fun isMobileNetworkSettingsSearchable_default_returnTrue() {
        // Use defaults
        val isSearchable = isMobileNetworkSettingsSearchable(context)

        assertThat(isSearchable).isTrue()
    }

    @Test
    fun isMobileNetworkSettingsSearchable_nonAdminUser_returnFalse() {
        mockUserManager.stub { on { isAdminUser } doReturn false }

        val isSearchable = isMobileNetworkSettingsSearchable(context)

        assertThat(isSearchable).isFalse()
    }

    @Test
    fun isMobileNetworkSettingsSearchable_noTelephony_returnFalse() {
        mockPackageManager.stub {
            on { hasSystemFeature(PackageManager.FEATURE_TELEPHONY) } doReturn false
        }

        val isSearchable = isMobileNetworkSettingsSearchable(context)

        assertThat(isSearchable).isFalse()
    }

    @Test
    fun getSearchIndexablePage() {
        val searchIndexablePage = mobileNetworkSettingsSearchIndex.getSearchIndexablePage()

        assertThat(searchIndexablePage.targetClass).isEqualTo(MobileNetworkSettings::class.java)
        val items = searchIndexablePage.itemsProvider(context)
        assertThat(items).hasSize(1)
        val item = items.single()
        assertThat(item.searchLandingKey)
            .isEqualTo(
                SpaSearchLandingKey.newBuilder()
                    .setFragment(
                        SpaSearchLandingFragment.newBuilder()
                            .setFragmentName(MobileNetworkSettings::class.java.name)
                            .setPreferenceKey(KEY)
                            .putArguments(
                                Settings.EXTRA_SUB_ID,
                                BundleValue.newBuilder().setIntValue(SUB_ID_1).build()))
                    .build())
        assertThat(item.pageTitle).isEqualTo("SIMs > $SUB_DISPLAY_NAME_1")
        assertThat(item.itemTitle).isEqualTo(TITLE)
    }

    private companion object {
        const val KEY = "key"
        const val TITLE = "Title"
        const val SUB_ID_1 = 1
        const val SUB_ID_2 = 2
        const val SUB_DISPLAY_NAME_1 = "Sub 1"
        const val SUB_DISPLAY_NAME_2 = "Sub 2"

        val SUB_INFO_1: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_1)
                    setDisplayName(SUB_DISPLAY_NAME_1)
                }
                .build()

        val SUB_INFO_2: SubscriptionInfo =
            SubscriptionInfo.Builder()
                .apply {
                    setId(SUB_ID_2)
                    setDisplayName(SUB_DISPLAY_NAME_2)
                }
                .build()
    }
}
