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

import android.content.Context;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilitySeekBarPreference;
import com.android.settings.accessibility.DisplaySizeData;
import com.android.settingslib.display.DisplayDensityUtils;

/**
 * The display size preference setting used for External displays.
 */
public class ExternalDisplaySizePreference extends AccessibilitySeekBarPreference {
    private int mDisplayWidth;
    private int mDisplayHeight;
    private int mDisplayId;
    private Context mContext;

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

    private void setStateForPreferenceInternal() {
        var displaySizeData = new DisplaySizeData(mContext,
                new DisplayDensityUtils(mContext, (info) -> info.displayId == mDisplayId));
        ExternalDisplaySizePreferenceStateHandler
                seekBarChangeHandler =
                new ExternalDisplaySizePreferenceStateHandler(
                        displaySizeData);

        setMax(displaySizeData.getValues().size() - 1);
        setProgress(displaySizeData.getInitialIndex());
        setContinuousUpdates(false);
        setOnSeekBarChangeListener(seekBarChangeHandler);
    }

    private class ExternalDisplaySizePreferenceStateHandler
            implements SeekBar.OnSeekBarChangeListener {
        private static final long MIN_COMMIT_INTERVAL_MS = 800;
        private static final long CHANGE_BY_BUTTON_DELAY_MS = 300;
        private static final long CHANGE_BY_SEEKBAR_DELAY_MS = 100;
        private final DisplaySizeData mDisplaySizeData;
        private int mLastDisplayProgress;
        private long mLastCommitTime;
        private boolean mSeekByTouch;
        ExternalDisplaySizePreferenceStateHandler(DisplaySizeData displaySizeData) {
            mDisplaySizeData = displaySizeData;
        }

        final Choreographer.FrameCallback mCommit = this::tryCommitDisplaySizeConfig;

        private void tryCommitDisplaySizeConfig(long unusedFrameTimeNanos) {
            final int displayProgress = getProgress();
            if (displayProgress != mLastDisplayProgress) {
                mDisplaySizeData.commit(displayProgress);
                mLastDisplayProgress = displayProgress;
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
        public void onProgressChanged(@NonNull SeekBar seekBar, int i, boolean b) {
            if (!mSeekByTouch) postCommitDelayed(CHANGE_BY_BUTTON_DELAY_MS);
        }

        @Override
        public void onStartTrackingTouch(@NonNull SeekBar seekBar) {
            mSeekByTouch = true;
        }

        @Override
        public void onStopTrackingTouch(@NonNull SeekBar seekBar) {
            mSeekByTouch = false;
            postCommitDelayed(CHANGE_BY_SEEKBAR_DELAY_MS);
        }
    }
}
