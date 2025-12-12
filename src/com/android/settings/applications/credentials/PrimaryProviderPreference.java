/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.applications.credentials;

import android.content.Context;
import android.credentials.flags.Flags;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settingslib.RestrictedPreference;

/**
 * This preference is shown at the top of the "passwords & accounts" screen and allows the user to
 * pick their primary credential manager provider.
 */
public class PrimaryProviderPreference extends RestrictedPreference {

    public static boolean shouldUseNewSettingsUi() {
        return Flags.isCredmanSettingsExpressiveDesign();
    }

    private @Nullable Button mChangeButton = null;
    private @Nullable Button mOpenButton = null;
    private @Nullable View mButtonFrameView = null;
    private @Nullable Delegate mDelegate = null;
    private boolean mButtonsCompactMode = false;
    private boolean mOpenButtonVisible = false;

    /** Called to send messages back to the parent controller. */
    public static interface Delegate {
        void onOpenButtonClicked();

        void onChangeButtonClicked();
    }

    public PrimaryProviderPreference(
            @NonNull Context context,
            @NonNull AttributeSet attrs,
            int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initializeSettingsUi();
    }

    public PrimaryProviderPreference(
           @NonNull Context context,
           @NonNull AttributeSet attrs) {
        super(context, attrs);
        initializeSettingsUi();
    }
    private void initializeSettingsUi() {
        // Change the layout to the new settings ui.
        if (!shouldUseNewSettingsUi()) {
            setLayoutResource(R.layout.preference_credential_manager_with_buttons);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (shouldUseNewSettingsUi()) {
            onBindViewHolderNewSettingsUi(holder);
        } else {
            onBindViewHolderOldSettingsUi(holder);
        }
    }

    private void onBindViewHolderNewSettingsUi(PreferenceViewHolder holder) {
        setOnPreferenceClickListener(
                new Preference.OnPreferenceClickListener() {
                    public boolean onPreferenceClick(@NonNull Preference preference) {
                        if (mDelegate != null) {
                            mDelegate.onOpenButtonClicked();
                            return true;
                        }

                        return false;
                    }
                });

        View mWidgetFrame = holder.findViewById(android.R.id.widget_frame);
        mWidgetFrame.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onChangeButtonClicked();
                        }
                    }
                });
    }

    private void onBindViewHolderOldSettingsUi(PreferenceViewHolder holder) {
        mOpenButton = (Button) holder.findViewById(R.id.open_button);
        mOpenButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onOpenButtonClicked();
                        }
                    }
                });
        setVisibility(mOpenButton, mOpenButtonVisible);
        holder.itemView.setClickable(false);

        mChangeButton = (Button) holder.findViewById(R.id.change_button);
        mChangeButton.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(@NonNull View v) {
                        if (mDelegate != null) {
                            mDelegate.onChangeButtonClicked();
                        }
                    }
                });

        mButtonFrameView = holder.findViewById(R.id.credman_button_frame);
        updateButtonFramePadding();
    }

    public void setOpenButtonVisible(boolean isVisible) {
        if (mOpenButton != null) {
            mOpenButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
            setVisibility(mOpenButton, isVisible);
        }

        mOpenButtonVisible = isVisible;
    }

    private void updateButtonFramePadding() {
        if (mButtonFrameView == null) {
          return;
        }

        int paddingLeft = mButtonsCompactMode ?
            getContext().getResources().getDimensionPixelSize(
                R.dimen.credman_primary_provider_pref_left_padding) :
            getContext().getResources().getDimensionPixelSize(
                R.dimen.credman_primary_provider_pref_left_padding_compact);

        mButtonFrameView.setPadding(
            paddingLeft,
            mButtonFrameView.getPaddingTop(),
            paddingLeft,
            mButtonFrameView.getPaddingBottom());
    }

    public void setButtonsCompactMode(boolean isCompactMode) {
        mButtonsCompactMode = isCompactMode;
        updateButtonFramePadding();
    }

    public void setDelegate(@NonNull Delegate delegate) {
        mDelegate = delegate;
    }

    @Override
    protected int getSecondTargetResId() {
        return R.layout.preference_widget_credman_edit;
    }

    @Override
    protected boolean shouldHideSecondTarget() {
        return !shouldUseNewSettingsUi();
    }

    @VisibleForTesting
    public @Nullable Button getOpenButton() {
        return mOpenButton;
    }

    @VisibleForTesting
    public @Nullable Button getChangeButton() {
        return mChangeButton;
    }

    @VisibleForTesting
    public @Nullable View getButtonFrameView() {
        return mButtonFrameView;
    }

    private static void setVisibility(View view, boolean isVisible) {
        view.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }
}
