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

import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY;
import static android.hardware.devicestate.DeviceState.PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED;

import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.hardware.devicestate.DeviceState;
import android.hardware.devicestate.DeviceStateManager;

import com.android.internal.R;

import java.util.List;
import java.util.Set;

/**
 * Helper for testing device state auto rotate setting
 */
public class DeviceStateAutoRotateSettingTestUtils {

    public static final DeviceState DEFAULT_DEVICE_STATE = new DeviceState(
            new DeviceState.Configuration.Builder(0, "DEFAULT").build());

    public static final DeviceState FOLDED_DEVICE_STATE = new DeviceState(
            new DeviceState.Configuration.Builder(1, "FOLDED").setPhysicalProperties(
                    Set.of(PROPERTY_FOLDABLE_HARDWARE_CONFIGURATION_FOLD_IN_CLOSED,
                            PROPERTY_FOLDABLE_DISPLAY_CONFIGURATION_OUTER_PRIMARY)).build());

    /**
     * Mock {@link mockResources} and DeviceStateManager to return device state auto rotate
     * enabled or disabled based on value passed for {@link enable}.
     */
    public static void setDeviceStateRotationLockEnabled(boolean enable, Resources mockResources,
            DeviceStateManager mockDeviceStateManager) {
        // Do not change supported device states and autoRotate support config if disabling.
        if (enable) {
            if (mockDeviceStateManager != null) {
                setDeviceTypeFoldable(/* isFoldable= */ true, mockDeviceStateManager);
            }
            setAutoRotateEnabled(/* isEnable= */ true, mockResources);
        }
        setDeviceStateAutoRotateConfig(/* isNonEmpty= */ enable, mockResources);
    }

    /**
     * Mocks {@link mockResources} to return whether auto-rotation is enabled or disabled based on
     * the value passed for {@link isEnabled}.
     */
    public static void setAutoRotateEnabled(boolean isEnabled, Resources mockResources) {
        when(mockResources.getBoolean(R.bool.config_supportAutoRotation)).thenReturn(isEnabled);
    }

    /**
     * Mocks {@link mockDeviceStateManager} to return device states indicating whether the device is
     * foldable.
     */
    public static void setDeviceTypeFoldable(boolean isFoldable,
            DeviceStateManager mockDeviceStateManager) {
        if (mockDeviceStateManager != null) {
            List<DeviceState> deviceStates;
            if (isFoldable) {
                deviceStates = List.of(DEFAULT_DEVICE_STATE, FOLDED_DEVICE_STATE);
            } else {
                deviceStates = List.of(DEFAULT_DEVICE_STATE);
            }
            when(mockDeviceStateManager.getSupportedDeviceStates()).thenReturn(deviceStates);
        }
    }

    /**
     * Mocks {@link mockResources} to return a non-empty or empty array for
     * {@code config_perDeviceStateRotationLockDefaults}.
     */
    public static void setDeviceStateAutoRotateConfig(boolean isNonEmpty, Resources mockResources) {
        if (isNonEmpty) {
            when(mockResources.getStringArray(
                    R.array.config_perDeviceStateRotationLockDefaults)).thenReturn(
                        new String[]{"0:1"});
        } else {
            when(mockResources.getStringArray(
                    R.array.config_perDeviceStateRotationLockDefaults)).thenReturn(new String[]{});
        }
    }
}
