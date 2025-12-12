/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.fuelgauge.batteryusage;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.fuelgauge.WarningFramePreference;
import com.android.settingslib.widget.SettingsThemeHelper;
import com.android.settingslib.widget.theme.R.style;

/**
 * Custom preference for displaying battery usage info as a bar and an icon on the left for the
 * subsystem/app type.
 *
 * <p>The battery usage info could be usage percentage or usage time. The preference won't show any
 * icon if it is null.
 */
public class PowerGaugePreference extends WarningFramePreference {

    // Please see go/battery-usage-app-list-alpha
    private static final float SELECTABLE_ALPHA = 1f;
    private static final float UNSELECTABLE_ALPHA_LIGHT_MODE = 0.65f;
    private static final float UNSELECTABLE_ALPHA_DARK_MODE = 0.65f;

    private final int mTitleColorNormal;

    private BatteryEntry mInfo;
    private BatteryDiffEntry mBatteryDiffEntry;
    private CharSequence mContentDescription;
    private CharSequence mProgress;
    private CharSequence mProgressContentDescription;

    public PowerGaugePreference(
            Context context, Drawable icon, CharSequence contentDescription, BatteryEntry info) {
        this(context, null, icon, contentDescription, info);
    }

    public PowerGaugePreference(Context context) {
        this(context, null, null, null, null);
    }

    public PowerGaugePreference(Context context, AttributeSet attrs) {
        this(context, attrs, null, null, null);
    }

    private PowerGaugePreference(
            Context context,
            AttributeSet attrs,
            Drawable icon,
            CharSequence contentDescription,
            BatteryEntry info) {
        super(context, attrs);
        if (icon != null) {
            setIcon(icon);
        }
        setWidgetLayoutResource(R.layout.preference_widget_summary);
        mInfo = info;
        mContentDescription = contentDescription;
        mTitleColorNormal =
                Utils.getColorAttrDefaultColor(context, android.R.attr.textColorPrimary);
    }

    /** Sets the content description. */
    public void setContentDescription(String name) {
        mContentDescription = name;
        notifyChanged();
    }

    /** Sets the percentage to show. */
    public void setPercentage(CharSequence percentage) {
        mProgress = percentage;
        mProgressContentDescription = percentage;
        notifyChanged();
    }

    /** Sets the content description of the percentage. */
    public void setPercentageContentDescription(CharSequence contentDescription) {
        mProgressContentDescription = contentDescription;
        notifyChanged();
    }

    /** Gets the percentage to show. */
    public String getPercentage() {
        return mProgress.toString();
    }

    public void setBatteryDiffEntry(BatteryDiffEntry entry) {
        mBatteryDiffEntry = entry;
    }

    BatteryEntry getInfo() {
        return mInfo;
    }

    BatteryDiffEntry getBatteryDiffEntry() {
        return mBatteryDiffEntry;
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);

        final boolean isNightMode = Utils.isNightMode(getContext());
        final float alpha =
                isSelectable()
                        ? SELECTABLE_ALPHA
                        : (isNightMode
                                ? UNSELECTABLE_ALPHA_DARK_MODE
                                : UNSELECTABLE_ALPHA_LIGHT_MODE);
        setViewAlpha(view.itemView, alpha);

        final TextView subtitle = (TextView) view.findViewById(R.id.widget_summary);
        if (SettingsThemeHelper.isExpressiveTheme(getContext())) {
            subtitle.setTextAppearance(style.TextAppearance_SettingsLib_TitleMedium);
            subtitle.setTextColor(mTitleColorNormal);
        }
        subtitle.setText(mProgress);
        if (!TextUtils.isEmpty(mProgressContentDescription)) {
            subtitle.setContentDescription(mProgressContentDescription);
        }
        if (mContentDescription != null) {
            final TextView titleView = (TextView) view.findViewById(android.R.id.title);
            titleView.setContentDescription(mContentDescription);
        }

        if (!isSelectable()) {
            // Set colors consistently to meet contrast requirements for non-selectable items
            ((TextView) view.findViewById(android.R.id.title)).setTextColor(mTitleColorNormal);
            ((TextView) view.findViewById(android.R.id.summary)).setTextColor(mTitleColorNormal);
        }
    }

    private static void setViewAlpha(View view, float alpha) {
        if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            for (int i = viewGroup.getChildCount() - 1; i >= 0; i--) {
                setViewAlpha(viewGroup.getChildAt(i), alpha);
            }
        } else {
            view.setAlpha(alpha);
        }
    }
}
