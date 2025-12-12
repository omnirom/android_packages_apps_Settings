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

package com.android.settings.inputmethod;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.lifecycle.LifecycleOwner;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowContentResolver;

/** Tests for {@link MouseKeysAccelerationController}. */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysAccelerationControllerTest {

    private static final String KEY_CUSTOM_SLIDER = "mouse_keys_acceleration_slider";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private MouseKeysAccelerationController mController;
    Context mContext = ApplicationProvider.getApplicationContext();
    private ShadowContentResolver mShadowContentResolver;
    private LifecycleOwner mLifecycleOwner;
    private Lifecycle mLifecycle;

    @Before
    public void setUp() {
        mShadowContentResolver = Shadow.extract(mContext.getContentResolver());
        mController = new MouseKeysAccelerationController(mContext, KEY_CUSTOM_SLIDER);
        mLifecycleOwner = () -> mLifecycle;
        mLifecycle = new Lifecycle(mLifecycleOwner);
        mLifecycle.addObserver(mController);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_available_whenFlagOn() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void getAvailabilityStatus_unavailable_whenFlagOff() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void setSliderPosition_accelerationValue_shouldReturnTrue() {
        int position = 3;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isTrue();
        assertThat(mController.getSliderPosition())
                .isEqualTo(position);
    }

    @Test
    public void setSliderPosition_accelerationValueOverMaxValue_shouldReturnFalse() {
        int position = mController.getMax() + 1;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition())
                .isEqualTo(mController.convertAccelerationToProgress(
                        InputSettings.DEFAULT_MOUSE_KEYS_ACCELERATION));
    }

    @Test
    public void setSliderPosition_accelerationValueBelowMinValue_shouldReturnFalse() {
        int position = mController.getMin() - 1;

        boolean result = mController.setSliderPosition(position);

        assertThat(result).isFalse();
        assertThat(mController.getSliderPosition())
                .isEqualTo(mController.convertAccelerationToProgress(
                        InputSettings.DEFAULT_MOUSE_KEYS_ACCELERATION));
    }

    @Test
    public void onStart_shouldRegisterContentObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);

        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION)))
                .hasSize(1);
    }

    @Test
    public void onStop_shouldUnregisterContentObserver() {
        mLifecycle.handleLifecycleEvent(ON_START);
        mLifecycle.handleLifecycleEvent(ON_STOP);

        assertThat(mShadowContentResolver.getContentObservers(
                Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_ACCELERATION)))
                .isEmpty();
    }
}
