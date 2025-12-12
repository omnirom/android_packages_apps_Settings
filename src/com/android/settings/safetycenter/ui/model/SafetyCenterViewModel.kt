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

package com.android.settings.safetycenter.ui.model

import android.app.Application
import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterErrorDetails
import android.safetycenter.SafetyCenterIssue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData

/**
 * Defines the abstract contract for a ViewModel that provides data for the Safety Center UI.
 *
 * @param app The application context.
 */
abstract class SafetyCenterViewModel(protected val app: Application) : AndroidViewModel(app) {

    /** Exposes the high-level status of the Safety Center for the UI. */
    abstract val statusUiLiveData: LiveData<StatusUiData>

    /** Exposes the complete, detailed data for rendering the Safety Center UI. */
    abstract val safetyCenterUiLiveData: LiveData<SafetyCenterData>

    /** Exposes any errors reported by the Safety Center framework. */
    abstract val errorLiveData: LiveData<SafetyCenterErrorDetails>

    /** Returns the [SafetyCenterData] currently stored by the Safety Center service. */
    abstract fun getCurrentSafetyCenterDataAsUiData(): SafetyCenterData

    /** Triggers a request to refresh all safety sources. */
    abstract fun rescan()

    /** Clears the current error state. */
    abstract fun clearError()

    /**
     * Requests that a specific [SafetyCenterIssue] be dismissed.
     *
     * @param issue The issue to be dismissed by the framework.
     */
    abstract fun dismissIssue(issue: SafetyCenterIssue)
}
