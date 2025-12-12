/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.util.AttributeSet;
import android.view.View;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settingslib.widget.GroupSectionDividerMixin;
import com.android.settingslib.widget.SettingsThemeHelper;

/** Custom preference for displaying the app power usage time. */
public class PowerUsageTimePreference extends WarningFramePreference
        implements GroupSectionDividerMixin {
    private static final String TAG = "PowerUsageTimePreference";

    private final Context mContext;
    private final int mNonBackgroundPaddingStart;

    public PowerUsageTimePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSelectable(false);
        mContext = context;
        mNonBackgroundPaddingStart =
                mContext.getResources()
                        .getDimensionPixelSize(
                                com.android.settingslib.widget.theme.R.dimen
                                        .settingslib_expressive_space_extrasmall4);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        // Update padding start for non-background items.
        if (SettingsThemeHelper.isExpressiveTheme(mContext)) {
            addPaddingStartForNonBackgroundItem(view, R.id.preference_frame);
            addPaddingStartForNonBackgroundItem(view, R.id.warning_chip_frame);
        }
    }

    private void addPaddingStartForNonBackgroundItem(PreferenceViewHolder viewHolder, int resId) {
        final View view = viewHolder.findViewById(resId);
        if (view == null) {
            return;
        }
        view.setPadding(
                view.getPaddingStart() + mNonBackgroundPaddingStart,
                view.getPaddingTop(),
                view.getPaddingEnd(),
                view.getPaddingBottom());
    }
}
