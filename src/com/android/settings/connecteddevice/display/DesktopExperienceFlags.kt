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
package com.android.settings.connecteddevice.display

import android.window.DesktopExperienceFlags.DesktopExperienceFlag
import com.android.settings.flags.FeatureFlags

/** Class handling Settings flags, but using the Desktop Experience developer option overrides. */
class DesktopExperienceFlags(private val featureFlagsImpl: FeatureFlags) : FeatureFlags by featureFlagsImpl {

    private val displayTopologyPaneInDisplayListFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::displayTopologyPaneInDisplayList,
            /* shouldOverrideByDevOption= */ true,
        )

    override fun displayTopologyPaneInDisplayList(): Boolean =
        displayTopologyPaneInDisplayListFlag.isTrue

    private val displaySizeConnectedDisplaySettingFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::displaySizeConnectedDisplaySetting,
            /* shouldOverrideByDevOption= */ true,
        )

    override fun displaySizeConnectedDisplaySetting(): Boolean =
        displaySizeConnectedDisplaySettingFlag.isTrue
}