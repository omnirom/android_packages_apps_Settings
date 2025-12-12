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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.R;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;
import com.android.settings.gestures.SystemNavigationGestureSettings;

/**
 * Fragment for creating a dialog suggesting 3-button navigation mode for Autoclick.
 */
public class AutoclickNavigationSuggestionDialogFragment extends InstrumentedDialogFragment {

    private static final String TAG =
            AutoclickNavigationSuggestionDialogFragment.class.getSimpleName();

    /** Create a new instance of the dialog fragment. */
    public static @NonNull AutoclickNavigationSuggestionDialogFragment newInstance() {
        return new AutoclickNavigationSuggestionDialogFragment();
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // Create button click listeners.
        final DialogInterface.OnClickListener cancelListener =
                (dialog, which) -> dialog.dismiss();
        final DialogInterface.OnClickListener settingsListener =
                (dialog, which) -> {
                    new SubSettingLauncher(getContext())
                            .setDestination(SystemNavigationGestureSettings.class.getName())
                            .setSourceMetricsCategory(getMetricsCategory())
                            .launch();
                    dialog.dismiss();
                };

        // Create dialog using standard title and message methods
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.accessibility_autoclick_navigation_title)
                .setMessage(R.string.accessibility_autoclick_navigation_message)
                .setNegativeButton(R.string.accessibility_autoclick_keep_gesture_navigation,
                        cancelListener)
                .setPositiveButton(R.string.accessibility_autoclick_navigation_settings,
                        settingsListener)
                .create();
    }
}
