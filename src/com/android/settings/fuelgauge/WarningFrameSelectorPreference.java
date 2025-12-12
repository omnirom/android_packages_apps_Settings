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

package com.android.settings.fuelgauge;

import android.content.Context;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.SelectorWithWidgetPreference;
import com.android.settingslib.widget.SettingsThemeHelper;

/**
 * Custom preference for displaying the {@link SelectorWithWidgetPreference}
 * with an optional hint chip
 */
public class WarningFrameSelectorPreference extends SelectorWithWidgetPreference {
    private final boolean mIsExpressiveTheme;
    private final int mWarningChipBackgroundResId;

    @Nullable private CharSequence mHintText;

    public WarningFrameSelectorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        mIsExpressiveTheme = SettingsThemeHelper.isExpressiveTheme(context);
        int layoutResId =
                mIsExpressiveTheme
                        ? R.layout.expressive_warning_frame_selector_preference
                        : R.layout.warning_frame_selector_preference;
        setLayoutResource(layoutResId);
        mWarningChipBackgroundResId =
                mIsExpressiveTheme
                        ? R.drawable.expressive_battery_hints_chip_bg_ripple
                        : R.drawable.battery_hints_chip_bg_ripple;
    }

    /** Sets the text of hint to show. */
    public void setHint(@Nullable CharSequence hintText) {
        if (!TextUtils.equals(mHintText, hintText)) {
            mHintText = hintText;
            notifyChanged();
        }
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        if (mIsExpressiveTheme) {
            final View preferenceFrame = view.findViewById(R.id.preference_frame);
            preferenceFrame.setBackground(null);
            preferenceFrame.setPadding(0, 0, 0, 0);
        }

        final View warningChipFrame = view.findViewById(R.id.warning_chip_frame);
        if (!TextUtils.isEmpty(mHintText)) {
            ((TextView) warningChipFrame.findViewById(R.id.warning_info)).setText(mHintText);
            warningChipFrame.setVisibility(View.VISIBLE);
            warningChipFrame
                    .findViewById(R.id.warning_chip)
                    .setBackgroundResource(mWarningChipBackgroundResId);
        } else {
            warningChipFrame.setVisibility(View.GONE);
        }
    }
}
