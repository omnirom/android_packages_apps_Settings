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

package com.android.settings.biometrics.face

import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_FINISHED
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_SKIP
import com.android.settings.biometrics.BiometricEnrollBase.RESULT_TIMEOUT
import com.android.settings.biometrics.BiometricsOnboardingProto
import com.android.settings.biometrics.combination.CombinedBiometricStatusUtils
import com.android.settings.biometrics.metrics.BiometricsLogger
import com.android.settings.biometrics.metrics.OnboardingEvent
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory

class FaceEnroll: AppCompatActivity() {

    /**
     * The class of the next activity to launch. This is open to allow subclasses to provide their
     * own behavior. Defaults to the default activity class provided by the
     * enrollActivityClassProvider.
     */
    private val nextActivityClass: Class<*>
        get() = enrollActivityProvider.next

    private val enrollActivityProvider: FaceEnrollActivityClassProvider
        get() = featureFactory.faceFeatureProvider.enrollActivityClassProvider

    @VisibleForTesting
    var launchedFromProvider: () -> String? = { launchedFromPackage }

    private var isLaunched = false
    private var startTimeMillis: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            isLaunched = savedInstanceState.getBoolean(KEY_IS_LAUNCHED, isLaunched)
            startTimeMillis = savedInstanceState.getLong(KEY_START_TIME, 0)
        }

        if (startTimeMillis <= 0) {
            startTimeMillis = SystemClock.elapsedRealtime()
        }

        if (!isLaunched) {
            /**
             *  Logs the next activity to be launched, creates an intent for that activity,
             *  adds flags to forward the result, includes any existing extras from the current intent,
             *  starts the new activity and then finishes the current one
             */
            Log.d("FaceEnroll", "forward to $nextActivityClass")
            val nextIntent = Intent(this, nextActivityClass)
            nextIntent.putExtras(intent)

            // drop extras that are not allowed from external packages before launching
            if (launchedFromProvider() != packageName) {
                nextIntent.removeExtra(Intent.EXTRA_USER_ID)
            }
            startActivityForResult(nextIntent, 0)

            isLaunched = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(KEY_IS_LAUNCHED, isLaunched)
        outState.putLong(KEY_START_TIME, startTimeMillis)
        super.onSaveInstanceState(outState)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)
        isLaunched = false
        if (intent.getBooleanExtra(
                CombinedBiometricStatusUtils.EXTRA_LAUNCH_FROM_SAFETY_SOURCE_ISSUE, false)
            && resultCode != RESULT_FINISHED) {
            featureFactory.biometricsFeatureProvider.notifySafetyIssueActionLaunched()
        }
        updateOnboardingEvent(resultCode, data)
        setResult(resultCode, data)
        finish()
    }

    fun updateOnboardingEvent(resultCode: Int, data: Intent?) {
        val event = getOnboardingEventFromIntent(data)
        event?.let { ev ->
            ev.resultCode =
                if (resultCode == RESULT_OK || resultCode == RESULT_FINISHED)
                    BiometricsOnboardingProto.OnboardingResult.RESULT_COMPLETED_VALUE
                else if (resultCode == RESULT_CANCELED)
                    BiometricsOnboardingProto.OnboardingResult.RESULT_CANCEL_VALUE
                else if (resultCode == RESULT_SKIP)
                    BiometricsOnboardingProto.OnboardingResult.RESULT_SKIP_VALUE
                else if (resultCode == RESULT_TIMEOUT)
                    BiometricsOnboardingProto.OnboardingResult.RESULT_TIMEOUT_VALUE
                else
                    BiometricsOnboardingProto.OnboardingResult.RESULT_UNKNOWN_VALUE
            ev.duration = SystemClock.elapsedRealtime() - startTimeMillis
            data?.putExtra(BiometricsLogger.EXTRA_BIOMETRICS_ONBOARDING_EVENT, ev)
            featureFactory.biometricsFeatureProvider
                .biometricsLogger?.logSettingsBiometricsOnboarding(ev)
        }
        if(BiometricsLogger.LOGGABLE) {
            Log.d(
                BiometricsLogger.TAG, "${javaClass.getSimpleName()}: " + " received event=" + event
            )
        }
    }

    private fun getOnboardingEventFromIntent(data: Intent?): OnboardingEvent? {
        val logger = featureFactory.biometricsFeatureProvider.biometricsLogger
        if (logger != null && data != null
            && data.hasExtra(BiometricsLogger.EXTRA_BIOMETRICS_ONBOARDING_EVENT_BYTES)) {
            return logger.messageByteArrayToEvent(
                data.getByteArrayExtra(BiometricsLogger.EXTRA_BIOMETRICS_ONBOARDING_EVENT_BYTES))
        }
        return null
    }

    private companion object {
        const val KEY_IS_LAUNCHED = "isLaunched"
        const val KEY_START_TIME = "startTime"
    }
}
