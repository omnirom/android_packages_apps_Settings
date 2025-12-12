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

package com.android.settings.connecteddevice.display;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Choreographer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.accessibility.DisplaySizeData;
import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settingslib.display.DisplayDensityUtils;
import com.android.settingslib.widget.SliderPreference;

import com.google.android.material.slider.Slider;

/**
 * The display size preference setting used for External displays.
 */
public class ExternalDisplaySizePreference extends SliderPreference {
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mDisplayId;
    private final Context mContext;

    public ExternalDisplaySizePreference(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mDisplayId = -1;
        mDisplayWidth = 0;
        mDisplayHeight = 0;

        setIconStart(R.drawable.ic_remove_24dp);
        setIconStartContentDescription(R.string.screen_zoom_make_smaller_desc);
        setIconEnd(R.drawable.ic_add_24dp);
        setIconEndContentDescription(R.string.screen_zoom_make_larger_desc);
    }

    /** Sets the display width and height for this preference. */
    public void setStateForPreference(int displayWidth, int displayHeight, int displayId) {
        if (mDisplayWidth == displayWidth && mDisplayHeight == displayHeight
                && displayId == mDisplayId) return;
        mDisplayWidth = displayWidth;
        mDisplayHeight = displayHeight;
        mDisplayId = displayId;

        setStateForPreferenceInternal();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    private void setStateForPreferenceInternal() {
        var displaySizeData = new DisplaySizeData(mContext,
                new DisplayDensityUtils(mContext, (info) -> info.displayId == mDisplayId));
        setMax(displaySizeData.getValues().size() - 1);
        setSliderIncrement(1);
        setMin(0);
        setUpdatesContinuously(false);
        setTickVisible(true);
        setValue(displaySizeData.getInitialIndex());

        ExternalDisplaySizePreferenceStateHandler
                seekBarChangeHandler =
                new ExternalDisplaySizePreferenceStateHandler(
                        displaySizeData);

        setExtraChangeListener(seekBarChangeHandler);
        setExtraTouchListener(seekBarChangeHandler);
    }

    private class ExternalDisplaySizePreferenceStateHandler
            implements Slider.OnSliderTouchListener, Slider.OnChangeListener {
        private static final long MIN_COMMIT_INTERVAL_MS = 800;
        private static final long CHANGE_BY_BUTTON_DELAY_MS = 300;
        private static final long CHANGE_BY_SEEKBAR_DELAY_MS = 100;
        private final DisplaySizeData mDisplaySizeData;
        private int mLastDisplayProgress = getValue();
        private long mLastCommitTime;
        private boolean mSeekByTouch;
        private final ExternalDisplaySettingsLoggerStore.ExternalDisplayMetricsLogger mLogger =
                ExternalDisplaySettingsLoggerStore.getLogger(mDisplayId);


        ExternalDisplaySizePreferenceStateHandler(DisplaySizeData displaySizeData) {
            mDisplaySizeData = displaySizeData;
            mLogger.updateDisplaySize(displaySizeData.getDensityPercentage(mLastDisplayProgress));
        }

        final Choreographer.FrameCallback mCommit = this::tryCommitDisplaySizeConfig;

        private void tryCommitDisplaySizeConfig(long unusedFrameTimeNanos) {
            final int displayProgress = getValue();
            if (displayProgress != mLastDisplayProgress) {
                mDisplaySizeData.commit(displayProgress);
                mLastDisplayProgress = displayProgress;

                mLogger.updateDisplaySize(
                        mDisplaySizeData.getDensityPercentage(mLastDisplayProgress));
                mLogger.log(
                        SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED__SETTING__DISPLAY_SIZE);
            }

            mLastCommitTime = SystemClock.elapsedRealtime();
        }

        private void postCommitDelayed(long commitDelayMs) {
            if (SystemClock.elapsedRealtime() - mLastCommitTime < MIN_COMMIT_INTERVAL_MS) {
                commitDelayMs += MIN_COMMIT_INTERVAL_MS;
            }

            final Choreographer choreographer = Choreographer.getInstance();
            choreographer.removeFrameCallback(mCommit);
            choreographer.postFrameCallbackDelayed(mCommit, commitDelayMs);
        }

        @Override
        public void onValueChange(@NonNull Slider slider, float value, boolean fromUser) {
            if (!mSeekByTouch) postCommitDelayed(CHANGE_BY_BUTTON_DELAY_MS);
        }

        @Override
        public void onStartTrackingTouch(@NonNull Slider slider) {
            mSeekByTouch = true;
        }

        @Override
        public void onStopTrackingTouch(@NonNull Slider slider) {
            mSeekByTouch = false;
            postCommitDelayed(CHANGE_BY_SEEKBAR_DELAY_MS);
        }
    }
}
