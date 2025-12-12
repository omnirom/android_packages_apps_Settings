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

import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.gestures.SystemNavigationGestureSettings;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
public class AutoclickNavigationSuggestionDialogFragmentTest {

    private FragmentScenario<AutoclickNavigationSuggestionDialogFragment> mFragmentScenario;

    @Before
    public void setUp() {
        // Set up the fragment.
        mFragmentScenario = FragmentScenario.launch(
                AutoclickNavigationSuggestionDialogFragment.class,
                new Bundle(),
                R.style.Theme_AlertDialog_SettingsLib,
                Lifecycle.State.RESUMED);
    }

    @Test
    public void onCreateDialog_returnsAlertDialogWithButtons() {
        mFragmentScenario.onFragment(fragment -> {
            // Verify dialog is created.
            assertThat(fragment.getDialog()).isNotNull();
            assertThat(fragment.requireDialog()).isInstanceOf(AlertDialog.class);

            AlertDialog dialog = (AlertDialog) fragment.requireDialog();

            // Verify buttons exist.
            assertThat(dialog.getButton(AlertDialog.BUTTON_NEGATIVE)).isNotNull();
            assertThat(dialog.getButton(AlertDialog.BUTTON_POSITIVE)).isNotNull();
        });
    }

    @Test
    public void performClickOnCancel_dialogDismisses() {
        mFragmentScenario.onFragment(fragment -> {
            AlertDialog dialog = (AlertDialog) fragment.requireDialog();
            assertThat(dialog.isShowing()).isTrue();

            // Click the negative button (Cancel).
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
            ShadowLooper.idleMainLooper();

            // Verify dialog dismisses.
            assertThat(dialog.isShowing()).isFalse();
        });
    }

    @Test
    public void performClickOnSettings_goToSystemNavigationGestureSettings() {
        mFragmentScenario.onFragment(fragment -> {
            AlertDialog dialog = (AlertDialog) fragment.requireDialog();
            assertThat(dialog.isShowing()).isTrue();

            // Click the positive button (Settings).
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            ShadowLooper.idleMainLooper();

            // Verify dialog dismisses.
            assertThat(dialog.isShowing()).isFalse();

            // Verify the correct intent was launched.
            Intent intent = shadowOf(
                    (ContextWrapper) dialog.getContext()).peekNextStartedActivity();
            assertThat(intent).isNotNull();
            assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
            assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                    .isEqualTo(SystemNavigationGestureSettings.class.getName());

        });
    }
}
