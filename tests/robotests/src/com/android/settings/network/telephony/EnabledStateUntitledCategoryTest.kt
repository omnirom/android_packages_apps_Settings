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

import android.content.ContextWrapper
import android.telephony.SubscriptionManager
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

@RunWith(AndroidJUnit4::class)
class EnabledStateUntitledCategoryTest {
    private val mockSubscriptionManager = mock<SubscriptionManager>()

    private val context =
        object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getSystemService(name: String): Any? =
                when (name) {
                    getSystemServiceName(SubscriptionManager::class.java) -> mockSubscriptionManager
                    else -> super.getSystemService(name)
                }
        }

    val enabledStateUntitledCategory = EnabledStateUntitledCategory(TEST_SUBID)

    @Test
    fun isAvailable_subscriptionIsActive_returnsTrue() {
        mockSubscriptionManager.stub { on { isActiveSubscriptionId(TEST_SUBID) } doReturn true }

        assertThat(enabledStateUntitledCategory.isAvailable(context)).isTrue()
    }

    @Test
    fun isAvailable_subscriptionIsInactive_returnsFalse() {
        mockSubscriptionManager.stub { on { isActiveSubscriptionId(TEST_SUBID) } doReturn false }

        assertThat(enabledStateUntitledCategory.isAvailable(context)).isFalse()
    }

    companion object {
        const val TEST_SUBID = 1
    }
}
