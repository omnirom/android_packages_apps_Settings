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

package com.android.settings.biometrics.metrics;

import android.annotation.Nullable;
import android.os.Build;

import androidx.annotation.NonNull;

import java.util.List;

/**
 * Logger class for biometrics
 */
public interface BiometricsLogger {
    String TAG = "BiometricsLogger";
    Boolean LOGGABLE = Build.isDebuggable();

    /** The bundle extra key for BiometricsOnboardingEvent used in Face/Fingerprint Enroll */
    String EXTRA_BIOMETRICS_ONBOARDING_EVENT = "biometrics_onboarding_event";

    /**
     * The bundle extra key for BiometricsOnboardingEvent in bytes array used in Face Enroll.
     */
    String EXTRA_BIOMETRICS_ONBOARDING_EVENT_BYTES = "biometrics_onboarding_event_bytes";

    /**
     * The bundle extra key for a list of BiometricsOnboardingEvent in byte array used in
     * Face /Fingerprint Enroll
     */
    String EXTRA_BIOMETRICS_ONBOARDING_EVENT_BYTES_LIST = "biometrics_onboarding_event_bytes_list";

    /** Log SettingsBiometricsOnboarding metrics */
    void logSettingsBiometricsOnboarding(@NonNull OnboardingEvent event);

    /** Convert BiometricOnboardingEvent to proto message byte array */
    byte[] eventToMessageByteArray(@NonNull OnboardingEvent event);

    /**
     * Convert proto message byte array to BiometricsOnboardingEvent.
     */
    @Nullable
    OnboardingEvent messageByteArrayToEvent(byte[] message);

    /**
     * Convert list of BiometricOnboardingEvent to repeated message byte array.
     */
    byte[] eventListToRepeatedMessageByteArray(@NonNull List<OnboardingEvent> events);
}
