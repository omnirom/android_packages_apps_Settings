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

import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.AccessibilityShortcutInfo
import android.content.ComponentName
import android.content.Context
import android.os.UserHandle
import android.view.accessibility.AccessibilityManager
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.utils.applications.PackageObservable
import com.android.systemui.utils.coroutines.flow.mapLatestConflated
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking

/** A repository that provides access to accessibility-related settings and information. */
interface AccessibilityRepository {
    /**
     * A flow that emits a list of [AccessibilityShortcutInfo] objects whenever the list of
     * installed accessibility activities changes.
     */
    val accessibilityShortcutInfos: SharedFlow<List<AccessibilityShortcutInfo>>
    /**
     * A flow that emits a list of [AccessibilityServiceInfo] objects whenever the list of installed
     * accessibility services changes.
     */
    val accessibilityServiceInfos: SharedFlow<List<AccessibilityServiceInfo>>

    /**
     * Returns the [AccessibilityShortcutInfo] for the given [ComponentName], or `null` if no such
     * accessibility activity exists.
     */
    fun getAccessibilityShortcutInfo(componentName: ComponentName): AccessibilityShortcutInfo?

    /**
     * Returns a flow that emits the [AccessibilityShortcutInfo] for the given [ComponentName]
     * whenever it changes, or `null` if no such accessibility activity exists. The flow will emit a
     * new value whenever the data changes.
     *
     * See [getAccessibilityServiceInfo]
     */
    fun getAccessibilityShortcutInfoFlow(
        componentName: ComponentName
    ): Flow<AccessibilityShortcutInfo?>

    /**
     * Returns the [AccessibilityServiceInfo] for the given [ComponentName], or `null` if no such
     * accessibility service exists.
     */
    fun getAccessibilityServiceInfo(componentName: ComponentName): AccessibilityServiceInfo?

    /**
     * Returns a flow that emits the [AccessibilityServiceInfo] for the given [ComponentName], or
     * `null` if no such accessibility service exists. The flow will emit a new value whenever the
     * data changes.
     */
    fun getAccessibilityServiceInfoFlow(
        componentName: ComponentName
    ): Flow<AccessibilityServiceInfo?>
}

internal class AccessibilityRepositoryImpl(
    context: Context,
    internal val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob()),
) : AccessibilityRepository {
    private val applicationContext: Context = context.applicationContext
    private val a11yManager: AccessibilityManager =
        applicationContext.getSystemService(AccessibilityManager::class.java)!!

    private val packageChangeFlow: Flow<Unit> = callbackFlow {
        val observer: KeyedObserver<String?> = KeyedObserver { _, _ -> trySend(Unit) }
        PackageObservable.get(context).addObserver(observer, HandlerExecutor.main)
        awaitClose { PackageObservable.get(context).removeObserver(observer) }
    }

    private val accessibilityShortcutInfosInternal: Flow<List<AccessibilityShortcutInfo>> =
        packageChangeFlow
            .mapLatestConflated { getInstalledAccessibilityShortcutList() }
            .onStart { emit(getInstalledAccessibilityShortcutList()) }

    private fun getInstalledAccessibilityShortcutList(): List<AccessibilityShortcutInfo> {
        return a11yManager.getInstalledAccessibilityShortcutListAsUser(
            applicationContext,
            UserHandle.myUserId(),
        )
    }

    private val accessibilityServiceInfosInternal: Flow<List<AccessibilityServiceInfo>> =
        packageChangeFlow
            .mapLatestConflated {
                // Timeout before we update the services if packages are added/removed
                // since the AccessibilityManagerService has to do that processing first
                // to generate the AccessibilityServiceInfo we need for proper
                // presentation.
                delay(FETCH_A11Y_SERVICE_INFO_DELAY)
                getInstalledAccessibilityServiceList()
            }
            .onStart { emit(getInstalledAccessibilityServiceList()) }

    private fun getInstalledAccessibilityServiceList(): List<AccessibilityServiceInfo> {
        return a11yManager.getInstalledAccessibilityServiceList()
    }

    /**
     * This flow is shared and replays the last emitted value to new subscribers. It stops emitting
     * after 5 seconds of no subscribers, and will clear the replay cache.
     */
    override val accessibilityShortcutInfos: SharedFlow<List<AccessibilityShortcutInfo>> =
        accessibilityShortcutInfosInternal.shareIn(
            scope = scope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = SHARING_STOP_TIMEOUT.inWholeMilliseconds,
                    replayExpirationMillis = 0,
                ),
            replay = 1, // Replay the last emitted value to new subscribers
        )
    /**
     * This flow is shared and replays the last emitted value to new subscribers. It stops emitting
     * after 5 seconds of no subscribers, and will clear the replay cache.
     */
    override val accessibilityServiceInfos: SharedFlow<List<AccessibilityServiceInfo>> =
        accessibilityServiceInfosInternal.shareIn(
            scope = scope,
            started =
                SharingStarted.WhileSubscribed(
                    stopTimeoutMillis = SHARING_STOP_TIMEOUT.inWholeMilliseconds,
                    replayExpirationMillis = 0,
                ),
            replay = 1, // Replay the last emitted value to new subscribers
        )

    override fun getAccessibilityShortcutInfo(
        componentName: ComponentName
    ): AccessibilityShortcutInfo? {
        return runBlocking { getAccessibilityShortcutInfoFlow(componentName).first() }
    }

    override fun getAccessibilityShortcutInfoFlow(
        componentName: ComponentName
    ): Flow<AccessibilityShortcutInfo?> {
        return accessibilityShortcutInfos.map { shortcutInfos ->
            shortcutInfos.firstOrNull { it.componentName == componentName }
        }
    }

    override fun getAccessibilityServiceInfo(
        componentName: ComponentName
    ): AccessibilityServiceInfo? {
        return runBlocking { getAccessibilityServiceInfoFlow(componentName).first() }
    }

    override fun getAccessibilityServiceInfoFlow(
        componentName: ComponentName
    ): Flow<AccessibilityServiceInfo?> {
        return accessibilityServiceInfos.map { serviceInfos ->
            serviceInfos.firstOrNull { serviceInfo -> componentName == serviceInfo.componentName }
        }
    }

    companion object {
        // Timeout before we update the services if packages are added/removed
        // since the AccessibilityManagerService has to do that processing first
        // to generate the AccessibilityServiceInfo we need for proper
        // presentation.
        internal val FETCH_A11Y_SERVICE_INFO_DELAY = 1000L.milliseconds
        internal val SHARING_STOP_TIMEOUT = 5000L.milliseconds
    }
}

class AccessibilityRepositoryProvider {
    companion object {
        @Volatile private var instance: AccessibilityRepository? = null

        @JvmStatic
        fun get(context: Context): AccessibilityRepository =
            instance
                ?: synchronized(this) {
                    instance ?: AccessibilityRepositoryImpl(context).also { instance = it }
                }

        @JvmStatic
        fun resetInstanceForTesting() {
            (instance as? AccessibilityRepositoryImpl)?.scope?.cancel()
            instance = null
        }
    }
}
