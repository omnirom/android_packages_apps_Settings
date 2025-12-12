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

package com.android.settings.gestures

import android.content.ContextWrapper
import android.content.res.Resources
import android.platform.test.annotations.DisableFlags
import android.platform.test.annotations.EnableFlags
import android.provider.Settings.Secure.CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED
import android.service.quickaccesswallet.Flags as QuickFlags
import com.android.settings.R
import com.android.settings.flags.Flags
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
import com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE
import com.android.settings.testutils2.SettingsCatalystTestCase
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

// LINT.IfChange
class DoubleTapPowerScreenTest : SettingsCatalystTestCase() {
    override val flagName: String
        get() = Flags.FLAG_DEEPLINK_SYSTEM_25Q4

    private val mockResources = mock<Resources>()

    private val context =
        object : ContextWrapper(appContext) {
            override fun getResources(): Resources = mockResources
        }

    override val preferenceScreenCreator = DoubleTapPowerScreen(context)

    val doubleTapPowerToOpenCameraDataStore =
        DoubleTapPowerToOpenCameraPreference.createDataStore(context)

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun isAvailable_flagEnabled_configIsMultiTargetMode_returnsTrue() {
        mockResources.stub {
            on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_MULTI_TARGET_MODE
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun isAvailable_flagEnabled_configIsCameraLaunchMode_returnsTrue() {
        mockResources.stub {
            on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
        }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun isAvailable_flagEnabled_configIsDisabledMode_returnsFalse() {
        mockResources.stub { on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_DISABLED_MODE }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun isAvailable_flagDisabled_configIsTrue_returnsTrue() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn true }
        assertThat(preferenceScreenCreator.isAvailable(context)).isTrue()
    }

    @Test
    @DisableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun isAvailable_flagDisabled_configIsFalse_returnsFalse() {
        mockResources.stub { on { getBoolean(anyInt()) } doReturn false }
        assertThat(preferenceScreenCreator.isAvailable(context)).isFalse()
    }

    @Test
    @DisableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getTitle_flagDisabled_cameraLaunchTitleIsDisplayed() {
        assertThat(preferenceScreenCreator.getTitle(context))
            .isEqualTo(context.getText(R.string.double_tap_power_for_camera_title))
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getTitle_flagEnabled_configIsCameraLaunchMode_cameraLaunchTitleIsDisplayed() {
        mockResources.stub {
            on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
        }

        assertThat(preferenceScreenCreator.getTitle(context))
            .isEqualTo(context.getText(R.string.double_tap_power_for_camera_title))
    }

    @Test
    @DisableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagDisabled_doubleTapPowerEnabled_returnsOn() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            true,
        )
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(R.string.gesture_setting_on))
    }

    @Test
    @DisableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagDisabled_doubleTapPowerDisabled_returnsOff() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            true,
        )
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(R.string.gesture_setting_off))
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagEnabled_doubleTapPowerEnabled_configIsCameraLaunchMode_returnsOn() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            true,
        )
        mockResources.stub {
            on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
        }
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(R.string.gesture_setting_on))
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagEnabled_doubleTapPowerDisabled_configIsCameraLaunchMode_returnsOff() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            false,
        )
        mockResources.stub {
            on { getInteger(anyInt()) } doReturn DOUBLE_TAP_POWER_LAUNCH_CAMERA_MODE
        }
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(R.string.gesture_setting_off))
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagEnabled_doubleTapPowerDisabled_returnsOff() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            false,
        )
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(context.getText(R.string.gesture_setting_off))
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagEnabled_doubleTapPowerEnabled_cameraTargetAction_returnsSummary() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            true,
        )
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForCameraLaunch(context)
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.double_tap_power_summary,
                    context.getText(R.string.gesture_setting_on),
                    context.getText(R.string.double_tap_power_camera_action_summary),
                )
            )
    }

    @Test
    @EnableFlags(QuickFlags.FLAG_LAUNCH_WALLET_OPTION_ON_POWER_DOUBLE_TAP)
    fun getSummary_flagEnabled_doubleTapPowerEnabled_walletTargetAction_returnsSummary() {
        doubleTapPowerToOpenCameraDataStore.setBoolean(
            CAMERA_DOUBLE_TAP_POWER_GESTURE_DISABLED,
            true,
        )
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonForWalletLaunch(context)
        assertThat(preferenceScreenCreator.getSummary(context))
            .isEqualTo(
                context.getString(
                    R.string.double_tap_power_summary,
                    context.getText(R.string.gesture_setting_on),
                    context.getText(R.string.double_tap_power_wallet_action_summary),
                )
            )
    }
}
// LINT.ThenChange(DoubleTapPowerPreferenceControllerTest.java)
