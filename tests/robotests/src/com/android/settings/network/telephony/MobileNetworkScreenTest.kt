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

package com.android.settings.network.telephony

import android.os.Bundle
import android.provider.Settings
import com.android.settings.Settings.MobileNetworkActivity
import com.android.settings.SettingsActivity.EXTRA_FRAGMENT_ARG_KEY
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.android.settingslib.metadata.PreferenceMetadata
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class MobileNetworkScreenTest : SettingsCatalystTestCase() {
    private val subId = 123

    override val preferenceScreenCreator =
        MobileNetworkScreen(Bundle().apply { putInt(Settings.EXTRA_SUB_ID, subId) })

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    @Test
    fun getKey_returnsCorrectKey() {
        assertThat(preferenceScreenCreator.key).isEqualTo(MobileNetworkScreen.KEY)
    }

    @Test
    fun getBindingKey_returnsCorrectBindingKey() {
        assertThat(preferenceScreenCreator.bindingKey).isEqualTo("${MobileNetworkScreen.KEY}-123")
    }

    @Test
    fun getLaunchIntent_returnsIntentWithSubId() {
        val prefKey = "fakePrefKey"
        val mockPrefMetadata = mock<PreferenceMetadata> { on { bindingKey } doReturn prefKey }
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, mockPrefMetadata)

        assertThat(intent).isNotNull()
        assertThat(intent!!.component?.className).isEqualTo(MobileNetworkActivity::class.java.name)
        assertThat(intent.getIntExtra(Settings.EXTRA_SUB_ID, -1)).isEqualTo(subId)
        assertThat(intent.hasExtra(EXTRA_FRAGMENT_ARG_KEY)).isTrue()
        assertThat(intent.getStringExtra(EXTRA_FRAGMENT_ARG_KEY)).isEqualTo(prefKey)
    }

    // TODO(b/419310279): Migration test fails when instantiating a BillingCycleRepository due to a
    // null networkService -- some setup needs to be added to provide the necessary interfaces.
    @Test override fun migration() {}
}
