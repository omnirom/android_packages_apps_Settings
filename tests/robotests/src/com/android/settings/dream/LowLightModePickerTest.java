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

package com.android.settings.dream;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.dream.DreamBackend;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = SettingsShadowResources.class)
public class LowLightModePickerTest {
    private LowLightModePicker mPicker;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private DreamBackend mBackend;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final Context context = spy(ApplicationProvider.getApplicationContext());

        mPicker = spy(new LowLightModePicker());
        when(mPicker.getContext()).thenReturn(context);
        mPicker.onAttach(context);

        ReflectionHelpers.setField(mPicker, "mBackend", mBackend);
    }

    @Test
    public void testDefaultKeyReturnsCurrentLowLightDisplayBehavior() {
        when(mBackend.getLowLightDisplayBehavior())
                .thenReturn(Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(LowLightModePicker.getKeyFromSetting(
                        Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE));

        when(mBackend.getLowLightDisplayBehavior())
                .thenReturn(Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(LowLightModePicker.getKeyFromSetting(
                        Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF));

        when(mBackend.getLowLightDisplayBehavior())
                .thenReturn(Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(LowLightModePicker.getKeyFromSetting(
                        Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM));

        when(mBackend.getLowLightDisplayBehavior())
                .thenReturn(Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM);
        assertThat(mPicker.getDefaultKey())
                .isEqualTo(LowLightModePicker.getKeyFromSetting(
                        Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM));
    }

    @Test
    public void testSetDefaultKey_None() {
        final String key = LowLightModePicker.getKeyFromSetting(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE);
        mPicker.setDefaultKey(key);
        verify(mBackend).setLowLightDisplayBehavior(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NONE);
    }

    @Test
    public void testSetDefaultKey_ScreenOff() {
        final String key = LowLightModePicker.getKeyFromSetting(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF);
        mPicker.setDefaultKey(key);
        verify(mBackend).setLowLightDisplayBehavior(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_SCREEN_OFF);
    }

    @Test
    public void testSetDefaultKey_LowLightClockDream() {
        final String key = LowLightModePicker.getKeyFromSetting(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM);
        mPicker.setDefaultKey(key);
        verify(mBackend).setLowLightDisplayBehavior(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM);
    }

    @Test
    public void testSetDefaultKey_NoDream() {
        final String key = LowLightModePicker.getKeyFromSetting(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM);
        mPicker.setDefaultKey(key);
        verify(mBackend).setLowLightDisplayBehavior(
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_NO_DREAM);
    }
}
