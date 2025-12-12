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

package com.android.settings.accessibility;

import static android.view.accessibility.Flags.FLAG_FORCE_INVERT_COLOR;

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link ForceInvertPreferenceController}. */
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
@RunWith(RobolectricTestRunner.class)
public class ForceInvertPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private Resources mResources;
    @Mock
    private PreferenceScreen mScreen;
    @Mock
    private PreferenceCategory mPreferenceCategory;

    @Spy
    private final Context mContext = ApplicationProvider.getApplicationContext();
    private ForceInvertPreferenceController mController;
    private SelectorWithWidgetPreference mStandardDarkThemePreference;
    private SelectorWithWidgetPreference mExpandedDarkThemePreference;

    @Before
    public void setUp() {
        when(mContext.getResources()).thenReturn(mResources);
        when(mResources.getConfiguration()).thenReturn(new Configuration());
        mController = new ForceInvertPreferenceController(mContext, "dark_theme_group");

        mStandardDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mStandardDarkThemePreference
                .setKey(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY);
        mExpandedDarkThemePreference = new SelectorWithWidgetPreference(mContext);
        mExpandedDarkThemePreference
                .setKey(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY);
        when(mScreen.findPreference(mController.getPreferenceKey()))
                .thenReturn(mPreferenceCategory);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.STANDARD_DARK_THEME_KEY))
                .thenReturn(mStandardDarkThemePreference);
        when(mPreferenceCategory
                .findPreference(ForceInvertPreferenceController.EXPANDED_DARK_THEME_KEY))
                .thenReturn(mExpandedDarkThemePreference);
    }

    @Test
    @DisableFlags(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOff_shouldReturnUnsupported() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.UNSUPPORTED_ON_DEVICE);
    }

    @Test
    @EnableFlags(FLAG_FORCE_INVERT_COLOR)
    public void getAvailabilityStatus_flagOn_shouldReturnAvailable() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void displayPreference_forceInvertOff_reflectsCorrectValue() {
        setForceInvertEnabled(false);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
    }

    @Test
    public void displayPreference_forceInvertOn_reflectsCorrectValue() {
        setForceInvertEnabled(true);

        mController.displayPreference(mScreen);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
    }

    @Test
    public void clickStandardPreference_settingChanges() {
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mStandardDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isTrue();
        assertThat(mExpandedDarkThemePreference.isChecked()).isFalse();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isFalse();
    }

    @Test
    public void clickExpandedPreference_settingChanges() {
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        assertThat(mStandardDarkThemePreference.isChecked()).isFalse();
        assertThat(mExpandedDarkThemePreference.isChecked()).isTrue();
        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isTrue();
    }

    @Test
    public void clickExpandedPreference_samePreference_noChange() {
        setForceInvertEnabled(true);
        mController.displayPreference(mScreen);

        mController.onRadioButtonClicked(mExpandedDarkThemePreference);

        boolean isForceInvertEnabled = Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, /* def= */ -1) == ON;
        assertThat(isForceInvertEnabled).isTrue();
    }

    private void setForceInvertEnabled(boolean enabled) {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_FORCE_INVERT_COLOR_ENABLED, enabled ? ON : OFF);
    }
}
