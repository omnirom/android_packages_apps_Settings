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

import android.os.SystemClock
import android.view.Choreographer
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Controller for debouncing committing the configuration change related actions.
 *
 * This class ensures that a commit action is executed only after a specified delay, and that
 * there's a minimum delay between consecutive commits. It uses [Choreographer] to schedule the
 * commit action on the next frame, ensuring smooth UI updates.
 *
 * @param scope The [CoroutineScope] to launch the debouncing coroutine in. Defaults to
 *   [CoroutineScope] with [Dispatchers.Main].
 * @param minCommitDelay The minimum duration that must pass between two consecutive commit actions.
 *   Defaults to [Duration.ZERO], meaning no minimum delay.
 */
class DebounceConfigurationChangeCommitController(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val minCommitDelay: Duration = Duration.ZERO,
) {
    private var debounceJob: Job? = null
    private var lastCommitTimeMillis = Duration.ZERO

    /**
     * Commits the given action after a specified delay. If this function is called again before the
     * delay has passed, the previous action will be cancelled and the new action will be scheduled.
     * This ensures that only the last action is committed after a period of inactivity.
     *
     * The commit action is posted to the [Choreographer] to ensure that it runs on the next frame,
     * preventing potential jank.
     *
     * The actual delay used might be longer than the specified `delay` if `minCommitDelay` is
     * configured. This is to ensure that there is a minimum time interval between consecutive
     * commits, which can be useful for scenarios where frequent updates might be expensive or
     * visually jarring.
     *
     * @param delay The desired delay before the commit action is executed.
     * @param commitAction The action to be executed after the delay.
     */
    fun commitDelayed(delay: Duration, commitAction: () -> Unit) {
        debounceJob?.cancel()
        val elapsedRealtime = SystemClock.elapsedRealtime().toDuration(DurationUnit.MILLISECONDS)
        val timeSinceLastCommitMs = elapsedRealtime - lastCommitTimeMillis
        val timeUntilMinDelaySatisfiedMs = minCommitDelay - timeSinceLastCommitMs
        val adjustedDelayInMs = maxOf(delay, timeUntilMinDelaySatisfiedMs)

        debounceJob =
            scope.launch {
                delay(adjustedDelayInMs)
                Choreographer.getInstance().postFrameCallback {
                    commitAction.invoke()
                    lastCommitTimeMillis =
                        SystemClock.elapsedRealtime().toDuration(DurationUnit.MILLISECONDS)
                }
            }
    }

    /**
     * Cancels any pending commit that was scheduled by [commitDelayed]. If no commit is pending,
     * this function does nothing.
     */
    fun cancelPendingCommit() {
        debounceJob?.cancel()
        debounceJob = null
    }

    companion object {
        /**
         * Minimum delay enforced between consecutive commit actions.
         *
         * This delay helps prevent overly frequent UI updates that could lead to jank or
         * performance issues. It ensures that after one commit action completes, a certain amount
         * of time must pass before the next commit action (triggered by any source) can proceed.
         *
         * **Note:** The default value of 800 milliseconds is an empirical heuristic derived from
         * observations (e.g., approximately double the Settings app's launch time on certain test
         * devices). This value may be subject to future fine-tuning based on performance analysis
         * and user experience feedback. See b/148192402.
         */
        val MIN_COMMIT_DELAY: Duration = 800.toDuration(DurationUnit.MILLISECONDS)

        /**
         * The debounce delay applied when a value is changed via a slider input.
         *
         * When a user interacts with a slider, value changes can be emitted very rapidly. This
         * delay ensures that the commit action (e.g., saving the preference, updating UI) is only
         * triggered after the user has paused their interaction for a short period, preventing
         * excessive updates during continuous sliding.
         *
         * The value is chosen to be responsive yet effective, roughly analogous in purpose to
         * `ViewConfiguration.TAP_TIMEOUT`. See b/148192402.
         */
        val CHANGE_BY_SLIDER_DELAY: Duration = 100.toDuration(DurationUnit.MILLISECONDS)

        /**
         * The debounce delay applied when a change is triggered by a button press.
         *
         * This delay helps ensure smooth UI updates and prevents jarring visual changes if a button
         * is pressed multiple times in quick succession. It provides a slightly longer buffer
         * compared to slider changes because buttons can be clicked repeatedly very quickly. The
         * delay allows any associated animations (e.g., on a seekbar controlled by the button) to
         * be more observable and feel less abrupt.
         *
         * The value is chosen to provide a good user experience, roughly analogous in purpose to
         * `ViewConfiguration.DOUBLE_TAP_TIMEOUT`. See b/148192402.
         */
        val CHANGE_BY_BUTTON_DELAY: Duration = 300.toDuration(DurationUnit.MILLISECONDS)
    }
}
