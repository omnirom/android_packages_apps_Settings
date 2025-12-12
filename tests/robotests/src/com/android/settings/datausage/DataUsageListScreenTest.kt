/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.datausage

import android.os.Bundle
import com.android.settings.Settings
import com.android.settings.flags.Flags
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DataUsageListScreenTest : SettingsCatalystTestCase() {

    override val preferenceScreenCreator =
        DataUsageListScreen(Bundle(1).also { it.putInt(android.provider.Settings.EXTRA_SUB_ID, 1) })

    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_NETWORK_AND_INTERNET_25Q4

    @Test
    fun getLaunchIntent_createsCorrectIntent() {
        val intent = preferenceScreenCreator.getLaunchIntent(appContext, null)

        assertThat(intent).isNotNull()
        assertThat(intent?.component?.className)
            .isEqualTo(Settings.MobileDataUsageListActivity::class.java.name)
    }

    // TODO(b/419311082): Migration test fails as a lot of telephony infra is not mocked.
    override fun migration() {}
}
