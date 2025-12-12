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
import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.view.accessibility.AccessibilityManager
import androidx.test.core.app.ApplicationProvider
import com.android.settings.accessibility.data.AccessibilityRepositoryImpl.Companion.FETCH_A11Y_SERVICE_INFO_DELAY
import com.android.settings.accessibility.data.AccessibilityRepositoryImpl.Companion.SHARING_STOP_TIMEOUT
import com.android.settings.testutils.AccessibilityTestUtils
import com.android.settings.testutils.shadow.ShadowAccessibilityManager
import com.android.settingslib.utils.applications.PackageChangeReason
import com.android.settingslib.utils.applications.PackageObservable
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowApplication

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AccessibilityRepositoryImplTest {
    private val testScope = TestScope(UnconfinedTestDispatcher())
    private lateinit var context: Application // Initialize in setUp for clarity
    private lateinit var a11yManager: ShadowAccessibilityManager
    private lateinit var repository: AccessibilityRepositoryImpl
    private lateinit var shadowApplication: ShadowApplication

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shadowApplication = shadowOf(context) // Cast to Application is implicit
        a11yManager = Shadow.extract(context.getSystemService(AccessibilityManager::class.java))
        shadowApplication.assertNoBroadcastListenersOfActionRegistered(
            context,
            Intent.ACTION_PACKAGE_CHANGED,
        )
        repository = AccessibilityRepositoryImpl(context, testScope.backgroundScope)

        // Initial setup for default data
        val initialServiceInfo = createTestServiceInfo(A11Y_SERVICE_COMPONENT, isTool = true)
        val initialShortcutInfo = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT)

        a11yManager.setInstalledAccessibilityServiceList(listOf(initialServiceInfo))
        a11yManager.setInstalledAccessibilityShortcutListAsUser(listOf(initialShortcutInfo))
    }

    @After
    fun cleanUp() {
        AccessibilityRepositoryProvider.resetInstanceForTesting()
        testScope.cancel() // Ensures all coroutines launched in this scope are cancelled
    }

    @Test
    fun accessibilityShortcutInfos_initialEmission_emitsCorrectList() =
        testScope.runTest {
            val firstEmission = repository.accessibilityShortcutInfos.first()

            assertThat(firstEmission).hasSize(1)
            assertThat(firstEmission.first().componentName).isEqualTo(A11Y_ACTIVITY_COMPONENT)
        }

    @Test
    fun accessibilityShortcutInfos_packageChange_emitsUpdatedList() =
        testScope.runTest {
            val collectedItems = mutableListOf<List<AccessibilityShortcutInfo>>()
            // Collect in a background job that can be cancelled if needed,
            // though `runTest` often handles this for simple collects.
            val collectionJob =
                backgroundScope.launch {
                    repository.accessibilityShortcutInfos.collect { collectedItems.add(it) }
                }
            runCurrent()

            assertThat(collectedItems).hasSize(1) // Verify initial emission
            assertThat(collectedItems[0].first().componentName).isEqualTo(A11Y_ACTIVITY_COMPONENT)

            // Update installed accessibility shortcut lists
            val shortcutInfo1 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT) // Existing
            val shortcutInfo2 = createMockShortcutInfo(A11Y_ACTIVITY_COMPONENT2) // New
            a11yManager.setInstalledAccessibilityShortcutListAsUser(
                listOf(shortcutInfo1, shortcutInfo2)
            )

            // Notify package changed
            val packageObservable = PackageObservable.get(context)
            packageObservable.notifyChange(PACKAGE_NAME, PackageChangeReason.CHANGED)
            runCurrent()

            assertThat(collectedItems).hasSize(2) // Verify second emission
            assertThat(collectedItems[1].map { it.componentName })
                .containsExactly(A11Y_ACTIVITY_COMPONENT, A11Y_ACTIVITY_COMPONENT2)

            collectionJob.cancel() // Clean up the collector
        }

    @Test
    fun accessibilityServiceInfos_initialEmission_emitsCorrectList() =
        testScope.runTest {
            val firstEmission = repository.accessibilityServiceInfos.first()

            assertThat(firstEmission).hasSize(1)
            assertThat(firstEmission.first().componentName).isEqualTo(A11Y_SERVICE_COMPONENT)
        }

    @Test
    fun accessibilityServiceInfos_packageChange_emitsUpdatedList() =
        testScope.runTest {
            val collectedItems = mutableListOf<List<AccessibilityServiceInfo>>()
            val collectionJob =
                backgroundScope.launch {
                    repository.accessibilityServiceInfos.collect { collectedItems.add(it) }
                }
            runCurrent()

            assertThat(collectedItems).hasSize(1)
            assertThat(collectedItems[0].first().componentName).isEqualTo(A11Y_SERVICE_COMPONENT)

            // Update installed accessibility service lists
            val serviceInfo1 = createTestServiceInfo(A11Y_SERVICE_COMPONENT) // Existing
            val serviceInfo2 = createTestServiceInfo(A11Y_SERVICE_COMPONENT2) // New
            a11yManager.setInstalledAccessibilityServiceList(listOf(serviceInfo1, serviceInfo2))

            // Notify package changed
            val packageObservable = PackageObservable.get(context)
            packageObservable.notifyChange(PACKAGE_NAME, PackageChangeReason.CHANGED)
            advanceTimeBy(FETCH_A11Y_SERVICE_INFO_DELAY)
            runCurrent()

            assertThat(collectedItems).hasSize(2)
            assertThat(collectedItems[1].map { it.componentName })
                .containsExactly(A11Y_SERVICE_COMPONENT, A11Y_SERVICE_COMPONENT2)

            collectionJob.cancel() // Clean up the collector
        }

    @Test
    fun getAccessibilityShortcutInfo_exists_returnsInfo() =
        testScope.runTest {
            val shortcutInfo = repository.getAccessibilityShortcutInfo(A11Y_ACTIVITY_COMPONENT)

            assertThat(shortcutInfo).isNotNull()
            assertThat(shortcutInfo!!.componentName).isEqualTo(A11Y_ACTIVITY_COMPONENT)
        }

    @Test
    fun getAccessibilityShortcutInfo_notFound_returnsNull() =
        testScope.runTest {
            val nonExistentComponent = ComponentName(PACKAGE_NAME, "NonExistentActivity")
            val shortcutInfo = repository.getAccessibilityShortcutInfo(nonExistentComponent)

            assertThat(shortcutInfo).isNull()
        }

    @Test
    fun getAccessibilityShortcutInfoFlow_exists_returnsInfo() =
        testScope.runTest {
            val shortcutInfo =
                repository.getAccessibilityShortcutInfoFlow(A11Y_ACTIVITY_COMPONENT).first()

            assertThat(shortcutInfo).isNotNull()
            assertThat(shortcutInfo!!.componentName).isEqualTo(A11Y_ACTIVITY_COMPONENT)
        }

    @Test
    fun getAccessibilityShortcutInfoFlow_notFound_returnsNull() =
        testScope.runTest {
            val nonExistentComponent = ComponentName(PACKAGE_NAME, "NonExistentActivity")
            val shortcutInfo =
                repository.getAccessibilityShortcutInfoFlow(nonExistentComponent).first()

            assertThat(shortcutInfo).isNull()
        }

    @Test
    fun getAccessibilityShortcutInfoFlow_packageRemoved_returnsNull() =
        testScope.runTest {
            val collectedItems = mutableListOf<AccessibilityShortcutInfo?>()
            val collectionJob =
                backgroundScope.launch {
                    repository.getAccessibilityShortcutInfoFlow(A11Y_ACTIVITY_COMPONENT).collect {
                        collectedItems.add(it)
                    }
                }
            runCurrent()

            // Initial emission should be the existing info
            assertThat(collectedItems).hasSize(1)
            assertThat(collectedItems[0]).isNotNull()
            assertThat(collectedItems[0]!!.componentName).isEqualTo(A11Y_ACTIVITY_COMPONENT)

            // Simulate package removal by updating the list to empty
            a11yManager.setInstalledAccessibilityShortcutListAsUser(emptyList())

            // Notify package changed
            val packageObservable = PackageObservable.get(context)
            packageObservable.notifyChange(PACKAGE_NAME, PackageChangeReason.REMOVED)
            runCurrent()

            assertThat(collectedItems).hasSize(2)
            assertThat(collectedItems[1]).isNull() // Second emission should be null
            collectionJob.cancel()
        }

    @Test
    fun getAccessibilityServiceInfo_exists_returnsInfo() =
        testScope.runTest {
            val serviceInfo = repository.getAccessibilityServiceInfo(A11Y_SERVICE_COMPONENT)

            assertThat(serviceInfo).isNotNull()
            assertThat(serviceInfo!!.componentName).isEqualTo(A11Y_SERVICE_COMPONENT)
        }

    @Test
    fun getAccessibilityServiceInfo_notFound_returnsNull() =
        testScope.runTest {
            val nonExistentComponent = ComponentName(PACKAGE_NAME, "NonExistentService")
            val serviceInfo = repository.getAccessibilityServiceInfo(nonExistentComponent)

            assertThat(serviceInfo).isNull()
        }

    @Test
    fun getAccessibilityServiceInfoFlow_exists_returnsInfo() =
        testScope.runTest {
            val serviceInfo =
                repository.getAccessibilityServiceInfoFlow(A11Y_SERVICE_COMPONENT).first()

            assertThat(serviceInfo).isNotNull()
            assertThat(serviceInfo!!.componentName).isEqualTo(A11Y_SERVICE_COMPONENT)
        }

    @Test
    fun getAccessibilityServiceInfoFlow_notFound_returnsNull() =
        testScope.runTest {
            val nonExistentComponent = ComponentName(PACKAGE_NAME, "NonExistentService")
            val serviceInfo =
                repository.getAccessibilityServiceInfoFlow(nonExistentComponent).first()

            assertThat(serviceInfo).isNull()
        }

    @Test
    fun getAccessibilityServiceInfoFlow_packageRemoved_returnsNull() =
        testScope.runTest {
            val collectedItems = mutableListOf<AccessibilityServiceInfo?>()
            val collectionJob =
                backgroundScope.launch {
                    repository.getAccessibilityServiceInfoFlow(A11Y_SERVICE_COMPONENT).collect {
                        collectedItems.add(it)
                    }
                }
            runCurrent()

            // Initial emission should be the existing info
            assertThat(collectedItems).hasSize(1)
            assertThat(collectedItems[0]).isNotNull()
            assertThat(collectedItems[0]!!.componentName).isEqualTo(A11Y_SERVICE_COMPONENT)

            // Simulate package removal by updating the list to empty
            a11yManager.setInstalledAccessibilityServiceList(emptyList())

            // Notify package changed
            val packageObservable = PackageObservable.get(context)
            packageObservable.notifyChange(PACKAGE_NAME, PackageChangeReason.REMOVED)
            advanceTimeBy(FETCH_A11Y_SERVICE_INFO_DELAY)
            runCurrent()

            assertThat(collectedItems).hasSize(2)
            assertThat(collectedItems[1]).isNull() // Second emission should be null
            collectionJob.cancel()
        }

    @Test
    fun accessibilityShortcutInfos_flowStopsAndRestarts() =
        testScope.runTest {
            var collectedCount = 0
            val collectionJob =
                backgroundScope.launch {
                    repository.accessibilityShortcutInfos.collect { collectedCount++ }
                }
            runCurrent()
            assertThat(collectedCount).isEqualTo(1)
            collectionJob.cancel() // No more subscriber

            advanceTimeBy(SHARING_STOP_TIMEOUT.inWholeMilliseconds)
            runCurrent()
            assertThat(PackageObservable.get(context).hasAnyObserver()).isFalse()

            var collectedCount2 = 0
            val collectionJob2 =
                backgroundScope.launch {
                    repository.accessibilityShortcutInfos.collect { collectedCount2++ }
                }
            runCurrent()
            assertThat(collectedCount2).isEqualTo(1)
            assertThat(PackageObservable.get(context).hasAnyObserver()).isTrue()

            PackageObservable.get(context).notifyChange(PACKAGE_NAME, PackageChangeReason.CHANGED)
            runCurrent()
            assertThat(collectedCount2).isEqualTo(2)

            collectionJob2.cancel() // No more subscriber
        }

    @Test
    fun accessibilityServiceInfos_flowStopsAndRestarts() =
        testScope.runTest {
            var collectedCount = 0
            val collectionJob =
                backgroundScope.launch {
                    repository.accessibilityServiceInfos.collect { collectedCount++ }
                }
            runCurrent()
            assertThat(collectedCount).isEqualTo(1)
            collectionJob.cancel() // No more subscriber

            advanceTimeBy(SHARING_STOP_TIMEOUT.inWholeMilliseconds)
            runCurrent()
            assertThat(PackageObservable.get(context).hasAnyObserver()).isFalse()

            var collectedCount2 = 0
            val collectionJob2 =
                backgroundScope.launch {
                    repository.accessibilityServiceInfos.collect { collectedCount2++ }
                }
            runCurrent()
            assertThat(collectedCount2).isEqualTo(1)
            assertThat(PackageObservable.get(context).hasAnyObserver()).isTrue()

            PackageObservable.get(context).notifyChange(PACKAGE_NAME, PackageChangeReason.CHANGED)
            advanceTimeBy(FETCH_A11Y_SERVICE_INFO_DELAY)
            runCurrent()
            assertThat(collectedCount2).isEqualTo(2)

            collectionJob2.cancel() // No more subscriber
        }

    // Helper to create mock AccessibilityShortcutInfo
    private fun createMockShortcutInfo(componentName: ComponentName): AccessibilityShortcutInfo {
        val mockInfo: AccessibilityShortcutInfo = mock()
        whenever(mockInfo.componentName).thenReturn(componentName)
        return mockInfo
    }

    // Helper to create AccessibilityServiceInfo (using existing TestUtils)
    private fun createTestServiceInfo(
        componentName: ComponentName,
        isTool: Boolean = false,
    ): AccessibilityServiceInfo {
        return AccessibilityTestUtils.createAccessibilityServiceInfo(
                context,
                componentName,
                false, // Or make this a parameter if it varies
            )
            .apply { isAccessibilityTool = isTool }
    }

    companion object {
        private const val PACKAGE_NAME = "com.foo.bar" // Used for notifyChange
        private val A11Y_SERVICE_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yService")
        private val A11Y_SERVICE_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yService2")
        private val A11Y_ACTIVITY_COMPONENT = ComponentName(PACKAGE_NAME, "FakeA11yActivity")
        private val A11Y_ACTIVITY_COMPONENT2 = ComponentName(PACKAGE_NAME, "FakeA11yActivity2")
    }
}
