/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.UiModeManager;
import android.content.Context;
import android.content.res.Resources;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.service.notification.ZenDeviceEffects;
import android.view.View;
import android.view.accessibility.Flags;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.testutils.BedtimeSettingsUtils;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settingslib.notification.modes.TestModeBuilder;
import com.android.settingslib.notification.modes.ZenMode;
import com.android.settingslib.notification.modes.ZenModesBackend;
import com.android.settingslib.widget.FooterPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

@RunWith(RobolectricTestRunner.class)
public class DarkModeCustomModesPreferenceControllerTest {

    private static final ZenMode MODE_WITH_DARK_THEME = new TestModeBuilder()
            .setDeviceEffects(new ZenDeviceEffects.Builder().setShouldUseNightMode(true).build())
            .build();

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Mock
    private UiModeManager mService;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private FooterPreference mCustomModesFooterPreference;
    @Mock
    private FooterPreference mExpandedDarkThemeFooterPreference;
    @Mock
    private ZenModesBackend mZenModesBackend;

    private DarkModeCustomModesPreferenceController mModeCustomModesPreferenceController;
    private Context mContext;
    private BedtimeSettingsUtils mBedtimeSettingsUtils;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        FakeFeatureFactory.setupForTest();

        mContext = spy(ApplicationProvider.getApplicationContext());
        mBedtimeSettingsUtils = new BedtimeSettingsUtils(mContext);

        when(mContext.getSystemService(UiModeManager.class)).thenReturn(mService);
        Resources res = spy(mContext.getResources());
        when(res.getString(com.android.internal.R.string.config_systemWellbeing))
                .thenReturn("wellbeing");
        when(mContext.getResources()).thenReturn(res);

        mModeCustomModesPreferenceController =
                new DarkModeCustomModesPreferenceController(
                        mContext, "dark_theme_custom_bedtime_footer");

        when(mScreen.findPreference(mModeCustomModesPreferenceController.getPreferenceKey()))
                .thenReturn(mCustomModesFooterPreference);
        when(mScreen.findPreference(
                DarkModeExpandedFooterPreferenceController.DARK_MODE_EXPANDED_FOOTER_KEY))
                .thenReturn(mExpandedDarkThemeFooterPreference);
        when(mExpandedDarkThemeFooterPreference.isVisible()).thenReturn(false);

        ZenModesBackend.setInstance(mZenModesBackend);
        when(mZenModesBackend.getModes()).thenReturn(List.of());
    }

    @Test
    public void displayPreference_withOneModeTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build()));

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference).setTitle("A also turns on dark theme");
        verify(mCustomModesFooterPreference).setLearnMoreAction(any());
        verify(mCustomModesFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    public void displayPreference_withTwoModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("B").build()));

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference).setTitle("A and B also turn on dark theme");
        verify(mCustomModesFooterPreference).setLearnMoreAction(any());
        verify(mCustomModesFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    public void displayPreference_withManyModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of(
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("A").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("B").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("C").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("D").build(),
                new TestModeBuilder(MODE_WITH_DARK_THEME).setName("E").build()
        ));

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference).setTitle("A, B, and 3 more also turn on dark theme");
        verify(mCustomModesFooterPreference).setLearnMoreAction(any());
        verify(mCustomModesFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    public void displayPreference_withZeroModesTogglingDarkTheme() {
        when(mZenModesBackend.getModes()).thenReturn(List.of());

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference).setTitle("Modes can also turn on dark theme");
        verify(mCustomModesFooterPreference).setLearnMoreAction(any());
        verify(mCustomModesFooterPreference).setLearnMoreText("Modes settings");
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void displayPreference_expandedDarkThemeFooterVisible_expectedOrderAndIconGone() {
        when(mExpandedDarkThemeFooterPreference.isVisible()).thenReturn(true);

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference).setIconVisibility(View.GONE);
        verify(mCustomModesFooterPreference).setOrder(
                eq(DarkModePreferenceOrderUtil.Order.MODES_FOOTER.getValue()));
    }

    @Test
    @EnableFlags(Flags.FLAG_FORCE_INVERT_COLOR)
    public void displayPreference_expandedDarkThemeFooterInvisible_neverSetIconGone() {
        when(mExpandedDarkThemeFooterPreference.isVisible()).thenReturn(false);

        mModeCustomModesPreferenceController.displayPreference(mScreen);

        verify(mCustomModesFooterPreference, never()).setIconVisibility(View.GONE);
    }
}
