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

import static org.robolectric.Shadows.shadowOf;

import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowAlertDialog;

/** Tests for {@link ToggleAutoclickDelayBeforeClickController}. */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickDelayBeforeClickControllerTest {

    private static final String PREFERENCE_KEY =
            "accessibility_control_autoclick_delay_before_click";

    private static final int TEST_AUTOCLICK_DELAY_MILLISECOND = 300;
    private static final String DEFAULT_AUTOCLICK_DELAY_SUMMARY = "1 second";
    private static final String TEST_AUTOCLICK_DELAY_SUMMARY = "0.3 seconds";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private Preference mPreference;
    private Context mContext;
    private ToggleAutoclickDelayBeforeClickController mController;

    private Fragment mFragment;
    private FragmentScenario<Fragment> mFragmentScenario;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mController = new ToggleAutoclickDelayBeforeClickController(mContext, PREFERENCE_KEY);
        mPreference = new Preference(mContext);
        mPreference.setKey(PREFERENCE_KEY);
        assertThat(mPreference.getKey()).isEqualTo(PREFERENCE_KEY);
        mFragmentScenario = FragmentScenario.launch(
                Fragment.class,
                new Bundle(),
                R.style.Theme_AlertDialog_SettingsLib,
                Lifecycle.State.INITIALIZED);

        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        mFragmentScenario.onFragment(fragment -> {
            mFragment = fragment;
            assertThat(fragment.getParentFragmentManager()).isNotNull();
        });
        mController.setFragment(mFragment);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_availableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getAvailabilityStatus_conditionallyUnavailableWhenFlagOn() {
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void handlePreferenceTreeClick_returnsTrue() {
        assertThat(ShadowAlertDialog.getLatestDialog()).isNull();
        assertThat(mController.handlePreferenceTreeClick(mPreference)).isTrue();
        shadowOf(Looper.getMainLooper()).idle();

        assertThat(ShadowAlertDialog.getLatestDialog().isShowing()).isTrue();
        Fragment dialogFrag = mFragment.getChildFragmentManager()
                .findFragmentByTag(ToggleAutoclickDelayBeforeClickController.TAG);
        assertThat(dialogFrag).isInstanceOf(AutoclickDelayDialogFragment.class);
    }

    @Test
    public void getSummary_matchesDelayTimeInSettings() {
        assertThat(mController.getSummary().toString()).isEqualTo(
                DEFAULT_AUTOCLICK_DELAY_SUMMARY);
        updateSetting(TEST_AUTOCLICK_DELAY_MILLISECOND);
        assertThat(mController.getSummary().toString()).isEqualTo(
                TEST_AUTOCLICK_DELAY_SUMMARY);
    }

    private void updateSetting(int value) {
        Settings.Secure.putInt(
                mContext.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                value);
    }
}
