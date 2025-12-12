/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.settings.notification.app;

import static android.app.NotificationManager.IMPORTANCE_DEFAULT;
import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.NotificationManager.IMPORTANCE_MIN;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.Utils;

public class ImportancePreference extends Preference {

    private boolean mIsConfigurable = true;
    private int mImportance;
    private View mSilenceButton;
    private View mAlertButton;
    Drawable selectedBackground;
    Drawable unselectedBackground;
    private static final int BUTTON_ANIM_TIME_MS = 100;

    public ImportancePreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public ImportancePreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ImportancePreference(Context context) {
        super(context);
        init(context);
    }

    private void init(Context context) {
        selectedBackground = context.getDrawable(
                R.drawable.notification_importance_button_background_selected);
        unselectedBackground = context.getDrawable(
                R.drawable.notification_importance_button_background_unselected);
        int layoutId = android.app.Flags.notificationsRedesignTemplates()
                ? R.layout.notification_2025_importance_preference
                : R.layout.notif_importance_preference;
        setLayoutResource(layoutId);
    }

    public void setImportance(int importance) {
        mImportance = importance;
    }

    public void setConfigurable(boolean configurable) {
        mIsConfigurable = configurable;
    }

    @Override
    public void onBindViewHolder(@NonNull final PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        mSilenceButton = holder.findViewById(R.id.silence);
        mAlertButton = holder.findViewById(R.id.alert);

        if (!mIsConfigurable) {
            mSilenceButton.setEnabled(false);
            mAlertButton.setEnabled(false);
        }

        setImportanceSummary((ViewGroup) holder.itemView, mImportance, false);
        switch (mImportance) {
            case IMPORTANCE_MIN:
            case IMPORTANCE_LOW:
                mAlertButton.setBackground(unselectedBackground);
                mSilenceButton.setBackground(selectedBackground);
                mSilenceButton.setSelected(true);
                break;
            case IMPORTANCE_HIGH:
            default:
                mSilenceButton.setBackground(unselectedBackground);
                mAlertButton.setBackground(selectedBackground);
                mAlertButton.setSelected(true);
                break;
        }

        mSilenceButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_LOW);
            mAlertButton.setBackground(unselectedBackground);
            mSilenceButton.setBackground(selectedBackground);
            setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_LOW, true);
            // a11y service won't always read the newly appearing text in the right order if the
            // selection happens too soon (readback happens on a different thread as layout). post
            // the selection to make that conflict less likely
            holder.itemView.post(() -> {
                mAlertButton.setSelected(false);
                mSilenceButton.setSelected(true);
            });
        });
        mAlertButton.setOnClickListener(v -> {
            callChangeListener(IMPORTANCE_DEFAULT);
            mSilenceButton.setBackground(unselectedBackground);
            mAlertButton.setBackground(selectedBackground);
            setImportanceSummary((ViewGroup) holder.itemView, IMPORTANCE_DEFAULT, true);
            holder.itemView.post(() -> {
                mSilenceButton.setSelected(false);
                mAlertButton.setSelected(true);
            });
        });
    }

    private ColorStateList getSelectedColor() {
        return Utils.getColorAttr(getContext(),
                R.attr.notification_importance_button_foreground_color_selected);
    }

    private ColorStateList getUnselectedColor() {
        return Utils.getColorAttr(getContext(),
                R.attr.notification_importance_button_foreground_color_unselected);
    }

    void setImportanceSummary(ViewGroup parent, int importance, boolean fromUser) {
        if (fromUser) {
            AutoTransition transition = new AutoTransition();
            transition.setDuration(BUTTON_ANIM_TIME_MS);
            TransitionManager.beginDelayedTransition(parent, transition);
        }

        ColorStateList colorSelected = getSelectedColor();
        ColorStateList colorUnselected = getUnselectedColor();

        if (importance >= IMPORTANCE_DEFAULT) {
            parent.findViewById(R.id.silence_summary).setVisibility(GONE);
            ((ImageView) parent.findViewById(R.id.silence_icon)).setImageTintList(colorUnselected);
            ((TextView) parent.findViewById(R.id.silence_label)).setTextColor(colorUnselected);

            ((ImageView) parent.findViewById(R.id.alert_icon)).setImageTintList(colorSelected);
            ((TextView) parent.findViewById(R.id.alert_label)).setTextColor(colorSelected);

            parent.findViewById(R.id.alert_summary).setVisibility(VISIBLE);
        } else {
            parent.findViewById(R.id.alert_summary).setVisibility(GONE);
            ((ImageView) parent.findViewById(R.id.alert_icon)).setImageTintList(colorUnselected);
            ((TextView) parent.findViewById(R.id.alert_label)).setTextColor(colorUnselected);

            ((ImageView) parent.findViewById(R.id.silence_icon)).setImageTintList(colorSelected);
            ((TextView) parent.findViewById(R.id.silence_label)).setTextColor(colorSelected);
            parent.findViewById(R.id.silence_summary).setVisibility(VISIBLE);
        }
    }
}
