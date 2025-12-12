/*
 * Copyright 2025 The Android Open Source Project
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

import com.android.settings.core.instrumentation.SettingsStatsLog;

import java.util.HashMap;
import java.util.Map;

/**
 * A store for holding and managing metrics loggers for external displays.
 * This allows different UI components to share the same state for logging purposes.
 */
public class ExternalDisplaySettingsLoggerStore {
    private static final Map<Integer, ExternalDisplayMetricsLogger> sLoggers =
            new HashMap<>();

    /**
     * Gets the logger instance for a given display ID. Creates one if it doesn't exist.
     */
    public static synchronized ExternalDisplayMetricsLogger getLogger(int displayId) {
        return sLoggers.computeIfAbsent(displayId, key -> new ExternalDisplayMetricsLogger());
    }

    /**
     * Removes all logger instances from the store. Should be called when the settings page
     * is destroyed.
     */
    public static synchronized void removeAllLoggers() {
        sLoggers.clear();
    }

    /**
     * A state holder and logger for a single external display's settings.
     */
    public static class ExternalDisplayMetricsLogger {
        private int mDisplayWidth = 0;
        private int mDisplayHeight = 0;
        private int mDisplayRotation = 0;
        private float mDisplaySizePercentage = 0.0f;

        ExternalDisplayMetricsLogger() {}

        /** Updates the width and height of the display. */
        public void updateResolution(int width, int height) {
            mDisplayWidth = width;
            mDisplayHeight = height;
        }

        /** Updates the rotation of the display. */
        public void updateRotation(int rotationInDegrees) {
            mDisplayRotation = rotationInDegrees;
        }

        /** Updates the display size percentage of the display. */
        public void updateDisplaySize(float percentage) {
            mDisplaySizePercentage = percentage;
        }

        /** Logs the current state of all settings. */
        public void log(int setting) {
            SettingsStatsLog.write(
                    SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED,
                    setting,
                    mDisplayWidth,
                    mDisplayHeight,
                    mDisplayRotation,
                    mDisplaySizePercentage);
        }
    }
}
