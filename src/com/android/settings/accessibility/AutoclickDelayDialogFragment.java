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

import static android.view.accessibility.AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;

import static com.android.settings.accessibility.AutoclickUtils.AUTOCLICK_DELAY_STEP;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.Group;

import com.android.settings.R;
import com.android.settings.core.instrumentation.InstrumentedDialogFragment;

import com.google.common.collect.ImmutableBiMap;

/**
 * Fragment for creating a dialog in autoclick settings.
 */
public class AutoclickDelayDialogFragment extends InstrumentedDialogFragment {

    private static final String TAG = AutoclickDelayDialogFragment.class.getSimpleName();

    public final ImmutableBiMap<Integer, Integer> RADIO_BUTTON_ID_TO_DELAY_TIME =
            new ImmutableBiMap.Builder<Integer, Integer>()
                .put(R.id.accessibility_autoclick_dialog_600ms, 600)
                .put(R.id.accessibility_autoclick_dialog_800ms, 800)
                .put(R.id.accessibility_autoclick_dialog_1sec, 1000)
                .put(R.id.accessibility_autoclick_dialog_2sec, 2000)
                .put(R.id.accessibility_autoclick_dialog_4sec, 4000)
                .buildOrThrow();

    /** Create an AutoclickDelayDialogFragment instance. */
    public static @NonNull AutoclickDelayDialogFragment newInstance() {
        return new AutoclickDelayDialogFragment();
    }

    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK;
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_autoclick_delay_before_click, /* root= */ null);
        getRadioButtonLabels(dialogView);
        RadioGroup radioGroup = dialogView.findViewById(
                R.id.autoclick_delay_before_click_value_group);
        SeekBar customProgressBar = dialogView.findViewById(
                R.id.accessibility_autoclick_custom_slider);
        TextView customValueTextView = dialogView.findViewById(
                R.id.accessibility_autoclick_custom_value);
        Group sliderContainer = dialogView.findViewById(R.id.sliderContainer);
        ImageView decreaseButton = dialogView.findViewById(
                R.id.accessibility_autoclick_custom_value_decrease);
        ImageView increaseButton = dialogView.findViewById(
                R.id.accessibility_autoclick_custom_value_increase);

        AlertDialog alertDialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            int checkedRadioButtonId =
                                    radioGroup.getCheckedRadioButtonId();

                            int delay = AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT;
                            if (RADIO_BUTTON_ID_TO_DELAY_TIME
                                    .containsKey(checkedRadioButtonId)) {
                                delay = RADIO_BUTTON_ID_TO_DELAY_TIME.get(checkedRadioButtonId);
                            } else {
                                delay = seekBarProgressToDelay(customProgressBar.getProgress());
                            }

                            updateAutoclickDelay(delay);
                        })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .create();
        radioGroup.setOnCheckedChangeListener((buttonView, checkedId) -> {
            customValueTextView.setText(delayTimeToString(
                    seekBarProgressToDelay(customProgressBar.getProgress())));
            sliderContainer.setVisibility(
                    isCustomButtonChecked(checkedId) ? View.VISIBLE : View.GONE);
        });

        customProgressBar.setOnSeekBarChangeListener(
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(
                        @NonNull SeekBar seekBar, int progress, boolean fromUser) {
                    CharSequence threshold = delayTimeToString(seekBarProgressToDelay(progress));
                    customValueTextView.setText(threshold);
                }

                @Override
                public void onStartTrackingTouch(@NonNull SeekBar seekBar) {
                }

                @Override
                public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
                }
            });

        decreaseButton.setOnClickListener(v -> decreaseDelayByImageView(customProgressBar));
        increaseButton.setOnClickListener(v -> increaseDelayByImageView(customProgressBar));

        if (savedInstanceState == null) {
            initStateBasedOnDelay(radioGroup, customValueTextView, customProgressBar);
        }

        return alertDialog;
    }

    private void initStateBasedOnDelay(@NonNull RadioGroup radioGroup,
            @NonNull TextView customValueTextView, @NonNull SeekBar customProgressBar) {
        final int autoclickDelay = Settings.Secure.getInt(getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                AccessibilityManager.AUTOCLICK_DELAY_WITH_INDICATOR_DEFAULT);

        customValueTextView.setText(delayTimeToString(autoclickDelay));
        customProgressBar.setProgress(autoclickDelay / AUTOCLICK_DELAY_STEP);

        Integer radioButtonId = RADIO_BUTTON_ID_TO_DELAY_TIME.inverse().get(autoclickDelay);
        if (radioButtonId != null) {
            radioGroup.check(radioButtonId);
        } else {
            radioGroup.check(R.id.accessibility_autoclick_dialog_custom);
        }
    }

    private void decreaseDelayByImageView(@NonNull SeekBar customProgressBar) {
        int delay = customProgressBar.getProgress();
        if (delay > customProgressBar.getMin()) {
            delay--;
        }
        customProgressBar.setProgress(delay);
    }

    private void increaseDelayByImageView(@NonNull SeekBar customProgressBar) {
        int delay = customProgressBar.getProgress();
        if (delay < customProgressBar.getMax()) {
            delay++;
        }
        customProgressBar.setProgress(delay);
    }

    private boolean isCustomButtonChecked(int checkedId) {
        return checkedId == R.id.accessibility_autoclick_dialog_custom;
    }

    private void getRadioButtonLabels(@NonNull View dialogView) {
        for (Integer radioButtonId : RADIO_BUTTON_ID_TO_DELAY_TIME.keySet()) {
            RadioButton radioButton = dialogView.findViewById(radioButtonId);
            if (radioButton != null) {
                radioButton.setText(delayTimeToString(
                        RADIO_BUTTON_ID_TO_DELAY_TIME.get(radioButtonId)));
            }
        }
    }

    /** Converts seek bar preference progress value to autoclick delay associated with it. */
    private int seekBarProgressToDelay(int progress) {
        return progress * AUTOCLICK_DELAY_STEP;
    }

    private CharSequence delayTimeToString(int delayMillis) {
        return AutoclickUtils.getAutoclickDelaySummary(getContext(),
                R.string.accessibility_autoclick_delay_unit_second, delayMillis);
    }

    /** Updates autoclick delay time. */
    private void updateAutoclickDelay(int delay) {
        Settings.Secure.putInt(
                getContext().getContentResolver(),
                Settings.Secure.ACCESSIBILITY_AUTOCLICK_DELAY,
                delay);
    }
}
