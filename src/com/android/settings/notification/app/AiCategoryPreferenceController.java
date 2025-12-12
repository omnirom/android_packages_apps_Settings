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

package com.android.settings.notification.app;

import static android.service.notification.Adjustment.KEY_SUMMARIZATION;
import static android.service.notification.Adjustment.KEY_TYPE;

import android.content.Context;

import androidx.annotation.NonNull;

import com.android.settings.notification.NotificationBackend;

/**
 * Controller for the ai notification features section
 */
public class AiCategoryPreferenceController extends NotificationPreferenceController {

    private static final String KEY = "ai_features";

    public AiCategoryPreferenceController(@NonNull Context context,
            @NonNull NotificationBackend backend) {
        super(context, backend);
    }

    @Override
    public @NonNull String getPreferenceKey() {
        return KEY;
    }

    @Override
    boolean isIncludedInFilter() {
        // not a channel-specific preference; only at the app level
        return false;
    }

    @Override
    public boolean isAvailable() {
        return (AdjustmentKeyPreferenceController.isAvailable(KEY_TYPE, mBackend, mAppRow.pkg,
                        mAppRow.uid)
                || AdjustmentKeyPreferenceController.isAvailable(KEY_SUMMARIZATION, mBackend,
                        mAppRow.pkg, mAppRow.uid))
                && super.isAvailable();
    }
}
