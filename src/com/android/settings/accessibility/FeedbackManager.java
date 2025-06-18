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

import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.server.accessibility.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.DeviceInfoUtils;

import java.lang.ref.WeakReference;

/**
 * Manages the feedback flow. This class is responsible for checking feedback availability and
 * sending feedback. Uses a WeakReference to the Activity to prevent memory leaks.
 */
public class FeedbackManager {

    static final String CATEGORY_TAG = "category_tag";
    private static final int FEEDBACK_INTENT_RESULT_CODE = 0;

    private final WeakReference<Activity> mActivityWeakReference;
    @Nullable private final String mReporterPackage;
    @Nullable private final String mCategoryTag;

    /**
     * Constructs a new FeedbackManager.
     *
     * @param activity The activity context. A WeakReference is used to prevent memory leaks.
     * @param pageId The unique identifier of the page associated with the feedback.
     */
    public FeedbackManager(@Nullable Activity activity, int pageId) {
        this(activity,
                DeviceInfoUtils.getFeedbackReporterPackage(activity),
                FeatureFactory.getFeatureFactory()
                        .getAccessibilityFeedbackFeatureProvider()
                        .getCategory(pageId));
    }

    /**
     * Constructs a new FeedbackManager. This constructor is visible for testing.
     *
     * @param activity The activity context. A WeakReference is used to prevent memory leaks.
     * @param reporterPackage The package name of the feedback reporter.
     * @param category The feedback bucket ID.
     */
    @VisibleForTesting
    public FeedbackManager(@Nullable Activity activity, @Nullable String reporterPackage,
            @Nullable String category) {
        this.mActivityWeakReference = new WeakReference<>(activity);
        this.mReporterPackage = reporterPackage;
        this.mCategoryTag = category;
    }

    /**
     * Checks if feedback is available on the device.
     *
     * @return {@code true} if feedback is available, {@code false} otherwise.
     */
    public boolean isAvailable() {
        if (!Flags.enableLowVisionGenericFeedback()) {
            return false;
        }

        return !TextUtils.isEmpty(mReporterPackage)
                && !TextUtils.isEmpty(mCategoryTag)
                && mActivityWeakReference.get() != null;
    }

    /**
     * Sends feedback using the available feedback reporter. This will start the feedback
     * activity. It is the responsibility of the calling activity to handle the result
     * code {@link #FEEDBACK_INTENT_RESULT_CODE} if necessary.
     *
     * @return {@code true} if the feedback intent was successfully started, {@code false}
     * otherwise.
     */
    public boolean sendFeedback() {
        Activity activity = mActivityWeakReference.get();
        if (!isAvailable() || activity == null) {
            return false;
        }

        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(mReporterPackage);
        intent.putExtra(CATEGORY_TAG, mCategoryTag);
        activity.startActivityForResult(intent, FEEDBACK_INTENT_RESULT_CODE);
        return true;
    }
}
