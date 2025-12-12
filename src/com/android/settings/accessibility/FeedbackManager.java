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

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.server.accessibility.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.DeviceInfoUtils;

/**
 * Manages the feedback flow. This class is responsible for checking feedback availability and
 * sending feedback. Uses a WeakReference to the Activity to prevent memory leaks.
 */
public class FeedbackManager {

    static final String CATEGORY_TAG_EXTRA = "category_tag";
    static final String TRIGGER_ID_EXTRA = "com.android.feedback.TRIGGER_ID_EXTRA";
    @Nullable private final String mReporterPackage;
    @Nullable private final String mCategoryTag;
    @Nullable private final String mTriggerId;

    /**
     * Constructs a new FeedbackManager.
     *
     * @param context The context.
     * @param pageId The unique identifier of the page associated with the feedback.
     */
    public FeedbackManager(@NonNull Context context, int pageId) {
        this(DeviceInfoUtils.getFeedbackReporterPackage(context),
                FeatureFactory.getFeatureFactory()
                        .getAccessibilityFeedbackFeatureProvider()
                        .getCategory(pageId),
                FeatureFactory.getFeatureFactory()
                        .getAccessibilityFeedbackFeatureProvider()
                        .getTriggerId(pageId));
    }

    /**
     * Constructs a new FeedbackManager. This constructor is visible for testing.
     *
     * @param reporterPackage The package name of the feedback reporter.
     * @param category The feedback bucket ID.
     * @param triggerId The feedback trigger ID.
     */
    @VisibleForTesting
    public FeedbackManager(@Nullable String reporterPackage,
            @Nullable String category, @Nullable String triggerId) {
        this.mReporterPackage = reporterPackage;
        this.mCategoryTag = category;
        this.mTriggerId = triggerId;
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
                && !TextUtils.isEmpty(mCategoryTag);
    }

    /**
     * Returns an {@link Intent} designed to launch the configured feedback activity and initiate
     * the feedback process.
     *
     * @return An {@link Intent} to launch the feedback activity.
     */
    @NonNull
    public Intent getFeedbackIntent() {
        if (!isAvailable()) {
            return new Intent();
        }

        final Intent intent = new Intent(Intent.ACTION_BUG_REPORT);
        intent.setPackage(mReporterPackage);
        intent.putExtra(CATEGORY_TAG_EXTRA, mCategoryTag);
        if (!TextUtils.isEmpty(mTriggerId)) {
            intent.putExtra(TRIGGER_ID_EXTRA, mTriggerId);
        }
        return intent;
    }
}
