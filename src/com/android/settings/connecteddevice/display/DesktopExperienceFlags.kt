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
import com.android.settings.flags.Flags

/** Class handling Settings flags, but using the Desktop Experience developer option overrides. */
class DesktopExperienceFlags(private val featureFlagsImpl: FeatureFlags) :
    FeatureFlags by featureFlagsImpl {

    private val displayTopologyPaneInDisplayListFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::displayTopologyPaneInDisplayList,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST,
        )

    override fun displayTopologyPaneInDisplayList(): Boolean =
        displayTopologyPaneInDisplayListFlag.isTrue

    private val displaySizeConnectedDisplaySettingFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::displaySizeConnectedDisplaySetting,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING,
        )

    override fun displaySizeConnectedDisplaySetting(): Boolean =
        displaySizeConnectedDisplaySettingFlag.isTrue

    private val resolutionAndEnableConnectedDisplaySettingFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::resolutionAndEnableConnectedDisplaySetting,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING,
        )

    private val resolutionAndEnableConnectedDisplaySettingBugfixFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::resolutionAndEnableConnectedDisplaySettingBugfix,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_RESOLUTION_AND_ENABLE_CONNECTED_DISPLAY_SETTING_BUGFIX,
        )

    override fun resolutionAndEnableConnectedDisplaySetting(): Boolean =
        resolutionAndEnableConnectedDisplaySettingFlag.isTrue or
            resolutionAndEnableConnectedDisplaySettingBugfixFlag.isTrue

    private val showStackedMirroringDisplayConnectedDisplaySettingFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::showStackedMirroringDisplayConnectedDisplaySetting,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_SHOW_STACKED_MIRRORING_DISPLAY_CONNECTED_DISPLAY_SETTING,
        )

    override fun showStackedMirroringDisplayConnectedDisplaySetting(): Boolean =
        showStackedMirroringDisplayConnectedDisplaySettingFlag.isTrue

    private val showTabbedConnectedDisplaySettingFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::showTabbedConnectedDisplaySetting,
            /* shouldOverrideByDevOption= */ false,
            Flags.FLAG_SHOW_TABBED_CONNECTED_DISPLAY_SETTING,
        )

    override fun showTabbedConnectedDisplaySetting(): Boolean =
        showTabbedConnectedDisplaySettingFlag.isTrue

    private val enableDefaultDisplayInTopologySwitchBugfixFlag =
        DesktopExperienceFlag(
            featureFlagsImpl::enableDefaultDisplayInTopologySwitchBugfix,
            /* shouldOverrideByDevOption= */ true,
            Flags.FLAG_ENABLE_DEFAULT_DISPLAY_IN_TOPOLOGY_SWITCH_BUGFIX,
        )

    override fun enableDefaultDisplayInTopologySwitchBugfix(): Boolean =
        enableDefaultDisplayInTopologySwitchBugfixFlag.isTrue

    private val enableResolutionApplyConfirmationBugfix =
        DesktopExperienceFlag(
            featureFlagsImpl::enableResolutionApplyConfirmationBugfix,
            /* shouldOverrideByDevOption= */ false,
            Flags.FLAG_ENABLE_RESOLUTION_APPLY_CONFIRMATION_BUGFIX,
        )

    override fun enableResolutionApplyConfirmationBugfix(): Boolean =
        enableResolutionApplyConfirmationBugfix.isTrue
}
