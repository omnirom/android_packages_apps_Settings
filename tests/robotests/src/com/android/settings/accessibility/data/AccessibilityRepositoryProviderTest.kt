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

package com.android.settings.accessibility.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AccessibilityRepositoryProviderTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
    }

    @Test
    fun get_returnsSameInstance() {
        val instance1 = AccessibilityRepositoryProvider.get(context)
        val instance2 = AccessibilityRepositoryProvider.get(context)
        assertThat(instance1).isSameInstanceAs(instance2)
    }

    @Test
    fun resetInstanceForTesting_allowsNewInstanceCreation() {
        val instance1 = AccessibilityRepositoryProvider.get(context)
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        val instance2 = AccessibilityRepositoryProvider.get(context)
        assertThat(instance1).isNotSameInstanceAs(instance2)
    }
}
