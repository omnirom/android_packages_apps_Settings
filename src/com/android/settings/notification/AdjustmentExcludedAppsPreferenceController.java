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

package com.android.settings.notification;

import static android.service.notification.Adjustment.KEY_SUMMARIZATION;
import static android.service.notification.Adjustment.KEY_TYPE;

import android.app.Flags;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.notification.Adjustment;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.text.BidiFormatter;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import com.android.settings.core.BasePreferenceController;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.applications.AppUtils;
import com.android.settingslib.applications.ApplicationsState;
import com.android.settingslib.utils.ThreadUtils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Adds a preference to the PreferenceCategory for every app excluded from an adjustment key
 */
public class AdjustmentExcludedAppsPreferenceController extends BasePreferenceController
        implements PreferenceControllerMixin {

    @NonNull private NotificationBackend mBackend;

    @Nullable String mAdjustmentKey;
    @Nullable @VisibleForTesting ApplicationsState mApplicationsState;
    @VisibleForTesting PreferenceCategory mPreferenceCategory;
    @VisibleForTesting Context mPrefContext;

    private ApplicationsState.Session mAppSession;
    private UserManager mUserManager;

    public AdjustmentExcludedAppsPreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        mBackend = new NotificationBackend();
        mUserManager = context.getSystemService(UserManager.class);
    }

    @VisibleForTesting
    void setUserManager(UserManager um) {
        mUserManager = um;
    }

    protected void onAttach(@Nullable ApplicationsState appState, @Nullable Fragment host,
            @NonNull NotificationBackend helperBackend, @Adjustment.Keys String adjustment) {
        mApplicationsState = appState;
        mBackend = helperBackend;
        mAdjustmentKey = adjustment;

        if (mApplicationsState != null && host != null) {
            mAppSession = mApplicationsState.newSession(mAppSessionCallbacks, host.getLifecycle());
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        mPreferenceCategory = screen.findPreference(getPreferenceKey());
        mPrefContext = screen.getContext();
        updateAppList();
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        if (!(Flags.nmSummarization() || Flags.nmSummarizationUi()
                || Flags.notificationClassificationUi())) {
            return CONDITIONALLY_UNAVAILABLE;
        }
        if (KEY_SUMMARIZATION.equals(mAdjustmentKey)
                && mBackend.isNotificationSummarizationSupported()) {
            return AVAILABLE;
        }
        if (KEY_TYPE.equals(mAdjustmentKey) && mBackend.isNotificationBundlingSupported()) {
            return AVAILABLE;
        }
        return CONDITIONALLY_UNAVAILABLE;
    }

    /**
     * Call this method to trigger the app list to refresh.
     */
    public void updateAppList() {
        if (mAppSession == null) {
            return;
        }

        mAppSession.rebuild(ApplicationsState.FILTER_ENABLED_NOT_QUIET,
                ApplicationsState.ALPHA_COMPARATOR);
    }

    // Set the icon for the given preference to the entry icon from cache if available, or look
    // it up.
    private void updateIcon(Preference pref, ApplicationsState.AppEntry entry) {
        synchronized (entry) {
            final Drawable cachedIcon = AppUtils.getIconFromCache(entry);
            if (cachedIcon != null && entry.mounted) {
                pref.setIcon(cachedIcon);
            } else {
                ListenableFuture unused = ThreadUtils.postOnBackgroundThread(() -> {
                    final Drawable icon = AppUtils.getIcon(mPrefContext, entry);
                    if (icon != null) {
                        ThreadUtils.postOnMainThread(() -> pref.setIcon(icon));
                    }
                });
            }
        }
    }

    @VisibleForTesting
    void updateAppList(List<ApplicationsState.AppEntry> apps) {
        if (mPreferenceCategory == null || mAdjustmentKey == null || apps == null) {
            return;
        }

        Map<Integer, List<String>> excludedAppsByUser = new ArrayMap<>();
        for (UserHandle userHandle : mUserManager.getUserProfiles()) {
            int userId = userHandle.getIdentifier();
            excludedAppsByUser.put(userId,
                    mBackend.getAdjustmentDeniedPackages(userId, mAdjustmentKey));
        }

        for (ApplicationsState.AppEntry app : apps) {
            String pkg = app.info.packageName;
            int userId = UserHandle.getUserId(app.info.uid);
            final String key = getKey(pkg, app.info.uid);
            boolean doesAppPassCriteria = false;

            if (excludedAppsByUser.getOrDefault(userId, Collections.EMPTY_LIST).contains(pkg)) {
                doesAppPassCriteria = true;
            }
            Preference pref = mPreferenceCategory.findPreference(key);
            if (pref == null) {
                if (doesAppPassCriteria) {
                    // does not exist but should
                    pref = new Preference(mPrefContext);
                    pref.setKey(key);
                    pref.setTitle(BidiFormatter.getInstance().unicodeWrap(app.label));
                    updateIcon(pref, app);
                    pref.setSelectable(false);
                    mPreferenceCategory.addPreference(pref);
                }
            } else if (!doesAppPassCriteria) {
                // exists but shouldn't anymore
                mPreferenceCategory.removePreference(pref);
            }
        }
    }

    /**
     * Create a unique key to identify an AppPreference
     */
    static String getKey(String pkg, int uid) {
        return "all|" + pkg + "|" + uid;
    }

    private final ApplicationsState.Callbacks mAppSessionCallbacks =
            new ApplicationsState.Callbacks() {

                @Override
                public void onRunningStateChanged(boolean running) {
                }

                @Override
                public void onPackageListChanged() {
                }

                @Override
                public void onRebuildComplete(@NonNull ArrayList<ApplicationsState.AppEntry> apps) {
                    updateAppList(apps);
                }

                @Override
                public void onPackageIconChanged() {
                }

                @Override
                public void onPackageSizeChanged(@NonNull String packageName) {
                }

                @Override
                public void onAllSizesComputed() { }

                @Override
                public void onLauncherInfoChanged() {
                }

                @Override
                public void onLoadEntriesCompleted() {
                    updateAppList();
                }
            };
}
