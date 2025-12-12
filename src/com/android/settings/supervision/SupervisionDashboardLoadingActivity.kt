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

import android.app.role.OnRoleHoldersChangedListener
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.android.settings.R
import com.android.settings.core.CategoryMixin
import com.android.settings.overlay.FeatureFactory.Companion.featureFactory
import com.android.settings.supervision.ipc.SupervisionMessengerClient
import com.android.settingslib.collapsingtoolbar.R.drawable.settingslib_expressive_icon_back as EXPRESSIVE_BACK_ICON
import com.android.settingslib.drawer.CategoryKey.CATEGORY_SUPERVISION
import com.android.settingslib.supervision.SupervisionLog.TAG
import com.android.settingslib.widget.SettingsThemeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SupervisionDashboardLoadingActivity : FragmentActivity(), CategoryMixin.CategoryListener {
    private var supervisionMessengerClient: SupervisionMessengerClient? = null
    private var launchJob: Job? = null
    private lateinit var categoryMixin: CategoryMixin
    private lateinit var supervisionPackage: String

    private val packageChangeReceiver =
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                // Only react to supervision package updates.
                if (intent.data?.schemeSpecificPart == supervisionPackage) {
                    categoryMixin.updateCategories()

                    maybeLaunchDashboard()
                }
            }
        }

    private val roleObserver = OnRoleHoldersChangedListener { roleName, _ ->
        if (roleName == RoleManager.ROLE_SYSTEM_SUPERVISION) {
            maybeLaunchDashboard()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get the supervision package name from the system config
        supervisionPackage =
            resources.getString(com.android.internal.R.string.config_systemSupervision)
        if (supervisionPackage.isEmpty()) {
            finish()
            return
        }

        setContentView(R.layout.supervision_dashboard_loading_screen)

        categoryMixin = CategoryMixin(this)
    }

    override fun onResume() {
        super.onResume()

        registerReceivers()
        tryEnableSupervisionPackage()

        setupActionBar()
        maybeLaunchDashboard()
    }

    override fun onStop() {
        super.onStop()

        supervisionMessengerClient?.close()
        unregisterReceivers()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupActionBar() {
        actionBar?.apply {
            elevation = 0f
            setDisplayHomeAsUpEnabled(true)
            if (SettingsThemeHelper.isExpressiveTheme(this@SupervisionDashboardLoadingActivity)) {
                setHomeAsUpIndicator(EXPRESSIVE_BACK_ICON)
            }
        }
    }

    private fun registerReceivers() {
        val intentFilter =
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addDataScheme("package")
            }
        registerReceiver(packageChangeReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
        categoryMixin.addCategoryListener(this)

        val roleManager = getSystemService(RoleManager::class.java)
        roleManager?.addOnRoleHoldersChangedListenerAsUser(mainExecutor, roleObserver, user)
    }

    private fun unregisterReceivers() {
        unregisterReceiver(packageChangeReceiver)
        categoryMixin.removeCategoryListener(this)

        val roleManager = getSystemService(RoleManager::class.java)
        roleManager?.removeOnRoleHoldersChangedListenerAsUser(roleObserver, user)
    }

    override fun onCategoriesChanged(categories: Set<String>?) {
        // A null set means refreshed all categories.
        if (categories == null || categories.contains(CATEGORY_SUPERVISION)) {
            maybeLaunchDashboard()
        }
    }

    private fun maybeLaunchDashboard() {
        if (isFinishing) return

        if (hasNecessarySupervisionComponent()) {
            // Supervision package with necessary component is ready, check if dashboard is ready
            // to be launched.

            // If a launch job is already active, do not start another one.
            if (launchJob?.isActive == true) {
                return
            }
            launchJob =
                lifecycleScope.launch(Dispatchers.IO) {
                    if (isSettingsDashboardReady()) {
                        withContext(Dispatchers.Main) {
                            val dashboardActivity =
                                Intent(
                                    this@SupervisionDashboardLoadingActivity,
                                    SupervisionDashboardActivity::class.java,
                                )
                            startActivity(dashboardActivity)
                            finish()
                        }
                    }
                }
        } else if (getSupervisionAppInstallActivityInfo() != null) {
            // Supervision package with necessary component is not ready, but a mitigation action
            // is available.
            startActivity(getSupervisionAppInstallIntent())
            finish()
        }
        // Otherwise, wait for another event to trigger this check again.
    }

    private suspend fun isSettingsDashboardReady(): Boolean {
        val tilesExist =
            featureFactory.dashboardFeatureProvider.getTilesForCategory(CATEGORY_SUPERVISION) !=
                null

        if (!hasSupervisionRole() || !tilesExist) {
            return false
        }

        if (supervisionMessengerClient == null) {
            supervisionMessengerClient = SupervisionMessengerClient(this)
        }
        return isPreferenceDataLoaded(supervisionMessengerClient!!)
    }

    private fun hasSupervisionRole(): Boolean {
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager == null) {
            Log.w(TAG, "null RoleManager")
            return false
        }

        val result =
            roleManager
                .getRoleHolders(RoleManager.ROLE_SYSTEM_SUPERVISION)
                .contains(supervisionPackage)
        return result
    }

    private suspend fun isPreferenceDataLoaded(
        client: SupervisionMessengerClient,
        retries: Int = 3,
        initialDelay: Long = 1000, // 1 second
    ): Boolean {
        val promoKey = SupervisionPromoFooterPreference.KEY
        val aocKey = SupervisionAocFooterPreference.KEY
        if (
            client.getCachedPreferenceData(listOf(promoKey))[promoKey] != null ||
                client.getCachedPreferenceData(listOf(aocKey))[aocKey] != null
        ) {
            return true
        }

        var currentDelay = initialDelay
        for (attempt in 0 until retries) {
            try {
                // Attempt to fetch the data. This is the part that throws the exception.
                val result =
                    withContext(Dispatchers.IO) {
                        val promoData = async { client.getPreferenceData(listOf(promoKey)) }
                        val aocData = async { client.getPreferenceData(listOf(aocKey)) }
                        promoData.await()[promoKey] != null && aocData.await()[aocKey] != null
                    }
                if (result) {
                    // Only return if data was fetched successfully. Otherwise, retry.
                    return true
                }
            } catch (e: Exception) {
                Log.w(TAG, "Attempt ${attempt + 1} to fetch preference data failed.", e)
            } finally {
                if (attempt == retries - 1) {
                    // If this was the last attempt, give up without delaying.
                    break
                }

                // Wait for the delay period before the next attempt.
                delay(currentDelay)
                // Double the delay for the next retry (exponential backoff).
                currentDelay *= 2
            }
        }
        // If we reach here, we have exhausted all retries and should just return true
        // instead of blocking the loading of the dashboard on the data load.
        return true
    }

    private fun tryEnableSupervisionPackage() {
        try {
            val packageInfo = packageManager.getPackageInfo(supervisionPackage, 0)
            if (packageInfo == null) return

            val applicationInfo = packageInfo.applicationInfo
            if (applicationInfo == null) return

            if (!applicationInfo.enabled) {
                packageManager.setApplicationEnabledSetting(
                    supervisionPackage,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    0,
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // Supervision package is not installed, do nothing
            return
        }
    }
}
