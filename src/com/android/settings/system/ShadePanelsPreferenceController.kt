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

package com.android.settings.system

import android.content.ContentResolver
import android.content.Context
import android.hardware.display.DisplayManager
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import com.android.settings.R
import com.android.settings.core.BasePreferenceController
import com.android.systemui.Flags
import kotlin.math.min

/**
 * The preference controller for the Settings page controlling Notifications & Quick Settings
 * panels, allowing the user to switch between "Dual Shade" and "Single Shade".
 */
class ShadePanelsPreferenceController(
    context: Context,
    key: String
) : BasePreferenceController(context, key) {

    override fun getAvailabilityStatus(): Int = getDualShadeAvailability(mContext)

    override fun getSummary(): CharSequence? {
        if (getAvailabilityStatus() != AVAILABLE) {
            return null
        }
        return mContext.getText(
            if (mContext.contentResolver.isDualShadeEnabled()) {
                R.string.shade_panels_separate_title
            } else {
                R.string.shade_panels_combined_title
            }
        )
    }

    companion object {
        internal const val TAG = "ShadePanelsPreferenceController"

        internal const val MIN_LARGE_SCREEN_WIDTH_DP = 600

        private const val ON = 1
        private const val OFF = 0

        /** Retrieve the preference value from secure settings. */
        fun ContentResolver.isDualShadeEnabled(): Boolean {
            return Settings.Secure.getInt(this, Settings.Secure.DUAL_SHADE, ON) == ON
        }

        /** Persist the preference value to secure settings. */
        fun ContentResolver.setDualShadeEnabled(enable: Boolean): Boolean {
            return Settings.Secure.putInt(
                this,
                Settings.Secure.DUAL_SHADE,
                if (enable) ON else OFF
            )
        }

        /** Whether the Dual Shade feature is available on this device. */
        @JvmStatic
        fun isDualShadeAvailable(context: Context) : Boolean {
            return getDualShadeAvailability(context) == AVAILABLE
        }

        @AvailabilityStatus
        internal fun getDualShadeAvailability(context: Context): Int {
            if (!Flags.sceneContainer()) {
                Log.i(TAG, "Scene container is disabled")
                return CONDITIONALLY_UNAVAILABLE
            }

            val deviceHasCompactScreen =
                hasAnyDisplayWithSmallestWidthLessThan(context, MIN_LARGE_SCREEN_WIDTH_DP)

            Log.i(TAG, "The device has ${if (deviceHasCompactScreen) "a" else "no"} compact screen")
            return if (deviceHasCompactScreen) AVAILABLE else UNSUPPORTED_ON_DEVICE
        }

        /**
         * Checks if any connected or internal display associated with the device has a smallest
         * width less than [thresholdDp].
         *
         * This function queries all logical displays reported by the [DisplayManager].
         *
         * @return `true` if at least one display meets the condition, `false` otherwise.
         * Returns `false` if [DisplayManager] cannot be accessed or no displays are found.
         */
        internal fun hasAnyDisplayWithSmallestWidthLessThan(
            context: Context,
            thresholdDp: Int
        ): Boolean {
            // Minor optimization that captures the vast majority of cases.
            if (context.resources.configuration.smallestScreenWidthDp < MIN_LARGE_SCREEN_WIDTH_DP) {
                Log.d(TAG, "The current display has smallestScreenWidthDp under the threshold")
                return true
            }

            val displayManager =
                context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            if (displayManager == null) {
                Log.w(TAG, "DisplayManager service not available.")
                return false
            }

            val displayMetrics = DisplayMetrics()

            // Iterate through all currently available logical displays, including built-in screens
            // (like inner/outer on foldables) and connected external displays (HDMI, wireless
            // display etc.)
            for (display in displayManager.displays) {
                val id = display.displayId
                try {
                    display.getRealMetrics(displayMetrics)

                    val density = displayMetrics.density
                    if (density <= 0f) {
                        Log.w(TAG, "Skipping display $id due to invalid density: $density")
                        continue
                    }

                    val smallestWidthDp = with (displayMetrics) {
                        min(widthPixels, heightPixels) / density
                    }

                    Log.d(
                        TAG,
                        "Display $id: (${displayMetrics.widthPixels}px, " +
                            "${displayMetrics.heightPixels}px), " +
                            "Density: $density, " +
                            "Smallest: ${"%.1f".format(smallestWidthDp)}dp"
                    )

                    if (smallestWidthDp < thresholdDp) {
                        Log.i(TAG, "Found a display with smallest width under the threshold: $id")
                        return true
                    }
                } catch (e: Exception) {
                    // Handle potential exceptions during metric retrieval.
                    Log.e(TAG, "Error getting metrics for display $id", e)
                    continue
                }
            }
            return false
        }
    }
}
