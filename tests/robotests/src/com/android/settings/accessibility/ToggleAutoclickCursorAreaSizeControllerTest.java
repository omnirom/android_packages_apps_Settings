/*
 * Copyright 2025 The Android Open Source Project
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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.when;

import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;
import android.view.accessibility.AccessibilityManager;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

/** Tests for {@link ToggleAutoclickCursorAreaSizeController}. */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
        ShadowAlertDialogCompat.class,
})
public class ToggleAutoclickCursorAreaSizeControllerTest {

    private static final String PREFERENCE_KEY = "accessibility_control_autoclick_cursor_area_size";

    @Rule public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Rule public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Mock
    private Preference mPreference;
    private Context mContext;
    private ToggleAutoclickCursorAreaSizeController mController;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);

        when(mPreference.getKey()).thenReturn(PREFERENCE_KEY);
        mController = new ToggleAutoclickCursorAreaSizeController(mContext, PREFERENCE_KEY);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_availableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    public void getSummary() {
        mController.updateAutoclickCursorAreaSize(
                mController.RADIO_BUTTON_ID_TO_CURSOR_SIZE.get(
                        R.id.autoclick_cursor_area_size_value_large));
        assertThat(mController.getSummary()).isEqualTo(
                mContext.getString(R.string.autoclick_cursor_area_size_dialog_option_large));
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_conditionallyUnavailableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_dialogShows() {
        mController.handlePreferenceTreeClick(mPreference);

        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();

        assertThat(alertDialog.isShowing()).isTrue();
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getProgress_matchesSetting_inRangeValue() {
        updateSetting(mController.RADIO_BUTTON_ID_TO_CURSOR_SIZE.get(
                R.id.autoclick_cursor_area_size_value_extra_large));
        ShadowLooper.idleMainLooper();
        mController.handlePreferenceTreeClick(mPreference);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        RadioGroup radioGroup =  alertDialog.findViewById(
                    R.id.autoclick_cursor_area_size_value_group);
        ShadowLooper.idleMainLooper();

        assertThat(radioGroup.getCheckedRadioButtonId())
                .isEqualTo(R.id.autoclick_cursor_area_size_value_extra_large);
    }

    @Test
    public void setProgress_updatesSetting_inRangeValue() {
        mController.handlePreferenceTreeClick(mPreference);
        AlertDialog alertDialog = ShadowAlertDialogCompat.getLatestAlertDialog();
        RadioGroup radioGroup =  alertDialog.findViewById(
                    R.id.autoclick_cursor_area_size_value_group);
        ShadowLooper.idleMainLooper();
        radioGroup.check(R.id.autoclick_cursor_area_size_value_extra_large);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertThat(readSetting()).isEqualTo(100);

        mController.handlePreferenceTreeClick(mPreference);
        ShadowLooper.idleMainLooper();
        radioGroup.check(R.id.autoclick_cursor_area_size_value_extra_small);
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertThat(readSetting()).isEqualTo(20);
    }

    private int readSetting() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                AccessibilityManager.AUTOCLICK_CURSOR_AREA_SIZE_DEFAULT);
    }

    private void updateSetting(int value) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_CURSOR_AREA_SIZE,
                value);
    }
}
