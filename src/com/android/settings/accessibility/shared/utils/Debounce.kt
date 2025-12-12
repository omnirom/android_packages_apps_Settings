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

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Debounces a function, ensuring it's only called after a specified delay since the last
 * invocation.
 *
 * @param delayDuration The duration to wait before invoking the original function.
 * @param scope The CoroutineScope in which the debouncing logic will run.
 * @return A new function that incorporates the debouncing behavior.
 */
fun debounce(delayDuration: Duration, scope: CoroutineScope, action: () -> Unit): () -> Unit {
    var debounceJob: Job? = null
    return {
        // Cancel the previous job if it exists
        debounceJob?.cancel()
        // Launch a new coroutine for the debounced action
        debounceJob =
            scope.launch {
                delay(delayDuration) // Wait for the debounce delay
                action()
            }
    }
}
