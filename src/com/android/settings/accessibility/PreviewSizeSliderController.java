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

package com.android.settings.accessibility;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;

import com.google.android.material.slider.Slider;
import com.google.android.setupcompat.util.WizardManagerHelper;

import java.util.Optional;

/**
 * The controller of the display size and font size sliders that listens to
 * changes and updates preview size threshold smoothly.
 */
// LINT.IfChange
abstract class PreviewSizeSliderController extends BasePreferenceController implements
        TextReadingResetController.ResetStateListener, DefaultLifecycleObserver {
    private final PreviewSizeData<? extends Number> mSizeData;
    private boolean mSeekByTouch;
    private Optional<ProgressInteractionListener> mInteractionListener = Optional.empty();
    @Nullable
    private TooltipSliderPreference mSliderPreference;
    private int mLastValue;
    private final Handler mHandler;

    private String[] mStateLabels = null;

    private final Slider.OnChangeListener mSliderChangeListener = new Slider.OnChangeListener() {
        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            setSliderStateDescription(value);

            if (mInteractionListener.isEmpty()) {
                return;
            }

            final ProgressInteractionListener interactionListener = mInteractionListener.get();
            // Avoid timing issues to update the corresponding preview fail when clicking
            // the increase/decrease button.
            slider.post(interactionListener::notifyPreferenceChanged);
            if (!mSeekByTouch) {
                interactionListener.onProgressChanged();
                onProgressFinalized();
            }
        }
    };

    @VisibleForTesting
    @NonNull
    public Slider.OnChangeListener getSliderChangeListener() {
        return mSliderChangeListener;
    }

    private final Slider.OnSliderTouchListener mSliderTouchListener =
            new Slider.OnSliderTouchListener() {
                @Override
                public void onStartTrackingTouch(@NonNull Slider slider) {
                    mSeekByTouch = true;
                }

                @Override
                public void onStopTrackingTouch(@NonNull Slider slider) {
                    mSeekByTouch = false;

                    mInteractionListener.ifPresent(ProgressInteractionListener::onEndTrackingTouch);
                    onProgressFinalized();
                }
            };

    PreviewSizeSliderController(@NonNull Context context, @NonNull String preferenceKey,
            @NonNull PreviewSizeData<? extends Number> sizeData) {
        super(context, preferenceKey);
        mSizeData = sizeData;
        mHandler = new Handler(context.getMainLooper());
    }

    @Override
    public void onStart(@NonNull LifecycleOwner owner) {
        if (mSliderPreference != null && mSliderPreference.getNeedsQSTooltipReshow()) {
            mHandler.post(this::showQuickSettingsTooltipIfNeeded);
        }
    }

    @Override
    public void onStop(@NonNull LifecycleOwner owner) {
        // all the messages/callbacks will be removed.
        mHandler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        if (mSliderPreference != null) {
            mSliderPreference.dismissTooltip();
        }
    }

    void setInteractionListener(ProgressInteractionListener interactionListener) {
        mInteractionListener = Optional.ofNullable(interactionListener);
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);

        final int dataSize = mSizeData.getValues().size();
        final int initialIndex = mSizeData.getInitialIndex();
        mLastValue = initialIndex;
        mSliderPreference = screen.findPreference(getPreferenceKey());
        if (mSliderPreference == null) {
            return;
        }
        mSliderPreference.setMax(dataSize - 1);
        mSliderPreference.setSliderIncrement(1);
        mSliderPreference.setValue(initialIndex);
        mSliderPreference.setUpdatesContinuously(true);
        mSliderPreference.setTickVisible(true);
        mSliderPreference.setExtraChangeListener(mSliderChangeListener);
        mSliderPreference.setExtraTouchListener(mSliderTouchListener);
        setSliderStateDescription(mSliderPreference.getValue());
    }

    @Override
    public void resetState() {
        final int defaultValue = mSizeData.getValues().indexOf(mSizeData.getDefaultValue());
        if (mSliderPreference != null) {
            mSliderPreference.setValue(defaultValue);
        }

        // Immediately take the effect of updating the progress to avoid waiting for receiving
        // the event to delay update.
        mInteractionListener.ifPresent(ProgressInteractionListener::onProgressChanged);
    }

    /**
     * Stores the String array we would like to use for describing the state of seekbar progress
     * and updates the state description with current progress.
     *
     * @param labels The state descriptions to be announced for each progress.
     */
    public void setProgressStateLabels(String[] labels) {
        if (labels == null) {
            return;
        }
        mStateLabels = labels;
        updateState(mSliderPreference);
    }

    /**
     * Sets the state of seekbar based on current progress. The progress of seekbar is
     * corresponding to the index of the string array. If the progress is larger than or equals
     * to the length of the array, the state description is set to an empty string.
     */
    private void setSliderStateDescription(float value) {
        if (mStateLabels == null || mSliderPreference == null) {
            return;
        }
        int index = (int) value;
        mSliderPreference.setSliderStateDescription(
                (index < mStateLabels.length)
                        ? mStateLabels[index] : "");
    }

    private void onProgressFinalized() {
        // Using progress in SeekBarPreference since the progresses in
        // SeekBarPreference and seekbar are not always the same.
        // See {@link androidx.preference.Preference#callChangeListener(Object)}
        if (mSliderPreference != null) {
            int value = mSliderPreference.getValue();
            if (value != mLastValue) {
                showQuickSettingsTooltipIfNeeded();
                mLastValue = value;
            }
        }
    }

    private void showQuickSettingsTooltipIfNeeded() {
        if (mSliderPreference == null) {
            return;
        }

        final ComponentName tileComponentName = getTileComponentName();
        if (tileComponentName == null) {
            // Returns if no tile service assigned.
            return;
        }

        if (mContext instanceof Activity
                && WizardManagerHelper.isAnySetupWizard(((Activity) mContext).getIntent())) {
            // Don't show QuickSettingsTooltip in Setup Wizard
            return;
        }

        if (!mSliderPreference.getNeedsQSTooltipReshow()
                && AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                mContext, tileComponentName)) {
            // Returns if quick settings tooltip only show once.
            return;
        }

        // TODO (287728819): Move tooltip showing to SystemUI
        // Since the lifecycle of controller is independent of that of the preference, doing
        // null check on seekbar is a temporary solution for the case that seekbar view
        // is not ready when we would like to show the tooltip.  If the seekbar is not ready,
        // we give up showing the tooltip and also do not reshow it in the future.
        if (mSliderPreference.getSlider() != null) {
            final AccessibilityQuickSettingsTooltipWindow tooltipWindow =
                    mSliderPreference.createTooltipWindow();
            tooltipWindow.setup(getTileTooltipContent(),
                    R.drawable.accessibility_auto_added_qs_tooltip_illustration);
            tooltipWindow.showAtTopCenter(mSliderPreference.getSlider());
        }
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(mContext,
                tileComponentName);
        mSliderPreference.setNeedsQSTooltipReshow(false);
    }

    /** Returns the accessibility Quick Settings tile component name. */
    abstract ComponentName getTileComponentName();

    /** Returns accessibility Quick Settings tile tooltip content. */
    abstract CharSequence getTileTooltipContent();


    /**
     * Interface for callbacks when users interact with the seek bar.
     */
    interface ProgressInteractionListener {

        /**
         * Called when the progress is changed.
         */
        void notifyPreferenceChanged();

        /**
         * Called when the progress is changed without tracking touch.
         */
        void onProgressChanged();

        /**
         * Called when the seek bar is end tracking.
         */
        void onEndTrackingTouch();
    }
}
// LINT.ThenChange(/src/com/android/settings/accessibility/textreading/data/FontSizeDataStore.kt)
