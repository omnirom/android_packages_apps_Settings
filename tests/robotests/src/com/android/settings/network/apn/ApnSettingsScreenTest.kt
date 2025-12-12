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

package com.android.settings.network.apn

import android.content.ContextWrapper
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.CarrierConfigManager
import android.telephony.TelephonyManager
import android.widget.TextView
import androidx.test.core.app.ApplicationProvider
import com.android.settings.Settings.ApnSettingsActivity
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.network.CarrierConfigCache
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

class ApnSettingsScreenTest : SettingsCatalystTestCase() {
    private val subId = 42

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    override val preferenceScreenCreator =
        ApnSettingsScreen(Bundle().apply { putInt(ApnSettings.SUB_ID, subId) })

    private val mockTelephonyManager = mock<TelephonyManager>()

    private val mockCarrierConfigCache = mock<CarrierConfigCache>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getSystemService(name: String): Any =
                when (name) {
                    TELEPHONY_SERVICE -> mockTelephonyManager
                    else -> super.getSystemService(name)
                }
        }

    @Before
    fun setup() {
        mockTelephonyManager.stub {
            on { createForSubscriptionId(subId) } doReturn mockTelephonyManager
        }
        CarrierConfigCache.setTestInstance(context, mockCarrierConfigCache)
    }

    @Test
    fun key_isEqualToStatic() {
        assertThat(preferenceScreenCreator.key).isEqualTo(ApnSettingsScreen.KEY)
    }

    @Test
    fun getLaunchIntent_correctActivity() {
        val underTest = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(underTest.component?.className)
            .isEqualTo(ApnSettingsActivity::class.java.getName())
    }

    @Test
    fun isAvailable_apnSettingsSupportedWithGsm_returnTrue() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true)
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn bundle }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(true)
    }

    @Test
    fun isAvailable_carrierConfigNull_returnFalse() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn null }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    fun isAvailable_hideCarrierNetworkSettings_returnFalse() {
        mockTelephonyManager.stub { on { phoneType } doReturn TelephonyManager.PHONE_TYPE_GSM }
        val bundle = PersistableBundle()
        bundle.putBoolean(CarrierConfigManager.KEY_APN_EXPAND_BOOL, true)
        bundle.putBoolean(CarrierConfigManager.KEY_HIDE_CARRIER_NETWORK_SETTINGS_BOOL, true)
        mockCarrierConfigCache.stub { on { getConfigForSubId(subId) } doReturn bundle }

        assertThat(preferenceScreenCreator.isAvailable(context)).isEqualTo(false)
    }

    @Test
    @Config(shadows = [ShadowRestrictedDashboardFragment::class])
    override fun migration() {
        super.migration()
    }
}

@Implements(RestrictedDashboardFragment::class)
class ShadowRestrictedDashboardFragment {
    @Implementation
    fun getEmptyTextView(): TextView? {
        return TextView(ApplicationProvider.getApplicationContext())
    }
}
