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

package com.android.settings.display;

import static android.provider.Settings.Secure.HDR_BRIGHTNESS_ENABLED;
import static android.view.Display.HdrCapabilities.HDR_TYPE_HDR10;

import static com.android.settings.core.BasePreferenceController.AVAILABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.platform.test.annotations.RequiresFlagsEnabled;
import android.platform.test.flag.junit.CheckFlagsRule;
import android.platform.test.flag.junit.DeviceFlagsValueProvider;
import android.provider.Settings;
import android.testing.TestableContext;
import android.view.Display;
import android.view.DisplayAdjustments;
import android.view.DisplayInfo;

import androidx.test.platform.app.InstrumentationRegistry;

import com.android.server.display.feature.flags.Flags;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class HdrBrightnessPreferenceControllerTest {
    private HdrBrightnessPreferenceController mPreferenceController;

    @Mock
    private DisplayManager mDisplayManager;

    @Mock
    private DisplayManagerGlobal mDisplayManagerGlobal;

    @Rule
    public final TestableContext mContext = new TestableContext(
            InstrumentationRegistry.getInstrumentation().getContext());

    @Rule
    public final CheckFlagsRule mCheckFlagsRule = DeviceFlagsValueProvider.createCheckFlagsRule();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext.addMockSystemService(DisplayManager.class, mDisplayManager);
        mPreferenceController = new HdrBrightnessPreferenceController(mContext, "test");
    }

    @Test
    @RequiresFlagsEnabled(Flags.FLAG_HDR_BRIGHTNESS_SETTING)
    public void getAvailabilityStatus_available() {
        DisplayAdjustments daj = null;

        DisplayInfo info1 = new DisplayInfo();
        info1.hdrCapabilities = new Display.HdrCapabilities(
                new int[]{HDR_TYPE_HDR10}, /* maxLuminance= */ 1000, /* maxAverageLuminance= */
                1000, /* minLuminance= */ 0);
        info1.hdrSdrRatio = 5;
        Display.Mode mode = new Display.Mode(/* modeId= */ 0, /* width= */ 800, /* height= */
                600, /* refreshRate= */ 60, /* alternativeRefreshRates= */ new float[0],
                new int[]{HDR_TYPE_HDR10});
        info1.supportedModes = new Display.Mode[]{mode};
        Display display1 = new Display(mDisplayManagerGlobal, /* displayId= */ 0, info1, daj);

        DisplayInfo info2 = new DisplayInfo();
        Display display2 = new Display(mDisplayManagerGlobal, /* displayId= */ 1, info2, daj);

        when(mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED))
                .thenReturn(new Display[]{display1, display2});

        assertEquals(AVAILABLE, mPreferenceController.getAvailabilityStatus());
    }

    @Test
    public void getAvailabilityStatus_unsupported() {
        DisplayAdjustments daj = null;
        Display display = new Display(mDisplayManagerGlobal, /* displayId= */ 0, new DisplayInfo(),
                daj);
        when(mDisplayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_ALL_INCLUDING_DISABLED))
                .thenReturn(new Display[]{display});

        assertEquals(UNSUPPORTED_ON_DEVICE, mPreferenceController.getAvailabilityStatus());
    }

    @Test
    public void isChecked_true() {
        Settings.Secure.putInt(mContext.getContentResolver(), HDR_BRIGHTNESS_ENABLED, 1);
        assertTrue(mPreferenceController.isChecked());
    }

    @Test
    public void isChecked_false() {
        Settings.Secure.putInt(mContext.getContentResolver(), HDR_BRIGHTNESS_ENABLED, 0);
        assertFalse(mPreferenceController.isChecked());
    }

    @Test
    public void setChecked_true() {
        mPreferenceController.setChecked(true);
        assertEquals(1, Settings.Secure.getInt(mContext.getContentResolver(),
                HDR_BRIGHTNESS_ENABLED, /* def= */ -1));
    }

    @Test
    public void setChecked_false() {
        mPreferenceController.setChecked(false);
        assertEquals(0, Settings.Secure.getInt(mContext.getContentResolver(),
                HDR_BRIGHTNESS_ENABLED, /* def= */ -1));
    }
}
