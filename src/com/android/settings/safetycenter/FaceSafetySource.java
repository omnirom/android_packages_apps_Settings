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

import static com.android.settings.biometrics.BiometricEnrollActivity.EXTRA_LAUNCH_FACE_ENROLL_FIRST;
import static com.android.settings.safetycenter.BiometricSourcesUtils.REQUEST_CODE_FACE_SETTING;

import android.content.Context;
import android.hardware.face.FaceManager;
import android.os.Bundle;
import android.os.Process;
import android.os.UserHandle;
import android.os.UserManager;
import android.safetycenter.SafetyEvent;

import com.android.settings.Utils;
import com.android.settings.biometrics.BiometricEnrollActivity;
import com.android.settings.biometrics.BiometricNavigationUtils;
import com.android.settings.biometrics.face.FaceStatusUtils;
import com.android.settings.flags.Flags;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.RestrictedLockUtils;

/** Face biometrics Safety Source for Safety Center. */
public final class FaceSafetySource {

    public static final String SAFETY_SOURCE_ID = "AndroidFaceUnlock";

    private FaceSafetySource() {}

    /** Sets biometric safety data for Safety Center. */
    public static void setSafetySourceData(Context context, SafetyEvent safetyEvent) {
        if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
            return;
        }
        if (!Flags.biometricsOnboardingEducation()) { // this source is effectively turned off
            sendNullData(context, safetyEvent);
            return;
        }

        // Handle private profile case
        UserManager userManager = UserManager.get(context);
        if (android.os.Flags.allowPrivateProfile()
                && android.multiuser.Flags.enablePrivateSpaceFeatures()
                && userManager.isPrivateProfile()) {
            // SC always expects a response from the source if the broadcast has been sent for this
            // source, therefore, we need to send a null SafetySourceData.
            sendNullData(context, safetyEvent);
            return;
        }

        UserHandle userHandle = Process.myUserHandle();
        int userId = userHandle.getIdentifier();
        FaceManager faceManager = Utils.getFaceManagerOrNull(context);
        FaceStatusUtils faceStatusUtils = new FaceStatusUtils(context, faceManager, userId);
        BiometricNavigationUtils biometricNavigationUtils = new BiometricNavigationUtils(userId);
        UserHandle profileParentUserHandle = userManager.getProfileParent(userHandle);
        if (profileParentUserHandle == null) {
            profileParentUserHandle = userHandle;
        }
        Context profileParentContext = context.createContextAsUser(profileParentUserHandle, 0);

        if (Utils.hasFaceHardware(context)) {
            boolean isMultipleBiometricsEnrollmentNeeded =
                    BiometricSourcesUtils.isMultipleBiometricsEnrollmentNeeded(context, userId);
            String settingClassName = isMultipleBiometricsEnrollmentNeeded
                    ? BiometricEnrollActivity.InternalActivity.class.getName()
                    : faceStatusUtils.getSettingsClassName();
            Bundle bundle = new Bundle();
            if (isMultipleBiometricsEnrollmentNeeded) {
                // Launch face enrollment first then fingerprint enrollment.
                bundle.putBoolean(EXTRA_LAUNCH_FACE_ENROLL_FIRST, true);
            }
            RestrictedLockUtils.EnforcedAdmin disablingAdmin = faceStatusUtils.getDisablingAdmin();
            BiometricSourcesUtils.setBiometricSafetySourceData(
                    SAFETY_SOURCE_ID,
                    context,
                    faceStatusUtils.getTitle(),
                    faceStatusUtils.getSummary(),
                    BiometricSourcesUtils.createPendingIntent(
                            profileParentContext,
                            biometricNavigationUtils
                                    .getBiometricSettingsIntent(
                                            context,
                                            settingClassName,
                                            disablingAdmin,
                                            bundle)
                                    .setIdentifier(Integer.toString(userId)),
                            REQUEST_CODE_FACE_SETTING),
                    disablingAdmin == null /* enabled */,
                    faceStatusUtils.hasEnrolled(),
                    safetyEvent,
                    FeatureFactory.getFeatureFactory().getBiometricsFeatureProvider()
                            .getSafetySourceIssue(SAFETY_SOURCE_ID));

            return;
        }

        sendNullData(context, safetyEvent);
    }

    private static void sendNullData(Context context, SafetyEvent safetyEvent) {
        SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                        context, SAFETY_SOURCE_ID, /* safetySourceData= */ null, safetyEvent);
    }

    /** Notifies Safety Center of a change in face biometrics settings. */
    public static void onBiometricsChanged(Context context) {
        setSafetySourceData(
                context,
                new SafetyEvent.Builder(SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED)
                        .build());
    }
}
