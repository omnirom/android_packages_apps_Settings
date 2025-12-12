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

package com.android.settings.display

import android.content.Context
import android.hardware.devicestate.DeviceStateManager
import android.os.Handler
import android.os.Looper
import com.android.internal.annotations.VisibleForTesting
import com.android.settingslib.devicestate.AndroidSecureSettings
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManager
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManagerProvider.createInstance
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingUtils.isDeviceStateRotationLockEnabled
import com.android.settingslib.devicestate.PostureDeviceStateConverter
import com.android.settingslib.utils.ThreadUtils
import java.util.Optional

/**
 * Provides appropriate instance of [DeviceStateAutoRotateSettingManager], based on the value of
 * [Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR].
 */
object DeviceStateAutoRotateSettingManagerProvider {
    /**
     * Will be:
     * - null if the singleton is not initialized yet
     * - empty optional if the current device is not supported by the manager
     * - non-empty optional if the current device is supported by the manager
     */
    private var singletonSettingManager: Optional<DeviceStateAutoRotateSettingManager>? = null

    /**
     * Provides a singleton instance of [DeviceStateAutoRotateSettingManager], based on the value
     * of[Flags.FLAG_ENABLE_DEVICE_STATE_AUTO_ROTATE_SETTING_REFACTOR]. It is supposed to be used by
     * apps that don't support dagger to provide and manager instance. Returns null if the current
     * device is not supported by the manager.
     */
    @JvmStatic
    fun getSingletonInstance(context: Context): DeviceStateAutoRotateSettingManager? {
        val managerOptional =
            singletonSettingManager ?: createManager(context).also { singletonSettingManager = it }

        return managerOptional.orElse(null)
    }

    private fun createManager(context: Context): Optional<DeviceStateAutoRotateSettingManager> {
        val deviceStateManager =
            context.getSystemService(DeviceStateManager::class.java) ?: return Optional.empty()
        if (!isDeviceStateRotationLockEnabled(context)) {
            return Optional.empty()
        }

        return Optional.of(
            createInstance(
                context,
                ThreadUtils.getBackgroundExecutor(),
                AndroidSecureSettings(context.contentResolver),
                Handler(Looper.getMainLooper()),
                PostureDeviceStateConverter(context, deviceStateManager),
            )
        )
    }

    /** Resets the singleton instance of [DeviceStateAutoRotateSettingManager]. */
    @JvmStatic
    @VisibleForTesting
    fun resetInstance() {
        singletonSettingManager = null
    }
}
