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

package com.android.settings.safetycenter;

import static com.android.settings.biometrics.combination.BiometricsSettingsBase.ACTIVE_UNLOCK_REQUEST;

import android.app.PendingIntent;
import android.content.Context;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.biometrics.activeunlock.ActiveUnlockStatusUtils;
import com.android.settings.flags.Flags;

/** Wear Safety Source for Safety Center. */
public final class WearSafetySource {

    private static final String TAG = "WearSafetySource";
    public static final String SAFETY_SOURCE_ID = "AndroidWearUnlock";
    private static boolean sIsTestingEnv = false;
    private static String sSummaryForTesting = "";
    private static boolean sHasEnrolledForTesting;

    private WearSafetySource() {}

    /** Sets test value for summary. */
    @VisibleForTesting
    public static void setSummaryForTesting(@NonNull String summary) {
        sIsTestingEnv = true;
        sSummaryForTesting = summary;
    }

    /** Sets test value for hasEnrolled. */
    @VisibleForTesting
    public static void setHasEnrolledForTesting(boolean hasEnrolled) {
        sIsTestingEnv = true;
        sHasEnrolledForTesting = hasEnrolled;
    }

    /** Sets biometric safety data for Safety Center. */
    public static void setSafetySourceData(
            @NonNull Context context, @NonNull SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }
        if (!Flags.biometricsOnboardingEducation()) { // this source is effectively turned off
            sendNullData(context, safetyEvent);
            return;
        }

        // Handle private profile case.
        UserManager userManager = UserManager.get(context);
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()
                && userManager.isPrivateProfile()) {
            // SC always expects a response from the source if the broadcast has been sent for this
            // source, therefore, we need to send a null SafetySourceData.
            sendNullData(context, safetyEvent);
            return;
        }

        ActiveUnlockStatusUtils activeUnlockStatusUtils = new ActiveUnlockStatusUtils(context);
        if (!userManager.isProfile() && activeUnlockStatusUtils.isAvailable()) {
            boolean hasEnrolled = false;
            String summary = "";

            if (sIsTestingEnv) {
                hasEnrolled = sHasEnrolledForTesting;
                summary = sSummaryForTesting;
            } else {
                String authority = new ActiveUnlockStatusUtils(context).getAuthority();
                hasEnrolled = getHasEnrolledFromContentProvider(context, authority);
                summary = getSummaryFromContentProvider(context, authority);
            }

            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    activeUnlockStatusUtils.getTitleForActiveUnlockOnly(),
                    summary,
                    PendingIntent.getActivity(context, ACTIVE_UNLOCK_REQUEST,
                            activeUnlockStatusUtils.getIntent(),
                            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT),
                    /* enabled= */ true,
                    hasEnrolled,
                    safetyEvent);
            return;
        }

        sendNullData(context, safetyEvent);
    }

    private static void sendNullData(Context context, SafetyEvent safetyEvent) {
        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                        context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
    }

    /** Notifies Safety Center of a change in wear biometrics settings. */
    public static void onBiometricsChanged(@NonNull Context context) {
        setSafetySourceData(
                context,
                new SafetyEvent.Builder(SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED)
                        .build());
    }

    private static boolean getHasEnrolledFromContentProvider(
            @NonNull Context context, @Nullable String authority) {
        if (authority == null) {
            return false;
        }
        return ActiveUnlockStatusUtils.getDeviceNameFromContentProvider(context, authority, TAG)
            != null;
    }

    private static String getSummaryFromContentProvider(
            @NonNull Context context, @Nullable String authority) {
        if (authority == null) {
            return "";
        }
        String summary = ActiveUnlockStatusUtils.getSummaryFromContentProvider(
                context, authority, TAG);
        if (summary == null) {
            return "";
        }
        return summary;
    }

}
