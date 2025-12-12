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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.provider.Settings;

import com.android.settings.R;
import com.android.settingslib.dream.DreamBackend;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class LowLightModePreferenceControllerTest {
    @Test
    public void testSummaryText_lowLightModeOn() {
        final @Settings.Secure.LowLightDisplayBehavior int testBehavior =
                Settings.Secure.LOW_LIGHT_DISPLAY_BEHAVIOR_LOW_LIGHT_CLOCK_DREAM;
        final String fakeSummary = "low light clock";
        final String fakeOnSummary = "On / " + fakeSummary;

        final DreamBackend mockBackend = mock(DreamBackend.class);
        final Context mockContext = mock(Context.class);

        when(mockBackend.getLowLightDisplayBehaviorEnabled()).thenReturn(true);
        when(mockBackend.getLowLightDisplayBehavior()).thenReturn(testBehavior);

        when(mockContext.getString(R.string.low_light_display_behavior_low_light_clock_dream))
                .thenReturn(fakeSummary);
        when(mockContext.getString(eq(R.string.low_light_display_behavior_summary_on), any()))
                .thenReturn(fakeOnSummary);

        assertThat(
                LowLightModePreferenceController
                        .getSummaryTextFromDreamBackend(mockContext, mockBackend))
                .isEqualTo(fakeOnSummary);
    }

    @Test
    public void testSummaryText_lowLightModeOff() {
        final String fakeSummaryOff = "Off";

        final DreamBackend mockBackend = mock(DreamBackend.class);
        final Context mockContext = mock(Context.class);

        when(mockBackend.getLowLightDisplayBehaviorEnabled()).thenReturn(false);
        when(mockContext.getString(R.string.low_light_display_behavior_summary_off))
                .thenReturn(fakeSummaryOff);

        assertThat(
                LowLightModePreferenceController
                        .getSummaryTextFromDreamBackend(mockContext, mockBackend))
                .isEqualTo(fakeSummaryOff);
    }
}
