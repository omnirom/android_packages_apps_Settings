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

package com.android.settings.accessibility.shared.utils

import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowChoreographer
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock

/** Test for [DebounceConfigurationChangeCommitController]. */
@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class DebounceConfigurationChangeCommitControllerTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var controller: DebounceConfigurationChangeCommitController

    // Mock action and its execution tracker
    private var actionExecutionCount = 0
    private var lastExecutedValue: Int? = null
    private val mockCommitAction: (Int) -> Unit = { value ->
        actionExecutionCount++
        lastExecutedValue = value
    }

    @Before
    fun setUp() {
        actionExecutionCount = 0
        lastExecutedValue = null

        // Initialize controller with test dependencies
        // We inject Choreographer.getInstance() which Robolectric will provide a shadow for.
        controller =
            DebounceConfigurationChangeCommitController(
                scope = testScope, // Use the TestScope
                minCommitDelay = Duration.ZERO,
            )
        ShadowSystemClock.reset() // Reset clock before each test
    }

    @After
    fun cleanUp() {
        testScope.cancel()
    }

    @Test
    fun commitDelayed_executesActionAfterSpecifiedDelayAndFrame() =
        testScope.runTest {
            val delay = 100.milliseconds
            var actionExecuted = false

            controller.commitDelayed(delay) { actionExecuted = true }
            assertThat(actionExecuted).isFalse()

            // Advance time before the delay, and assert the action is not executed.
            advanceTimeBy(delay.inWholeMilliseconds - 1)
            runCurrent()
            triggerFrame()
            assertThat(actionExecuted).isFalse()

            // Advance time to satisfy the delay, and assert the action is not executed, until the
            // next frame.
            advanceTimeBy(1)
            runCurrent()
            assertThat(actionExecuted).isFalse()
            triggerFrame()
            assertThat(actionExecuted).isTrue()
        }

    @Test
    fun commitDelayed_debouncesRapidCalls_onlyLastActionExecutes() =
        testScope.runTest {
            val delay = 50.milliseconds
            val firstExecutionValue = 1
            val secondExecutionValue = 2
            val thirdExecutionValue = 3

            // Triggering 3 actions within the delay
            controller.commitDelayed(delay) { mockCommitAction(firstExecutionValue) }
            advanceTimeBy(10)
            runCurrent()
            controller.commitDelayed(delay) { mockCommitAction(secondExecutionValue) }
            advanceTimeBy(10)
            runCurrent()
            controller.commitDelayed(delay) { mockCommitAction(thirdExecutionValue) }

            // Advance time enough for the third (last) action's delay
            advanceTimeBy(delay.inWholeMilliseconds)
            runCurrent()
            triggerFrame()

            assertThat(actionExecutionCount).isEqualTo(1)
            assertThat(lastExecutedValue).isEqualTo(thirdExecutionValue)
        }

    @Test
    fun cancelPendingCommit_preventsActionExecution() =
        testScope.runTest {
            val delay = 100.milliseconds
            var actionExecuted = false
            controller.commitDelayed(delay) { actionExecuted = true }

            // Advance time before delay and cancel the pending commit
            advanceTimeBy(delay.inWholeMilliseconds / 2)
            runCurrent()
            controller.cancelPendingCommit()

            // Advance time to satisfy the delay, and assert the action is not executed
            advanceTimeBy(delay.inWholeMilliseconds)
            runCurrent()
            triggerFrame()

            assertThat(actionExecuted).isFalse()
        }

    @Test
    fun commitDelayed_enforcesMinimumIntervalBetweenCommits() =
        testScope.runTest {
            val minDelay = 200.milliseconds
            val actionDelay = 10.milliseconds // Short delay for each action

            // Re-initialize controller with minCommitDelay
            controller =
                DebounceConfigurationChangeCommitController(
                    scope = testScope,
                    minCommitDelay = minDelay,
                )

            controller.commitDelayed(actionDelay) { mockCommitAction(1) }
            advanceTimeBy(minDelay.inWholeMilliseconds)
            runCurrent()
            triggerFrame()

            assertThat(actionExecutionCount).isEqualTo(1)
            assertThat(lastExecutedValue).isEqualTo(1)

            // Second commit immediately after, should be delayed by minCommitDelay
            controller.commitDelayed(actionDelay) { mockCommitAction(2) }

            // Advance by actionDelay, action 2 should NOT have run yet due to minCommitDelay
            advanceTimeBy(actionDelay.inWholeMilliseconds + 1)
            runCurrent()
            triggerFrame()
            assertThat(actionExecutionCount).isEqualTo(1)

            advanceTimeBy(minDelay)
            runCurrent()
            triggerFrame() // Now action 2 should commit

            assertThat(actionExecutionCount).isEqualTo(2)
            assertThat(lastExecutedValue).isEqualTo(2)
        }

    private fun triggerFrame() {
        ShadowSystemClock.advanceBy(ShadowChoreographer.getFrameDelay())
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
    }
}
