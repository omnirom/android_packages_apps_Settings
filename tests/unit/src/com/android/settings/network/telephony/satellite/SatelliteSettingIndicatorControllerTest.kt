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
package com.android.settings.network.telephony.satellite

import android.content.Context
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.CarrierConfigManager.SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceManager
import androidx.test.annotation.UiThreadTest
import androidx.test.core.app.ApplicationProvider
import com.android.settings.testutils.ResourcesUtils
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.kotlin.spy

@UiThreadTest
class SatelliteSettingIndicatorControllerTest {
    private var mContext: Context? = null
    private var mController: SatelliteSettingIndicatorController? = null
    private val mCarrierConfig = PersistableBundle()

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
        mController = SatelliteSettingIndicatorController(mContext, KEY)
    }

    @Test
    fun updateHowItWorksContent_accountNotEligible_categoryIsDisabled() {
        mCarrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )
        mController?.init(TEST_SUB_ID, mCarrierConfig)
        val preferenceManager = PreferenceManager(mContext!!)
        val preferenceScreen = preferenceManager.createPreferenceScreen(mContext!!)
        val category = spy(PreferenceCategory(mContext!!))
        category.setKey(
            SatelliteSettingIndicatorController.Companion.PREF_KEY_CATEGORY_HOW_IT_WORKS
        )
        category.title = "test title"
        category.isEnabled = true
        val preference = spy(Preference(mContext!!))
        preference.setKey(SatelliteSettingIndicatorController.Companion.KEY_SUPPORTED_SERVICE)
        preference.title = "preference"
        preferenceScreen.addPreference(category)
        preferenceScreen.addPreference(preference)

        mController?.updateHowItWorksContent(preferenceScreen, false)

        Mockito.verify(category, Mockito.times(1)).isEnabled = false
        Mockito.verify(category, Mockito.times(1)).shouldDisableView = true
    }

    @Test
    fun updateHowItWorksContent_accountEligible_categoryIsEnabled() {
        mCarrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )
        mController?.init(TEST_SUB_ID, mCarrierConfig)
        val preferenceManager = PreferenceManager(mContext!!)
        val preferenceScreen = preferenceManager.createPreferenceScreen(mContext!!)
        val category = Mockito.spy<PreferenceCategory>(PreferenceCategory(mContext!!))
        category.setKey(
            SatelliteSettingIndicatorController.Companion.PREF_KEY_CATEGORY_HOW_IT_WORKS
        )
        category.title = "test title"
        category.isEnabled = true
        val preference = spy(Preference(mContext!!))
        preference.setKey(SatelliteSettingIndicatorController.Companion.KEY_SUPPORTED_SERVICE)
        preference.title = "preference"
        preferenceScreen.addPreference(category)
        preferenceScreen.addPreference(preference)

        mController!!.updateHowItWorksContent(preferenceScreen, true)

        Mockito.verify(category, Mockito.times(0)).isEnabled = false
        Mockito.verify(category, Mockito.times(0)).shouldDisableView = true
    }

    @Test
    fun updateHowItWorksContent_dataAvailable_summaryChanged() {
        mCarrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_AUTOMATIC,
        )
        mController?.init(TEST_SUB_ID, mCarrierConfig)
        mController?.setCarrierRoamingNtnAvailability(
            true,
            true,
            SATELLITE_DATA_SUPPORT_BANDWIDTH_CONSTRAINED,
        )
        val preferenceManager = PreferenceManager(mContext!!)
        val preferenceScreen = preferenceManager.createPreferenceScreen(mContext!!)
        val category = spy(PreferenceCategory(mContext!!))
        category.setKey(
            SatelliteSettingIndicatorController.Companion.PREF_KEY_CATEGORY_HOW_IT_WORKS
        )
        category.title = "test title"
        category.isEnabled = true
        val preference1 = spy(Preference(mContext!!))
        val preference2 = Preference(mContext!!)
        preference1.setKey(
            SatelliteSettingIndicatorController.Companion.KEY_SATELLITE_CONNECTION_GUIDE
        )
        preference1.title = "preference"
        preference2.setKey(SatelliteSettingIndicatorController.Companion.KEY_SUPPORTED_SERVICE)
        preference2.title = "preference2"

        preferenceScreen.addPreference(category)
        preferenceScreen.addPreference(preference1)
        preferenceScreen.addPreference(preference2)

        mController!!.updateHowItWorksContent(preferenceScreen, true)

        assertThat(preference2.summary)
            .isEqualTo(
                ResourcesUtils.getResourcesString(
                    mContext,
                    "summary_supported_service_with_constrained_data",
                )
            )
    }

    @Test
    fun updateHowItWorksContent_ntnConnectIsManual_summaryChanged() {
        mCarrierConfig.putInt(
            CarrierConfigManager.KEY_CARRIER_ROAMING_NTN_CONNECT_TYPE_INT,
            CarrierConfigManager.CARRIER_ROAMING_NTN_CONNECT_MANUAL,
        )
        mController?.init(TEST_SUB_ID, mCarrierConfig)
        val preferenceManager = PreferenceManager(mContext!!)
        val preferenceScreen = preferenceManager.createPreferenceScreen(mContext!!)
        val category = Mockito.spy<PreferenceCategory>(PreferenceCategory(mContext!!))
        category.setKey(
            SatelliteSettingIndicatorController.Companion.PREF_KEY_CATEGORY_HOW_IT_WORKS
        )
        category.title = "test title"
        val preference1 = Mockito.spy<Preference>(Preference(mContext!!))
        preference1.setKey(
            SatelliteSettingIndicatorController.Companion.KEY_SATELLITE_CONNECTION_GUIDE
        )
        preference1.title = "preference1"
        val preference2 = Mockito.spy<Preference>(Preference(mContext!!))
        preference2.setKey(SatelliteSettingIndicatorController.Companion.KEY_SUPPORTED_SERVICE)
        preference2.title = "preference2"
        preferenceScreen.addPreference(category)
        preferenceScreen.addPreference(preference1)
        preferenceScreen.addPreference(preference2)

        mController?.updateHowItWorksContent(preferenceScreen, false)

        Mockito.verify(preference1).setSummary(ArgumentMatchers.any<CharSequence?>())
        Mockito.verify(preference2).setSummary(ArgumentMatchers.any<CharSequence?>())
    }

    companion object {
        private const val KEY = "SatelliteSettingIndicatorControllerTest"
        private const val TEST_SUB_ID = 5
    }
}
