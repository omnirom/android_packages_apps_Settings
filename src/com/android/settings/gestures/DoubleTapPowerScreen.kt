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

package com.android.settings.gestures

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE
import android.provider.Settings.Secure.DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED
import android.service.quickaccesswallet.Flags.launchWalletOptionOnPowerDoubleTap
import androidx.fragment.app.Fragment
import com.android.internal.R as IR
import com.android.settings.R
import com.android.settings.Settings.DoubleTapPowerSettingsActivity
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(DoubleTapPowerScreen.KEY)
open class DoubleTapPowerScreen(context: Context) :
    PreferenceScreenMixin,
    PreferenceAvailabilityProvider,
    PreferenceTitleProvider,
    PreferenceSummaryProvider,
    PreferenceLifecycleProvider {
    private val doubleTapPowerToOpenCameraDataStore =
        DoubleTapPowerToOpenCameraPreference.createDataStore(context)
    private lateinit var keyedObserver: KeyedObserver<String>

    private val doubleTapKeys =
        listOf(
            DoubleTapPowerToOpenCameraPreference.KEY,
            DOUBLE_TAP_POWER_BUTTON_GESTURE_ENABLED,
            DOUBLE_TAP_POWER_BUTTON_GESTURE,
        )

    override val key: String
        get() = KEY

    override val indexable
        get() = false

    override val highlightMenuKey: Int
        get() = R.string.menu_key_system

    override fun getMetricsCategory() = SettingsEnums.SETTINGS_GESTURE_DOUBLE_TAP_POWER

    override fun isFlagEnabled(context: Context) = Flags.deeplinkSystem25q4()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = DoubleTapPowerSettings::class.java

    override fun isAvailable(context: Context) = context.isGestureAvailable()

    override fun getTitle(context: Context): CharSequence? =
        when {
            context.isNonLaunchWalletOrNonMultiTargetDoubleTap() ->
                context.getText(R.string.double_tap_power_for_camera_title)
            else -> context.getText(R.string.double_tap_power_title)
        }

    override fun getSummary(context: Context): CharSequence? =
        when {
            context.isNonLaunchWalletOrNonMultiTargetDoubleTap() -> {
                val isCameraDoubleTapPowerGestureEnabled =
                    doubleTapPowerToOpenCameraDataStore.getBoolean(
                        DoubleTapPowerToOpenCameraPreference.KEY
                    )
                context.getText(
                    if (isCameraDoubleTapPowerGestureEnabled == true) {
                        R.string.gesture_setting_on
                    } else {
                        R.string.gesture_setting_off
                    }
                )
            }
            DoubleTapPowerSettingsUtils.isDoubleTapPowerButtonGestureEnabled(context) -> {
                val onString: CharSequence = context.getText(R.string.gesture_setting_on)
                val actionString: CharSequence =
                    if (
                        DoubleTapPowerSettingsUtils
                            .isDoubleTapPowerButtonGestureForCameraLaunchEnabled(context)
                    ) {
                        context.getText(R.string.double_tap_power_camera_action_summary)
                    } else {
                        context.getText(R.string.double_tap_power_wallet_action_summary)
                    }
                context.getString(R.string.double_tap_power_summary, onString, actionString)
            }
            else -> context.getText(R.string.gesture_setting_off)
        }

    override fun onCreate(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            keyedObserver = KeyedObserver { _, _ -> context.notifyPreferenceChange(KEY) }
            for (doubleTapKey in doubleTapKeys) {
                doubleTapPowerToOpenCameraDataStore.addObserver(
                    doubleTapKey,
                    keyedObserver,
                    HandlerExecutor.main,
                )
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        if (isEntryPoint(context)) {
            for (doubleTapKey in doubleTapKeys) {
                doubleTapPowerToOpenCameraDataStore.removeObserver(doubleTapKey, keyedObserver)
            }
        }
    }

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, DoubleTapPowerSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    private fun Context.isNonLaunchWalletOrNonMultiTargetDoubleTap(): Boolean =
        !launchWalletOptionOnPowerDoubleTap() ||
            !DoubleTapPowerSettingsUtils.isMultiTargetDoubleTapPowerButtonGestureAvailable(this)

    private fun Context.isGestureAvailable(): Boolean =
        if (!launchWalletOptionOnPowerDoubleTap()) {
            resources.getBoolean(IR.bool.config_cameraDoubleTapPowerGestureEnabled)
        } else {
            resources.getInteger(IR.integer.config_doubleTapPowerGestureMode) !=
                DoubleTapPowerSettingsUtils.DOUBLE_TAP_POWER_DISABLED_MODE
        }

    companion object {
        const val KEY = "gesture_double_tap_power_input_summary"
    }
}
// LINT.ThenChange(
//     DoubleTapPowerPreferenceController.java,
//     DoubleTapPowerSettings.java,
// )
