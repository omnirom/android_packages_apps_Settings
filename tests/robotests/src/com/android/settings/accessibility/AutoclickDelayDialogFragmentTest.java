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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;

import static com.android.settings.accessibility.AutoclickUtils.AUTOCLICK_DELAY_STEP;

import static com.google.common.truth.Truth.assertThat;

import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.SeekBar;

import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class AutoclickDelayDialogFragmentTest {

    private static final int TEST_SEEK_BAR_PROGRESS = 5;
    private AutoclickDelayDialogFragment mFragment;
    private AlertDialog mDialog;
    private FragmentScenario<AutoclickDelayDialogFragment> mFragmentScenario;

    @Before
    public void setUp() {
        Settings.Secure.putInt(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        mFragmentScenario = FragmentScenario.launch(
                AutoclickDelayDialogFragment.class,
                new Bundle(),
                R.style.Theme_AlertDialog_SettingsLib,
                Lifecycle.State.INITIALIZED);

        mFragmentScenario.moveToState(Lifecycle.State.RESUMED);

        mFragmentScenario.onFragment(fragment -> {
            assertThat(fragment.getDialog()).isNotNull();
            assertThat(fragment.requireDialog().isShowing()).isTrue();
            assertThat(fragment.requireDialog()).isInstanceOf(AlertDialog.class);
            mDialog = (AlertDialog) fragment.requireDialog();
            mFragment = fragment;
        });
    }

    @Test
    public void performClickOnDismiss_dialogCancels() {
        assertThat(mDialog.isShowing()).isTrue();
        RadioGroup radioGroup = mDialog.findViewById(R.id.autoclick_delay_before_click_value_group);
        radioGroup.check(R.id.accessibility_autoclick_dialog_800ms);

        mDialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertThat(mDialog.isShowing()).isFalse();
    }

    @Test
    public void performClickOn800ms_updatesAutoclickDelay() {
        assertThat(mDialog.isShowing()).isTrue();
        RadioGroup radioGroup = mDialog.findViewById(R.id.autoclick_delay_before_click_value_group);
        radioGroup.check(R.id.accessibility_autoclick_dialog_800ms);

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertDelayUpdatedInSettings(800);
    }

    @Test
    public void performClickOnNonCustomRadioButton_sliderGroupIsGone() {
        assertThat(mDialog.isShowing()).isTrue();
        Group sliderContainer = mDialog.findViewById(R.id.sliderContainer);
        assertThat(sliderContainer.getVisibility()).isEqualTo(View.GONE);

        RadioGroup radioGroup = mDialog.findViewById(
                R.id.autoclick_delay_before_click_value_group);
        radioGroup.check(R.id.accessibility_autoclick_dialog_custom);

        assertThat(sliderContainer.getVisibility()).isEqualTo(View.VISIBLE);

        radioGroup.check(R.id.accessibility_autoclick_dialog_800ms);
        assertThat(sliderContainer.getVisibility()).isEqualTo(View.GONE);
    }

    @Test
    public void performUpdateOnSeekBar_updatesAutoclickDelay() {
        assertThat(mDialog.isShowing()).isTrue();
        RadioGroup radioGroup = mDialog.findViewById(
                R.id.autoclick_delay_before_click_value_group);
        radioGroup.check(R.id.accessibility_autoclick_dialog_custom);

        SeekBar customProgressBar = mDialog.findViewById(
                R.id.accessibility_autoclick_custom_slider);
        assertThat(customProgressBar.getVisibility()).isEqualTo(View.VISIBLE);
        customProgressBar.setProgress(TEST_SEEK_BAR_PROGRESS);
        ShadowLooper.idleMainLooper();

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();
        assertDelayUpdatedInSettings(TEST_SEEK_BAR_PROGRESS * AUTOCLICK_DELAY_STEP);
    }

    @Test
    public void clickIncreaseAndDecreaseImageView_updatesAutoclickDelay() {
        assertThat(mDialog.isShowing()).isTrue();
        RadioGroup radioGroup = mDialog.findViewById(
                R.id.autoclick_delay_before_click_value_group);
        radioGroup.check(R.id.accessibility_autoclick_dialog_custom);

        SeekBar customProgressBar = mDialog.findViewById(
                R.id.accessibility_autoclick_custom_slider);
        assertThat(customProgressBar.getVisibility()).isEqualTo(View.VISIBLE);
        customProgressBar.setProgress(TEST_SEEK_BAR_PROGRESS);
        ShadowLooper.idleMainLooper();

        ImageView decreaseButton = mDialog.findViewById(
                R.id.accessibility_autoclick_custom_value_decrease);
        decreaseButton.callOnClick();
        decreaseButton.callOnClick();
        assertThat(customProgressBar.getProgress()).isEqualTo(TEST_SEEK_BAR_PROGRESS - 2);

        ImageView increaseButton = mDialog.findViewById(
                R.id.accessibility_autoclick_custom_value_increase);
        increaseButton.callOnClick();
        assertThat(customProgressBar.getProgress()).isEqualTo(TEST_SEEK_BAR_PROGRESS - 1);

        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        final int autoclickDelay = Settings.Secure.getInt(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        assertDelayUpdatedInSettings((TEST_SEEK_BAR_PROGRESS - 1) * AUTOCLICK_DELAY_STEP);
    }

    private void assertDelayUpdatedInSettings(int expectedDelay) {
        final int autoclickDelay = Settings.Secure.getInt(
                ApplicationProvider.getApplicationContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        assertThat(autoclickDelay).isEqualTo(expectedDelay);
    }
}
