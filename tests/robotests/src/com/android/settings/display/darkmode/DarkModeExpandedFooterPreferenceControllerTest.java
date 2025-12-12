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
package com.android.settings.display.darkmode;

import static com.android.settings.core.BasePreferenceController.AVAILABLE_UNSEARCHABLE;
import static com.android.settings.core.BasePreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class DarkModeExpandedFooterPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mMocks = MockitoJUnit.rule();

    private DarkModeExpandedFooterPreferenceController mController;
    private final Context mContext = ApplicationProvider.getApplicationContext();
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreference mFooter;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mController = new DarkModeExpandedFooterPreferenceController(
                mContext, "dark_theme_expanded_footer");
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mFooter);
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOn_available() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(AVAILABLE_UNSEARCHABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOff_unavailable() {
        assertThat(mController.getAvailabilityStatus()).isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void displayPreference_setOrder_expectedOrder() {
        mController.displayPreference(mScreen);

        verify(mFooter).setOrder(
                eq(DarkModePreferenceOrderUtil.Order.EXPANDED_DARK_THEME_FOOTER.getValue()));
    }
}
