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
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DebounceTest {

    @Test
    fun actionShouldExecuteAfterDelayWhenCalledOnce() = runTest {
        var actionExecutedCount = 0
        val delayDuration = 100.milliseconds

        val debouncedAction = debounce(delayDuration, this) { actionExecutedCount++ }

        // Action shouldn't execute immediately
        debouncedAction()
        assertThat(actionExecutedCount).isEqualTo(0)

        advanceTimeBy(delayDuration.inWholeMilliseconds / 2)
        runCurrent()
        // Action shouldn't execute yet
        assertThat(actionExecutedCount).isEqualTo(0)

        advanceTimeBy(delayDuration.inWholeMilliseconds / 2 + 1)
        runCurrent()
        // Action should now execute
        assertThat(actionExecutedCount).isEqualTo(1)
    }

    @Test
    fun actionShouldOnlyExecuteOnceAfterMultipleRapidCalls() = runTest {
        var actionExecutedCount = 0
        val delayDuration = 100.milliseconds
        val callInterval = 20.milliseconds // Calls are faster than the debounce delay

        val debouncedAction = debounce(delayDuration, this) { actionExecutedCount++ }

        repeat(6) {
            // Call the action shouldn't execute immediately
            debouncedAction()
            advanceTimeBy(callInterval.inWholeMilliseconds)
            runCurrent()
            assertThat(actionExecutedCount).isEqualTo(0)
        }

        // Advance time enough for the last call's debounce to complete
        advanceTimeBy(delayDuration.inWholeMilliseconds + 1)
        runCurrent()
        assertThat(actionExecutedCount).isEqualTo(1)

        // Advance more time to ensure no other executions happen
        advanceTimeBy(delayDuration.inWholeMilliseconds * 2)
        runCurrent()
        assertThat(actionExecutedCount).isEqualTo(1)
    }

    @Test
    fun actionShouldExecuteIfCallsAreSpacedLongerThanDelay() = runTest {
        var actionExecutedCount = 0
        val delayDuration = 100.milliseconds

        val debouncedAction = debounce(delayDuration, this) { actionExecutedCount++ }

        // First call
        debouncedAction()
        advanceTimeBy(delayDuration.inWholeMilliseconds + 1)
        runCurrent()
        assertThat(actionExecutedCount).isEqualTo(1)

        // Second call
        debouncedAction()
        advanceTimeBy(delayDuration.inWholeMilliseconds / 2) // Not enough time yet
        runCurrent()
        assertThat(actionExecutedCount).isEqualTo(1) // Still 1 from previous execution

        // Enough time for the second call
        advanceTimeBy(delayDuration.inWholeMilliseconds / 2 + 1)
        runCurrent()
        assertThat(actionExecutedCount).isEqualTo(2)
    }

    @Test
    fun cancellingTheScopeShouldPreventActionExecution() = runTest {
        var actionExecutedCount = 0
        val delayDuration = 100.milliseconds

        // Create a child scope so we can cancel it independently
        val childScope = CoroutineScope(coroutineContext + Job())

        try {

            val debouncedAction = debounce(delayDuration, childScope) { actionExecutedCount++ }

            debouncedAction()
            assertThat(actionExecutedCount).isEqualTo(0)

            // Cancel the scope where the debounce job is running and advance time well past the
            // delay
            childScope.cancel()
            advanceTimeBy(delayDuration.inWholeMilliseconds * 2)
            runCurrent()

            assertThat(actionExecutedCount).isEqualTo(0)
        } finally {
            childScope.cancel()
        }
    }

    @Test
    fun multipleDebouncedFunctionsWithDifferentScopesShouldWorkIndependently() = runTest {
        var action1ExecutedCount = 0
        var action2ExecutedCount = 0
        val delay = 100.milliseconds

        val coroutineScope1 = CoroutineScope(coroutineContext + Job())
        val coroutineScope2 = CoroutineScope(coroutineContext + Job())

        try {

            val debouncedAction1 = debounce(delay, coroutineScope1) { action1ExecutedCount++ }
            val debouncedAction2 = debounce(delay, coroutineScope2) { action2ExecutedCount++ }

            debouncedAction1()
            debouncedAction2()

            advanceTimeBy(delay.inWholeMilliseconds / 2)
            runCurrent()
            assertThat(action1ExecutedCount).isEqualTo(0)
            assertThat(action2ExecutedCount).isEqualTo(0)

            debouncedAction1() // Re-trigger action 1

            advanceTimeBy(delay.inWholeMilliseconds / 2 + 1)
            runCurrent()

            // Only action2 should have fired by now, as action1 was re-triggered
            assertThat(action1ExecutedCount).isEqualTo(0)
            assertThat(action2ExecutedCount).isEqualTo(1)

            advanceTimeBy(delay.inWholeMilliseconds)
            runCurrent()
            // Now action1 should also have fired
            assertThat(action1ExecutedCount).isEqualTo(1)
            assertThat(action2ExecutedCount).isEqualTo(1)
        } finally {
            coroutineScope1.cancel()
            coroutineScope2.cancel()
        }
    }
}
