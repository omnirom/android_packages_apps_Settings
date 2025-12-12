/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.appfunctions.providers

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.appfunctions.DeviceStateAppFunctionType.GET_STORAGE
import com.android.settings.appfunctions.DeviceStateAppFunctionType.GET_UNCATEGORIZED
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaticIntentProviderTest {

    @Test
    fun execute_whenCategoryMatches_returnsStates() = runTest {
        // Arrange
        val staticIntents =
            listOf(
                StaticIntents(
                    GET_UNCATEGORIZED,
                    listOf(
                        StaticIntent("description1", "intentUri1"),
                        StaticIntent("description2", "intentUri2"),
                    ),
                )
            )
        val provider = StaticIntentProviderExecutor(staticIntents)

        // Act
        val result = provider.execute(GET_UNCATEGORIZED)

        // Assert
        assertThat(result.states).hasSize(2)
        assertThat(result.states[0].description).isEqualTo("description1")
        assertThat(result.states[0].intentUri).isEqualTo("intentUri1")
        assertThat(result.states[1].description).isEqualTo("description2")
        assertThat(result.states[1].intentUri).isEqualTo("intentUri2")
        assertThat(result.hintText).isNull()
    }

    @Test
    fun execute_whenCategoryDoesNotMatch_returnsEmptyList() = runTest {
        // Arrange
        val staticIntents =
            listOf(StaticIntents(GET_STORAGE, listOf(StaticIntent("description1", "intentUri1"))))
        val provider = StaticIntentProviderExecutor(staticIntents)

        // Act
        val result = provider.execute(GET_UNCATEGORIZED)

        // Assert
        assertThat(result.states).isEmpty()
        assertThat(result.hintText).isNull()
    }

    @Test
    fun execute_withEmptyIntents_returnsEmptyList() = runTest {
        // Arrange
        val provider = StaticIntentProviderExecutor(emptyList())

        // Act
        val result = provider.execute(GET_UNCATEGORIZED)

        // Assert
        assertThat(result.states).isEmpty()
        assertThat(result.hintText).isNull()
    }
}
