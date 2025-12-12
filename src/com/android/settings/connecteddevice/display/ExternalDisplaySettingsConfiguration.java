/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.android.settings.connecteddevice.display;

import android.hardware.display.DisplayManager;

import androidx.annotation.NonNull;

import com.android.settings.R;
import com.android.settings.flags.FeatureFlags;

public class ExternalDisplaySettingsConfiguration {
    static final String VIRTUAL_DISPLAY_PACKAGE_NAME_SYSTEM_PROPERTY =
            "persist.demo.userrotation.package_name";
    static final String DISPLAY_ID_ARG = "display_id";
    static final int EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE = R.string.external_display_not_found;
    static final int EXTERNAL_DISPLAY_HELP_URL = R.string.help_url_external_display;

    public abstract static class DisplayListener implements DisplayManager.DisplayListener {
        @Override
        public void onDisplayAdded(int displayId) {
            update(displayId);
        }

        @Override
        public void onDisplayRemoved(int displayId) {
            update(displayId);
        }

        @Override
        public void onDisplayChanged(int displayId) {
            update(displayId);
        }

        @Override
        public void onDisplayConnected(int displayId) {
            update(displayId);
        }

        @Override
        public void onDisplayDisconnected(int displayId) {
            update(displayId);
        }

        /**
         * Called from other listener methods to trigger update of the settings page.
         */
        public abstract void update(int displayId);
    }

    /**
     * @return whether the settings page is enabled or not.
     */
    public static boolean isExternalDisplaySettingsPageEnabled(@NonNull FeatureFlags flags) {
        DesktopExperienceFlags desktopExperienceFlags = new DesktopExperienceFlags(flags);
        boolean result = desktopExperienceFlags.rotationConnectedDisplaySetting()
                || desktopExperienceFlags.resolutionAndEnableConnectedDisplaySetting()
                || desktopExperienceFlags.displayTopologyPaneInDisplayList();
        return result;
    }
}
