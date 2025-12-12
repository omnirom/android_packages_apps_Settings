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
package com.android.settings.network

import android.content.pm.PackageManager.FEATURE_TELEPHONY
import android.platform.test.annotations.DisableFlags
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.mock
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSubscriptionManager

class MobileNetworkListScreenTest : SettingsCatalystTestCase() {
    override val preferenceScreenCreator = MobileNetworkListScreen(appContext)

    override val flagName: String
        get() = Flags.FLAG_CATALYST_MOBILE_NETWORK_LIST

    @DisableFlags(
        Flags.FLAG_IS_DUAL_SIM_ONBOARDING_ENABLED,
        Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4,
    )
    @Ignore("UI of MobileNetworkListScreen is replaced by SPA now.")
    @Config(shadows = [ShadowSubscriptionManager::class])
    @Test
    override fun migration() {
        val subscriptionManager =
            shadowOf(appContext.getSystemService(SubscriptionManager::class.java))
        val subscriptionInfo: SubscriptionInfo = mock()
        subscriptionManager.setAvailableSubscriptionInfos(subscriptionInfo)
        // make screen available
        shadowOf(appContext.packageManager).setSystemFeature(FEATURE_TELEPHONY, true)
        super.migration()
    }
}
