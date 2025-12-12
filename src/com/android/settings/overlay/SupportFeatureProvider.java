/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.android.settings.overlay;

import android.app.Activity;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import com.android.settingslib.search.SearchIndexableRaw;

/**
 * Feature provider for support tab.
 */
public interface SupportFeatureProvider {

    /**
     * Starts support, invokes the support home page.
     *
     * @param activity Calling activity.
     */
    void startSupport(Activity activity);

    /**
     * Applies overrides to the support preference, if needed.
     *
     * @param context Preference controller context.
     * @param pref The support preference.
     */
    default void applyOverrides(@NonNull Context context, @NonNull Preference pref) {
    }

    /**
     * Applies overrides to the support search indexable, if needed.
     *
     * @param context support dashboard activity context.
     * @param data The support search indexable.
     */
    default void applyOverrides(@NonNull Context context, @NonNull SearchIndexableRaw data) {
    }
}
