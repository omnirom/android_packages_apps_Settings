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

package com.android.settings.datetime;

import static android.content.Intent.URI_INTENT_SCHEME;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.preference.Preference;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settings.flags.Flags;

import java.net.URISyntaxException;

/**
 * A controller for the Settings button that launches "time feedback". The intent launches is
 * configured with an Intent URI.
 */
public class TimeFeedbackPreferenceController
        extends BasePreferenceController
        implements PreferenceControllerMixin {

    private static final String TAG = "TimeFeedbackController";

    private final PackageManager mPackageManager;
    private final String mIntentUri;

    public TimeFeedbackPreferenceController(Context context, String preferenceKey) {
        this(context, context.getPackageManager(), preferenceKey, context.getResources().getString(
                R.string.config_time_feedback_intent_uri));
    }

    @VisibleForTesting
    TimeFeedbackPreferenceController(Context context, PackageManager packageManager,
            String preferenceKey, String intentUri) {
        super(context, preferenceKey);
        mPackageManager = packageManager;
        mIntentUri = intentUri;
    }

    /**
     * Registers this controller with a category controller so that the category can be optionally
     * displayed, i.e. if all the child controllers are not available, the category heading won't be
     * available.
     */
    public void registerWithOptionalCategoryController(
            TimeFeedbackPreferenceCategoryController categoryController) {
        categoryController.addChildController(this);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!Flags.datetimeFeedback() || TextUtils.isEmpty(mIntentUri)) {
            return UNSUPPORTED_ON_DEVICE;
        } else if (!isTimeFeedbackTargetAvailable()) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        return AVAILABLE;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return super.handlePreferenceTreeClick(preference);
        }

        // Don't allow a monkey user to launch feedback
        if (ActivityManager.isUserAMonkey()) {
            return true;
        }

        try {
            Intent intent = Intent.parseUri(mIntentUri, URI_INTENT_SCHEME);
            mContext.startActivity(intent);
            return true;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Bad intent configuration: " + mIntentUri, e);
            return false;
        }
    }

    private boolean isTimeFeedbackTargetAvailable() {
        Intent intent;
        try {
            intent = Intent.parseUri(mIntentUri, URI_INTENT_SCHEME);
        } catch (URISyntaxException e) {
            Log.e(TAG, "Bad intent configuration: " + mIntentUri, e);
            return false;
        }
        ComponentName resolvedActivity = intent.resolveActivity(mPackageManager);

        if (resolvedActivity == null) {
            Log.w(TAG, "No valid target for the time feedback intent: " + intent);
            return false;
        }
        return true;
    }
}
