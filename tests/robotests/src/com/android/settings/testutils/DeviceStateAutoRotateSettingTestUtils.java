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

package com.android.settings.testutils;

import static org.mockito.Mockito.when;

import android.content.res.Resources;

/**
 * Helper for testing device state auto rotate setting
 */
public class DeviceStateAutoRotateSettingTestUtils {

    /**
     * Mock {@link mockResources} to return device state auto rotate enabled or disabled based on
     * value passed for {@link enable}.
     */
    public static void setDeviceStateRotationLockEnabled(boolean enable, Resources mockResources) {
        String[] perDeviceStateRotationLockDefaults = new String[0];
        if (enable) {
            perDeviceStateRotationLockDefaults = new String[]{"test_value"};
        }
        when(mockResources.getStringArray(
                com.android.internal.R.array.config_perDeviceStateRotationLockDefaults))
                .thenReturn(perDeviceStateRotationLockDefaults);
    }
}
