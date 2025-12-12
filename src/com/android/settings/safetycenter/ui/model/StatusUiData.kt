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

import android.safetycenter.SafetyCenterData
import android.safetycenter.SafetyCenterStatus

/**
 * A data class that holds the high-level status information needed by the UI.
 *
 * @property status The underlying [SafetyCenterStatus] from the framework.
 */
data class StatusUiData(val status: SafetyCenterStatus) {
    /**
     * A convenience constructor to create an instance of [StatusUiData] directly from a
     * [SafetyCenterData] object.
     */
    constructor(safetyCenterData: SafetyCenterData) : this(safetyCenterData.status)
}
