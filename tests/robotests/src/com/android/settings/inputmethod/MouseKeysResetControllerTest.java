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

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.CONDITIONALLY_UNAVAILABLE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.hardware.input.InputSettings;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;

import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.ButtonPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MouseKeysResetController}. */
@RunWith(RobolectricTestRunner.class)
public class MouseKeysResetControllerTest {

    private static final String PREFERENCE_KEY = "mouse_keys_reset_button";

    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private PreferenceScreen mScreen;

    private ButtonPreference mPreference;
    private MouseKeysResetController mController;
    Context mContext = ApplicationProvider.getApplicationContext();

    @Before
    public void setUp() {
        mController = new MouseKeysResetController(mContext, PREFERENCE_KEY);
        mPreference = new ButtonPreference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        final View rootView = View.inflate(mContext, mPreference.getLayoutResource(), null);
        mPreference.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(rootView));
        when(mScreen.findPreference(mController.getPreferenceKey())).thenReturn(mPreference);
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
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_MOUSE_KEY_ENHANCEMENT)
    public void clickButton_resetMouseKeySettings() {
        InputSettings.setAccessibilityMouseKeysMaxSpeed(mContext, 3);
        InputSettings.setAccessibilityMouseKeysAcceleration(mContext, 0.3f);
        mController.displayPreference(mScreen);
        mPreference.getButton().callOnClick();

        assertThat(InputSettings.getAccessibilityMouseKeysMaxSpeed(mContext))
                .isEqualTo(InputSettings.DEFAULT_MOUSE_KEYS_MAX_SPEED);
        assertThat(InputSettings.getAccessibilityMouseKeysAcceleration(mContext))
                .isEqualTo(InputSettings.DEFAULT_MOUSE_KEYS_ACCELERATION);
        assertThat(InputSettings.isPrimaryKeysForMouseKeysEnabled(mContext)).isTrue();
    }
}
