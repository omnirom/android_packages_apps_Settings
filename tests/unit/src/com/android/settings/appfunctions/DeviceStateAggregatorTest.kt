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

package com.android.settings.appfunctions

import android.app.appsearch.GenericDocument
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.appfunctions.providers.DeviceStateExecutor
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit

@RunWith(AndroidJUnit4::class)
class DeviceStateAggregatorTest {

    @get:Rule val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var provider1: DeviceStateExecutor

    @Mock private lateinit var provider2: DeviceStateExecutor

    @Test
    fun aggregate_combinesResultsFromMultipleProviders() = runTest {
        // Arrange
        val states1 = listOf(PerScreenDeviceStates(description = "State from Provider 1"))
        val result1 = DeviceStateProviderExecutorResult(states = states1, hintText = "Hint 1")
        `when`(provider1.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)).thenReturn(result1)

        val states2 = listOf(PerScreenDeviceStates(description = "State from Provider 2"))
        val result2 = DeviceStateProviderExecutorResult(states = states2, hintText = "Hint 2")
        `when`(provider2.execute(DeviceStateAppFunctionType.GET_UNCATEGORIZED)).thenReturn(result2)

        val aggregator = DeviceStateProviderAggregator(executors = listOf(provider1, provider2))

        // Act
        val response =
            aggregator.aggregate(
                DeviceStateAppFunctionType.GET_UNCATEGORIZED,
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build(),
                "en-US",
            )

        // Assert
        assertThat(response.perScreenDeviceStates).containsExactlyElementsIn(states1 + states2)
        assertThat(response.deviceLocale).isEqualTo("en-US")
    }

    @Test
    fun aggregate_withNoProviders_returnsEmptyResponse() = runTest {
        // Arrange
        val aggregator = DeviceStateProviderAggregator(executors = emptyList())

        // Act
        val response =
            aggregator.aggregate(
                DeviceStateAppFunctionType.GET_UNCATEGORIZED,
                GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "").build(),
                "en-US",
            )

        // Assert
        assertThat(response.perScreenDeviceStates).isEmpty()
        assertThat(response.deviceLocale).isEqualTo("en-US")
    }
}
