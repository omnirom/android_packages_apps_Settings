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

package com.android.settings.inputmethod;

import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG;

import android.app.Activity;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.android.settings.R;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;

import com.google.android.material.slider.Slider;

import org.jspecify.annotations.Nullable;

import java.util.concurrent.TimeUnit;

public abstract class KeyboardAccessibilityKeysDialogFragment extends DialogFragment {
    private static final long MILLISECOND_IN_SECONDS = TimeUnit.SECONDS.toMillis(1);
    protected static final String EXTRA_TITLE_RES = "extra_title_res";
    protected static final String EXTRA_SUBTITLE_RES = "extra_subtitle_res";
    protected static final String EXTRA_SEEKBAR_CONTENT_DESCRIPTION =
            "extra_seekbar_content_description_res";

    protected final MetricsFeatureProvider mMetricsFeatureProvider;

    public KeyboardAccessibilityKeysDialogFragment() {
        mMetricsFeatureProvider = FeatureFactory.getFeatureFactory().getMetricsFeatureProvider();
    }

    protected void updateInputSettingKeysValue(int thresholdTimeMillis) {
    }

    protected void onCustomValueUpdated(int thresholdTimeMillis) {
    }

    protected int getInputSettingKeysValue() {
        return 0;
    }

    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        int titleRes = getArguments().getInt(EXTRA_TITLE_RES);
        int subtitleRes = getArguments().getInt(EXTRA_SUBTITLE_RES);
        int seekbarContentDescriptionRes = getArguments().getInt(EXTRA_SEEKBAR_CONTENT_DESCRIPTION);

        Activity activity = getActivity();
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);
        View dialoglayout =
                LayoutInflater.from(/*dialogBuilder.getContext()*/activity).inflate(
                        R.layout.dialog_keyboard_a11y_input_setting_keys, null);
        dialogBuilder.setView(dialoglayout);
        Button doneButton = dialoglayout.findViewById(R.id.done_button);
        doneButton.setOnClickListener(v -> {
            RadioGroup radioGroup =
                    dialoglayout.findViewById(
                            R.id.input_setting_keys_value_group);
            Slider slider = dialoglayout.findViewById(
                    R.id.input_setting_keys_value_custom_slider);
            RadioButton customRadioButton = dialoglayout.findViewById(
                    R.id.input_setting_keys_value_custom);
            int threshold;
            if (customRadioButton.isChecked()) {
                threshold = (int) (slider.getValue() * MILLISECOND_IN_SECONDS);
            } else {
                int checkedRadioButtonId = radioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId == R.id.input_setting_keys_value_600) {
                    threshold = 600;
                } else if (checkedRadioButtonId
                        == R.id.input_setting_keys_value_400) {
                    threshold = 400;
                } else if (checkedRadioButtonId
                        == R.id.input_setting_keys_value_200) {
                    threshold = 200;
                } else {
                    threshold = 0;
                }
            }
            updateInputSettingKeysValue(threshold);
            onCustomValueUpdated(threshold);
            dismiss();
        });

        Button cancelButton = dialoglayout.findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(v -> {
            dismiss();
        });
        AlertDialog accessibilityKeyDialog = dialogBuilder.create();
        accessibilityKeyDialog.setOnShowListener(dialog -> {
            RadioGroup cannedValueRadioGroup = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_group);
            RadioButton customRadioButton = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom);
            TextView customValueTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom_value);
            View seekbarView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_custom_seekbar_layout);
            Slider customProgressSlider = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_value_custom_slider);
            TextView titleTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_dialog_title);
            TextView subTitleTextView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_dialog_subtitle);
            titleTextView.setText(titleRes);
            subTitleTextView.setText(subtitleRes);

            if (seekbarContentDescriptionRes != 0) {
                customProgressSlider.setContentDescription(
                        activity.getString(seekbarContentDescriptionRes));
            }
            View customValueView = accessibilityKeyDialog.findViewById(
                    R.id.input_setting_keys_custom_value_option);
            customValueView.setOnClickListener(l -> customRadioButton.performClick());
            customRadioButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    cannedValueRadioGroup.clearCheck();
                }
                customValueTextView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                customValueTextView.setText(
                        progressToThresholdInSecond(customProgressSlider.getValue()));
                seekbarView.setVisibility(isChecked ? View.VISIBLE : View.GONE);
                buttonView.setChecked(isChecked);
            });
            cannedValueRadioGroup.setOnCheckedChangeListener(
                    (group, checkedId) -> customRadioButton.setChecked(false));
            customProgressSlider.clearOnChangeListeners();
            customProgressSlider.addOnChangeListener((slider, v, b) -> {
                String thresholdInSecond = progressToThresholdInSecond(v);
                customValueTextView.setText(thresholdInSecond);
                customProgressSlider.setContentDescription(thresholdInSecond);
            });
            if (cannedValueRadioGroup.getCheckedRadioButtonId() == -1
                    && !customRadioButton.isChecked()) {
                //if canned radio group and custom are not select, initial check state from input
                // setting
                initStateBasedOnThreshold(cannedValueRadioGroup, customRadioButton,
                        customValueTextView,
                        customProgressSlider, seekbarView);
            } else if (customRadioButton.isChecked()) {
                cannedValueRadioGroup.clearCheck();
                customRadioButton.setChecked(true);
                customValueTextView.setVisibility(View.VISIBLE);
                customValueTextView.setText(
                        progressToThresholdInSecond(customProgressSlider.getValue()));
                seekbarView.setVisibility(View.VISIBLE);
            }
        });

        final Window window = accessibilityKeyDialog.getWindow();
        window.setType(TYPE_SYSTEM_DIALOG);

        return accessibilityKeyDialog;
    }

    private String progressToThresholdInSecond(float progress) {
        return getString(R.string.input_setting_keys_dialog_option_custom, progress);
    }

    private void initStateBasedOnThreshold(RadioGroup cannedValueRadioGroup,
            RadioButton customRadioButton, TextView customValueTextView,
            Slider customProgressSlider, View seekbarView) {
        int inputSettingKeysThreshold = getInputSettingKeysValue();
        switch (inputSettingKeysThreshold) {
            case 600 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_600);
            case 400 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_400);
            case 0, 200 -> cannedValueRadioGroup.check(R.id.input_setting_keys_value_200);
            default -> {
                float thresholdInSecond =
                        (float) inputSettingKeysThreshold / MILLISECOND_IN_SECONDS;
                customValueTextView.setText(
                        progressToThresholdInSecond(thresholdInSecond));
                customProgressSlider.setValue(thresholdInSecond);
                customRadioButton.setChecked(true);
            }
        }
        seekbarView.setVisibility(customRadioButton.isChecked() ? View.VISIBLE : View.GONE);
    }
}
