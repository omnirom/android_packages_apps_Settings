/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.safetycenter

import android.app.ActivityManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.Flags
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemProperties
import android.provider.Settings
import android.proximity.IProximityResultCallback
import android.proximity.ProximityResultCode
import android.safetycenter.SafetyEvent
import android.safetycenter.SafetyEvent.SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED
import android.safetycenter.SafetySourceData
import android.safetycenter.SafetySourceIssue
import android.security.authenticationpolicy.AuthenticationPolicyManager
import android.util.Log
import com.android.internal.annotations.VisibleForTesting
import com.android.settings.R
import com.android.settings.biometrics.IdentityCheckNotificationPromoCardActivity
import com.android.settings.biometrics.IdentityCheckPromoCardActivity
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executors

/**
 * This class sets Safety Center Issue for [IdentityCheckSafetySource.SAFETY_SOURCE_ID]. It also
 * receives broadcast, [IdentityCheckSafetySource.ISSUE_CARD_DISMISSED_ACTION] when the issue card
 * is dismissed in Safety Center.
 */
class IdentityCheckSafetySource : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WatchContentObserver(context).registerContentObserver()

        if (intent.action?.equals(Intent.ACTION_BOOT_COMPLETED) == true) {
            val pendingResult = goAsync()
            val watchRangingFuture = context.getWatchRangingAvailabilityFuture()
            watchRangingFuture.addListener(
                {
                    try {
                        val watchRangingSupported = watchRangingFuture.get()
                        context.setWatchRangingSupported(watchRangingSupported)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting watch ranging support: ${e.message}", e)
                    } finally {
                        pendingResult?.finish()
                    }
                },
                Executors.newSingleThreadExecutor(),
            )
        }
    }

    companion object {
        const val SAFETY_SOURCE_ID = "AndroidIdentityCheck"
        const val ACTION_ISSUE_CARD_SHOW_DETAILS =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_SHOW_DETAILS"
        const val ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_ISSUE_CARD_WATCH_SHOW_DETAILS"
        const val ACTION_ISSUE_NOTIFICATION_CLICKED =
            "com.android.settings.safetycenter.action.IDENTITY_CHECK_NOTIFICATION_CLICKED"
        private const val TAG = "ICSafetySource"
        private const val REQUEST_ID = 0
        private const val ISSUE_CARD_VIEW_DETAILS = 1
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_ID = "IdentityCheckPromoCardIssue"
        private const val IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE = "IdentityCheckAllSurfaces"
        private const val IDENTITY_CHECK_PROMO_NOTIFICATION_ACTION_ID =
            "identity_check_promo_notification_action_id"

        /** Sets the safety source data with the Identity Check issue info. */
        fun setSafetySourceData(context: Context, safetyEvent: SafetyEvent) {
            setSafetySourceData(
                context,
                safetyEvent,
                context.getSystemService(BiometricManager::class.java),
            )
        }

        @VisibleForTesting
        fun setSafetySourceData(
            context: Context,
            safetyEvent: SafetyEvent,
            biometricManager: BiometricManager?,
            isTablet: Boolean = isTablet(),
            isLowRamDevice: Boolean = isLowRam(context),
        ) {
            if (!SafetyCenterManagerWrapper.get().isEnabled(context)) {
                return
            }
            if (isTablet) {
                sendNullData(context, safetyEvent)
                return
            }
            if (isLowRamDevice) {
                sendNullData(context, safetyEvent)
                return
            }
            if (biometricManager == null) {
                Log.e(TAG, "Biometric manager is null")
                return
            }
            if (!isIdentityCheckSupportedOnDevice(biometricManager)) {
                sendNullData(context, safetyEvent)
                return
            }
            if (!Flags.identityCheckAllSurfaces()) {
                sendNullData(context, safetyEvent)
                return
            }
            if (Flags.identityCheckWatch()) {
                if (!isWatchRangingSupportedValueUpdated(context)) {
                    sendNullData(context, safetyEvent)
                    return
                }
            }
            if (!hasPromoCardBeenShown(context)) {
                val safetySourceData =
                    SafetySourceData.Builder().addIssue(getIssue(context)).build()
                SafetyCenterManagerWrapper.get()
                    .setSafetySourceData(context, SAFETY_SOURCE_ID, safetySourceData, safetyEvent)
            } else {
                sendNullData(context, safetyEvent)
            }
        }

        private fun sendNullData(context: Context, safetyEvent: SafetyEvent) {
            SafetyCenterManagerWrapper.get()
                .setSafetySourceData(
                    context,
                    SAFETY_SOURCE_ID,
                    null, /* safetySourceData */
                    safetyEvent,
                )
        }

        private fun hasPromoCardBeenShown(context: Context): Boolean =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.IDENTITY_CHECK_PROMO_CARD_SHOWN,
                0, /* def */
            ) == 1

        private fun getIssue(context: Context): SafetySourceIssue {
            var issueCardDetails =
                IssueCardDetails(
                    context.getString(R.string.identity_check_issue_card_title),
                    context.getString(R.string.identity_check_issue_card_summary),
                    ACTION_ISSUE_CARD_SHOW_DETAILS,
                )
            var notificationDetails =
                NotificationDetails(
                    context.getString(R.string.identity_check_notification_title),
                    context.getString(R.string.identity_check_notification_summary),
                    context.getString(R.string.identity_check_view_details),
                )

            if (Flags.identityCheckWatch() && shouldShowWatchRangingPromoCard(context)) {
                val watchIssueCardTitle =
                    context.getString(R.string.identity_check_watch_issue_card_title)
                val watchIssueCardSummary =
                    context.getString(R.string.identity_check_watch_issue_card_summary)
                issueCardDetails.title = watchIssueCardTitle
                issueCardDetails.summary = watchIssueCardSummary
                issueCardDetails.intentAction = ACTION_ISSUE_CARD_WATCH_SHOW_DETAILS
            }

            return getIssue(context, issueCardDetails, notificationDetails)
        }

        /**
         * Checks if watch ranging is supported and if the requires strings for the promo card is
         * not empty.
         */
        private fun shouldShowWatchRangingPromoCard(context: Context): Boolean {
            return isWatchRangingSupportedValueUpdated(context) &&
                isWatchRangingSupported(context) &&
                context.resources.getBoolean(R.bool.config_show_identity_check_watch_promo)
        }

        private fun isWatchRangingSupported(context: Context): Boolean {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            ) == 1
        }

        private fun isWatchRangingSupportedValueUpdated(context: Context): Boolean {
            try {
                isWatchRangingSupported(context)
                return true
            } catch (_: Settings.SettingNotFoundException) {
                return false
            }
        }

        private fun isIdentityCheckSupportedOnDevice(biometricManager: BiometricManager): Boolean {
            return when (biometricManager.canAuthenticate(BIOMETRIC_STRONG)) {
                BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false
                else -> true
            }
        }

        private fun isLowRam(context: Context): Boolean {
            val activityManager = context.getSystemService(ActivityManager::class.java)
            return activityManager.isLowRamDevice
        }

        private fun isTablet(): Boolean {
            return "tablet" in SystemProperties.get("ro.build.characteristics", "").split(',')
        }

        private fun getIfIdentityCheckPromoNotificationHasBeenClicked(context: Context): Boolean =
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.IDENTITY_CHECK_NOTIFICATION_VIEW_DETAILS_CLICKED,
                0,
            ) == 1

        /** Returns the safety source issue for Identity Check. */
        private fun getIssue(
            context: Context,
            issueCardDetails: IssueCardDetails,
            notificationDetails: NotificationDetails,
        ): SafetySourceIssue {
            val issueCardButtonText =
                context.getString(R.string.identity_check_issue_card_button_text)
            val action =
                SafetySourceIssue.Action.Builder(
                        ACTION_ISSUE_CARD_SHOW_DETAILS,
                        issueCardButtonText,
                        PendingIntent.getActivity(
                            context,
                            ISSUE_CARD_VIEW_DETAILS,
                            Intent(issueCardDetails.intentAction)
                                .setClass(context, IdentityCheckPromoCardActivity::class.java),
                            PendingIntent.FLAG_IMMUTABLE,
                        ),
                    )
                    .build()
            val notificationIntent =
                Intent(ACTION_ISSUE_NOTIFICATION_CLICKED)
                    .setClass(context, IdentityCheckNotificationPromoCardActivity::class.java)

            val notificationPendingIntent =
                PendingIntent.getActivity(
                    context,
                    REQUEST_ID,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE,
                )
            val notification =
                SafetySourceIssue.Notification.Builder(
                        notificationDetails.title,
                        notificationDetails.summary,
                    )
                    .addAction(
                        SafetySourceIssue.Action.Builder(
                                IDENTITY_CHECK_PROMO_NOTIFICATION_ACTION_ID,
                                notificationDetails.buttonText,
                                notificationPendingIntent,
                            )
                            .build()
                    )
                    .build()
            val issue =
                SafetySourceIssue.Builder(
                        IDENTITY_CHECK_PROMO_CARD_ISSUE_ID,
                        issueCardDetails.title,
                        issueCardDetails.summary,
                        SafetySourceData.SEVERITY_LEVEL_INFORMATION,
                        IDENTITY_CHECK_PROMO_CARD_ISSUE_TYPE,
                    )
                    .setIssueCategory(SafetySourceIssue.ISSUE_CATEGORY_GENERAL)
                    .addAction(action)
                    .setIssueActionability(SafetySourceIssue.ISSUE_ACTIONABILITY_TIP)

            if (!getIfIdentityCheckPromoNotificationHasBeenClicked(context)) {
                issue
                    .setCustomNotification(notification)
                    .setNotificationBehavior(SafetySourceIssue.NOTIFICATION_BEHAVIOR_IMMEDIATELY)
            }
            return issue.build()
        }
    }

    private fun Context.getWatchRangingAvailabilityFuture(): ListenableFuture<Boolean> {
        val future = SettableFuture.create<Boolean>()
        val authenticationPolicyManager = getSystemService(AuthenticationPolicyManager::class.java)

        if (!hasPromoCardBeenShown(this) && Flags.identityCheckWatch()) {
            if (authenticationPolicyManager == null) {
                Log.e(TAG, "Authentication policy manager is null. Setting future to false.")
                future.set(false)
            } else {
                authenticationPolicyManager.isWatchRangingAvailable(
                    object : IProximityResultCallback.Stub() {
                        override fun onError(errorCode: Int) {
                            val supported =
                                errorCode !=
                                    ProximityResultCode.PRIMARY_DEVICE_RANGING_NOT_SUPPORTED
                            future.set(supported)
                        }

                        override fun onSuccess(p0: Int) {
                            future.set(true)
                        }
                    }
                )
            }
        } else {
            future.set(false)
        }
        return future
    }

    private fun Context.setWatchRangingSupported(boolean: Boolean) =
        Settings.Global.putInt(
            contentResolver,
            Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
            if (boolean) 1 else 0,
        )

    // Data class to encapsulate issue card details
    data class IssueCardDetails(var title: String, var summary: String, var intentAction: String)

    // Data class to encapsulate notification details
    data class NotificationDetails(val title: String, val summary: String, val buttonText: String)

    class WatchContentObserver(private val context: Context) :
        ContentObserver(Handler(Looper.getMainLooper())) {
        private val WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE =
            Settings.Global.getUriFor(Settings.Global.WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE)

        fun registerContentObserver() {
            context.contentResolver.registerContentObserver(
                WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE,
                false, /* notifyForDescendants */
                this,
            )
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (WATCH_RANGING_SUPPORTED_BY_PRIMARY_DEVICE.equals(uri)) {
                setSafetySourceData(
                    context,
                    SafetyEvent.Builder(SAFETY_EVENT_TYPE_SOURCE_STATE_CHANGED).build(),
                )
            }
        }
    }
}
