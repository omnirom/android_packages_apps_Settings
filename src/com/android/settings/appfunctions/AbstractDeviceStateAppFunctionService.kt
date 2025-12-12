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

import android.app.KeyguardManager
import android.app.appfunctions.AppFunctionException.ERROR_DENIED
import android.app.appsearch.GenericDocument
import android.content.Context
import android.content.res.Configuration
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import android.os.Trace
import android.util.Log
import androidx.annotation.Keep
import com.android.extensions.appfunctions.AppFunctionException
import com.android.extensions.appfunctions.AppFunctionException.ERROR_FUNCTION_NOT_FOUND
import com.android.extensions.appfunctions.AppFunctionService
import com.android.extensions.appfunctions.ExecuteAppFunctionRequest
import com.android.extensions.appfunctions.ExecuteAppFunctionResponse
import com.android.settings.appfunctions.providers.AndroidApiStateProviderExecutor
import com.android.settings.appfunctions.providers.AndroidApiStateSetterExecutor
import com.android.settings.appfunctions.providers.CatalystStateMetadataProviderExecutor
import com.android.settings.appfunctions.providers.CatalystStateProviderExecutor
import com.android.settings.appfunctions.providers.CatalystStateSetterExecutor
import com.android.settings.appfunctions.providers.DeviceStateExecutor
import com.android.settings.utils.getLocale
import java.util.Locale
import kotlinx.coroutines.runBlocking

/**
 * An abstract [AppFunctionService] that provides device state information.
 *
 * Subclasses must implement [executors] to define the data sources and transformations for device
 * state.
 */
@Keep
abstract class AbstractDeviceStateAppFunctionService : AppFunctionService() {
    protected lateinit var englishContext: Context
        private set

    open val deviceStateProviderExecutors: List<DeviceStateExecutor> by lazy {
        listOf(
            CatalystStateProviderExecutor(
                getSettingsCatalystConfig(),
                applicationContext,
                englishContext,
            ),
            AndroidApiStateProviderExecutor(applicationContext),
        )
    }
    val deviceStateProviderAggregator by lazy {
        DeviceStateProviderAggregator(deviceStateProviderExecutors)
    }

    open val deviceStateMetadataProviderExecutors: List<DeviceStateExecutor> by lazy {
        listOf(
            CatalystStateMetadataProviderExecutor(
                getSettingsCatalystConfig(),
                applicationContext,
                englishContext,
            )
        )
    }
    val deviceStateMetadataProviderAggregator by lazy {
        DeviceStateMetadataProviderAggregator(deviceStateMetadataProviderExecutors)
    }

    open val deviceStateSetterExecutors: List<DeviceStateExecutor> by lazy {
        listOf(CatalystStateSetterExecutor(), AndroidApiStateSetterExecutor(applicationContext))
    }
    val deviceStateSetterAggregator by lazy {
        DeviceStateSetterAggregator(deviceStateSetterExecutors)
    }

    open val aggregators by lazy {
        mapOf(
            DeviceStateAppFunctionType.GET_UNCATEGORIZED to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_STORAGE to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_BATTERY to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_MOBILE_DATA to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_NOTIFICATIONS to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_APPS to deviceStateProviderAggregator,
            DeviceStateAppFunctionType.GET_METADATA to deviceStateMetadataProviderAggregator,
            DeviceStateAppFunctionType.SET_DEVICE_STATE to deviceStateSetterAggregator,
            DeviceStateAppFunctionType.ADJUST_DEVICE_STATE_BY_PERCENTAGE to
                deviceStateSetterAggregator,
            DeviceStateAppFunctionType.OFFSET_DEVICE_STATE_BY_VALUE to deviceStateSetterAggregator,
            DeviceStateAppFunctionType.TOGGLE_DEVICE_STATE to deviceStateSetterAggregator,
        )
    }

    override fun onCreate() {
        super.onCreate()
        englishContext = createEnglishContext()
    }

    final override fun onExecuteFunction(
        request: ExecuteAppFunctionRequest,
        callingPackage: String,
        cancellationSignal: CancellationSignal,
        callback: OutcomeReceiver<ExecuteAppFunctionResponse, AppFunctionException>,
    ) {
        val appFunctionType = DeviceStateAppFunctionType.fromId(request.functionIdentifier)
        if (appFunctionType == null) {
            callback.onError(
                AppFunctionException(
                    ERROR_FUNCTION_NOT_FOUND,
                    "${request.functionIdentifier} not supported.",
                )
            )
            return
        }

        if (
            shouldCheckForDeviceLock(request.parameters, appFunctionType) &&
                applicationContext.getSystemService(KeyguardManager::class.java).isDeviceLocked
        ) {
            callback.onError(
                AppFunctionException(
                    ERROR_DENIED,
                    "Attempting to execute a device state app function while " +
                        "the device is locked.",
                )
            )
        }

        runBlocking {
            Trace.beginSection("DeviceStateAppFunction ${request.functionIdentifier}")
            Log.d(TAG, "device state app function ${request.functionIdentifier} called.")
            if (!aggregators.containsKey(appFunctionType)) {
                callback.onError(
                    AppFunctionException(
                        ERROR_FUNCTION_NOT_FOUND,
                        "${request.functionIdentifier} not supported.",
                    )
                )
            }
            val responseData =
                aggregators[appFunctionType]!!.aggregate(
                    appFunctionType,
                    request.parameters,
                    applicationContext.getLocale().toString(),
                )
            val response = buildResponse(responseData)
            callback.onResult(response)
            Log.d(TAG, "app function ${request.functionIdentifier} fulfilled.")
            Trace.endSection()
        }
    }

    private fun buildResponse(responseData: Any): ExecuteAppFunctionResponse {
        val jetpackDocument = androidx.appsearch.app.GenericDocument.fromDocumentClass(responseData)
        val platformDocument =
            GenericDocumentToPlatformConverter.toPlatformGenericDocument(jetpackDocument)
        val result =
            GenericDocument.Builder<GenericDocument.Builder<*>>("", "", "")
                .setPropertyDocument(
                    ExecuteAppFunctionResponse.PROPERTY_RETURN_VALUE,
                    platformDocument,
                )
                .build()
        return ExecuteAppFunctionResponse(result)
    }

    private fun createEnglishContext(): Context {
        val configuration = Configuration(applicationContext.resources.configuration)
        configuration.setLocale(Locale.US)
        return applicationContext.createConfigurationContext(configuration)
    }

    private fun shouldCheckForDeviceLock(
        params: GenericDocument,
        appFunctionType: DeviceStateAppFunctionType,
    ): Boolean {
        return params
            .getPropertyDocument(appFunctionType.functionId + "Params")
            ?.getPropertyBoolean("requestInitiatedWhileUnlocked") != true
    }

    companion object {
        private const val TAG = "AbstractDeviceStateAppFunctionService"
    }
}
