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

import android.content.Context
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse
import com.android.settings.appfunctions.providers.DeviceStateExecutor
import com.google.android.appfunctions.schema.common.v1.devicestate.PerScreenDeviceStates
import com.google.common.truth.Truth.assertThat
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnit

@RunWith(AndroidJUnit4::class)
class AbstractDeviceStateAppFunctionServiceTest {

    @get:Rule val mockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var mockProvider: DeviceStateExecutor
    @Mock
    private lateinit var mockCallback:
        OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>
    @Captor private lateinit var responseCaptor: ArgumentCaptor<ExecuteAppFunctionResponse>
    @Captor private lateinit var exceptionCaptor: ArgumentCaptor<AppFunctionException>

    private lateinit var service: TestDeviceStateAppFunctionService

    // Test implementation of the abstract class that allows injecting mocks
    private class TestDeviceStateAppFunctionService(
        override val deviceStateProviderExecutors: List<DeviceStateExecutor>
    ) : AbstractDeviceStateAppFunctionService() {
        init {
            // Attach a base context to allow applicationContext to be used
            attachBaseContext(ApplicationProvider.getApplicationContext<Context>())
        }

        // Helper to access protected property in tests
        fun getEnglishContextForTest(): Context = englishContext
    }

    @Before
    fun setUp() {
        service = TestDeviceStateAppFunctionService(listOf(mockProvider))
        service.onCreate()
    }

    @Test
    fun onCreate_createsEnglishContext() {
        val context = service.getEnglishContextForTest()
        assertThat(context.resources.configuration.locales[0]).isEqualTo(Locale.US)
    }

    @Test
    fun onExecuteFunction_invalidFunctionId_callsOnError() = runTest {
        val request = ExecuteAppFunctionRequest.Builder("test.package", "invalidFunction").build()

        service.onExecuteFunction(request, "test.package", CancellationSignal(), mockCallback)

        verify(mockCallback).onError(exceptionCaptor.capture())
        val exception = exceptionCaptor.value
        assertThat(exception.errorCode).isEqualTo(AppFunctionException.ERROR_FUNCTION_NOT_FOUND)
        assertThat(exception.message).contains("invalidFunction not supported")
    }

    @Test
    fun onExecuteFunction_validFunctionId_callsOnResult() = runTest {
        // Arrange
        val request =
            ExecuteAppFunctionRequest.Builder(
                    "test.package",
                    DeviceStateAppFunctionType.GET_STORAGE.functionId,
                )
                .build()
        val providerResult =
            DeviceStateProviderExecutorResult(
                states = listOf(PerScreenDeviceStates(description = "Storage State")),
                hintText = "Storage Hint",
            )
        `when`(mockProvider.execute(DeviceStateAppFunctionType.GET_STORAGE))
            .thenReturn(providerResult)

        // Act
        service.onExecuteFunction(request, "test.package", CancellationSignal(), mockCallback)

        // Assert
        verify(mockCallback).onResult(responseCaptor.capture())
        val response = responseCaptor.value
        assertThat(response).isNotNull()

        // Verify the returned document has the expected schema type.
        val returnedDoc =
            response
                .getResultDocument()
                .getPropertyDocument(ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE)
        assertThat(returnedDoc).isNotNull()
        assertThat(returnedDoc!!.schemaType)
            .isEqualTo(
                "com.google.android.appfunctions.schema.common.v1.devicestate.DeviceStateResponse"
            )
    }
}
