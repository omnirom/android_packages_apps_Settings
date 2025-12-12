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

package com.android.settings.supervision

import android.content.Intent
import android.util.Log
import com.android.settings.CatalystSettingsActivity
import com.android.settingslib.supervision.SupervisionLog.TAG

/**
 * Activity to display the Supervision settings landing page (Settings > Supervision).
 *
 * See [SupervisionDashboardScreen] for details on the page contents.
 */
class SupervisionDashboardActivity :
    CatalystSettingsActivity(
        SupervisionDashboardScreen.KEY,
        SupervisionDashboardFragment::class.java,
    ) {

    override fun onResume() {
        super.onResume()

        if (shouldRedirectToSupervisionApp()) {
            val redirectIntent = getSupervisionAppIntent()
            // We don't expect the intent to be null, but if it happens, we just skip the redirect.
            if (redirectIntent != null) {
                startActivity(redirectIntent)
                finish()
                return
            }
            Log.w(TAG, "Should redirect to supervision app but no intent available.")
        }

        // If the supervision package doesn't have the necessary components, the dashboard can't be
        // directly loaded.
        if (!hasNecessarySupervisionComponent(matchAll = true)) {
            // Check if the app provides any mitigating actions and trigger them if so.
            val installActivityInfo = getSupervisionAppInstallActivityInfo()
            if (installActivityInfo != null) {
                if (installActivityInfo.isEnabled()) {
                    // There is a mitigating action available, launch it.
                    val installIntent =
                        Intent(SupervisionHelper.INSTALL_SUPERVISION_APP_ACTION)
                            .setPackage(installActivityInfo.packageName)
                    startActivity(installIntent)
                } else {
                    // There is a mitigating action available, but the component is disabled.
                    // Launch the loading screen to try to enable it.
                    val loadingActivity =
                        Intent(this, SupervisionDashboardLoadingActivity::class.java)
                    startActivity(loadingActivity)
                }
            }
            finish()
            return
        }

        // If the supervision package has the necessary component but not in the enabled state,
        // launch a loading screen while trying to enable it.
        if (!hasNecessarySupervisionComponent()) {
            val loadingActivity = Intent(this, SupervisionDashboardLoadingActivity::class.java)
            startActivity(loadingActivity)
            finish()
            return
        }
    }

    private fun shouldRedirectToSupervisionApp(): Boolean {
        return supervisionRoleHolders.isNotEmpty() || isSupervisionPackageProfileOwner()
    }

    private fun getSupervisionAppIntent(): Intent? {
        val packageName = readDefaultSupervisionPackageNameFromResources() ?: return null

        val intent = Intent(INTERSTITIAL_REDIRECT_ACTION).setPackage(packageName)

        if (packageManager.queryIntentActivitiesAsUser(intent, 0, userId).isNotEmpty()) {
            return intent
        } else {
            Log.w(TAG, "Should redirect to supervision app but $intent did not resolve.")
            return null
        }
    }

    companion object {
        const val INTERSTITIAL_REDIRECT_ACTION =
            "android.app.supervision.action.INTERSTITIAL_SCREEN"
    }
}
