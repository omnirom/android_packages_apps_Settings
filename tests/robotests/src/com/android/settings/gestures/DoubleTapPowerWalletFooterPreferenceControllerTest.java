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

package com.android.settings.gestures;

import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE;
import static com.android.settings.gestures.DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_MULTI_TARGET_MODE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Resources;
import android.service.quickaccesswallet.QuickAccessWalletClient;

import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.internal.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class DoubleTapPowerWalletFooterPreferenceControllerTest {

    private static final String KEY = "gesture_double_tap_power_wallet_footer";
    private Context mContext;
    private Resources mResources;
    private final QuickAccessWalletClient mQuickAccessWalletClient = mock(
            QuickAccessWalletClient.class);

    private DoubleTapPowerWalletFooterPreferenceController mController;
    private Preference mPreference;


    @Before
    public void setUp() {
        mContext = spy(ApplicationProvider.getApplicationContext());
        mResources = mock(Resources.class);
        when(mContext.getResources()).thenReturn(mResources);
        when(mQuickAccessWalletClient.isWalletServiceAvailable()).thenReturn(true);

        mController = new DoubleTapPowerWalletFooterPreferenceController(mContext, KEY,
                mQuickAccessWalletClient);
        mPreference = new Preference(mContext);
    }

    @Test
    public void updateState_quickAccessWalletAvailable_preferenceNotVisible() {
        when(mQuickAccessWalletClient.isWalletServiceAvailable()).thenReturn(true);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isFalse();
    }

    @Test
    public void updateState_quickAccessWalletNotAvailable_preferenceVisible() {
        when(mQuickAccessWalletClient.isWalletServiceAvailable()).thenReturn(false);

        mController.updateState(mPreference);

        assertThat(mPreference.isVisible()).isTrue();
    }

    @Test
    public void updateState_doubleTapPowerGestureDisabled_preferenceDisabled() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isFalse();
    }

    @Test
    public void updateState_doubleTapPowerGestureEnabled_preferenceEnabled() {
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        mController.updateState(mPreference);

        assertThat(mPreference.isEnabled()).isTrue();
    }


    @Test
    public void getAvailabilityStatus_setDoubleTapPowerGestureNotAvailable_preferenceUnsupported() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_DISABLED_MODE);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerButtonDisabled_preferenceDisabled() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_MULTI_TARGET_MODE);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, false);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.DISABLED_DEPENDENT_SETTING);
    }

    @Test
    public void getAvailabilityStatus_setDoubleTapPowerWalletLaunchEnabled_preferenceEnabled() {
        when(mResources.getInteger(R.integer.config_doubleTapPowerGestureMode)).thenReturn(
                DOUBLE_TAP_POWER_MULTI_TARGET_MODE);
        DoubleTapPowerSettingsUtils.setDoubleTapPowerButtonGestureEnabled(mContext, true);

        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE_UNSEARCHABLE);
    }
}
