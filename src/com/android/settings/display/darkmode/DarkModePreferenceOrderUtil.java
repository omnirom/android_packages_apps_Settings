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
package com.android.settings.display.darkmode;

/**
 * Utility class for managing dark theme preference ordering.
 */
public final class DarkModePreferenceOrderUtil {

    /**
     * Represents the unique identifiers for various menu items in the application. Each menu item
     * is associated with an integer value.
     */
    public enum Order {

        /**
         * Order for the location connection footer.
         */
        LOCATION_CONNECTION_FOOTER(Integer.MAX_VALUE - 3),

        /**
         * Order for the expanded dark theme footer.
         */
        EXPANDED_DARK_THEME_FOOTER(Integer.MAX_VALUE - 2),

        /**
         * Order for the modes footer.
         */
        MODES_FOOTER(Integer.MAX_VALUE - 1);

        private final int mValue;

        /**
         * Constructs a {@code Order} enum constant with the specified integer value.
         *
         * @param value The unique order integer for the preference.
         */
        Order(int value) {
            this.mValue = value;
        }

        /**
         * Returns the order integer value for the preference.
         */
        public int getValue() {
            return mValue;
        }
    }

    private DarkModePreferenceOrderUtil() {}
}
