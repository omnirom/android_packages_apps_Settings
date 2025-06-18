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

package com.android.settings.bluetooth.ui.view

import android.app.ActivityOptions
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.bluetooth.BlockingPrefWithSliceController
import com.android.settings.bluetooth.BluetoothDetailsProfilesController
import com.android.settings.bluetooth.ui.composable.MultiTogglePreference
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment.Companion.KEY_DEVICE_ADDRESS
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.DashboardFragment
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.core.AbstractPreferenceController
import com.android.settingslib.core.lifecycle.LifecycleObserver
import com.android.settingslib.core.lifecycle.events.OnPause
import com.android.settingslib.core.lifecycle.events.OnStop
import com.android.settingslib.widget.FooterPreference
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/** Handles device details fragment layout according to config. */
interface DeviceDetailsFragmentFormatter {
    /** Updates device details fragment layout. */
    fun updateLayout(fragmentType: FragmentTypeModel)

    /** Gets the menu items of the fragment. */
    fun getMenuItem(
        fragmentType: FragmentTypeModel
    ): Flow<DeviceSettingPreferenceModel.HelpPreference?>
}

@FlowPreview
@OptIn(ExperimentalCoroutinesApi::class)
class DeviceDetailsFragmentFormatterImpl(
    private val context: Context,
    private val dashboardFragment: DashboardFragment,
    controllers: List<AbstractPreferenceController>,
    private val bluetoothAdapter: BluetoothAdapter,
    private val cachedDevice: CachedBluetoothDevice,
    private val backgroundCoroutineContext: CoroutineContext,
) : DeviceDetailsFragmentFormatter {
    private val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
    private val prefVisibility = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val prefVisibilityJobs = mutableListOf<Job>()
    private var isLoading = false
    private var prefKeyToController: Map<String, AbstractPreferenceController> =
        controllers.associateBy { it.preferenceKey }

    private val viewModel: BluetoothDeviceDetailsViewModel =
        ViewModelProvider(
                dashboardFragment,
                BluetoothDeviceDetailsViewModel.Factory(
                    dashboardFragment.requireActivity().application,
                    bluetoothAdapter,
                    cachedDevice,
                    backgroundCoroutineContext,
                ),
            )
            .get(BluetoothDeviceDetailsViewModel::class.java)

    /** Updates bluetooth device details fragment layout. */
    override fun updateLayout(fragmentType: FragmentTypeModel) {
        dashboardFragment.setLoading(true, false)
        isLoading = true
        dashboardFragment.lifecycleScope.launch { updateLayoutInternal(fragmentType) }
    }

    private suspend fun updateLayoutInternal(fragmentType: FragmentTypeModel) {
        val items =
            viewModel.getItems(fragmentType)
                ?: run {
                    dashboardFragment.setLoading(false, false)
                    return
                }

        val prefKeyToSettingId =
            items
                .filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                .associateBy({ it.preferenceKey }, { it.settingId })

        val settingIdToXmlPreferences: MutableMap<Int, Preference> = HashMap()
        for (i in 0 until dashboardFragment.preferenceScreen.preferenceCount) {
            val pref = dashboardFragment.preferenceScreen.getPreference(i)
            prefKeyToSettingId[pref.key]?.let { id -> settingIdToXmlPreferences[id] = pref }
            if (pref.key !in prefKeyToSettingId) {
                getController(pref.key)?.let { disableController(it) }
            }
        }
        dashboardFragment.preferenceScreen.removeAll()
        for (job in prefVisibilityJobs) {
            job.cancel()
        }
        prefVisibilityJobs.clear()
        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            if (settingIdToXmlPreferences.containsKey(settingId)) {
                val pref = settingIdToXmlPreferences[settingId]!!.apply { order = row }
                dashboardFragment.preferenceScreen.addPreference(pref)
            } else {
                val prefKey = getPreferenceKey(settingId)

                prefVisibilityJobs.add(
                    viewModel
                        .getDeviceSetting(cachedDevice, settingId)
                        .onEach { logItemShown(prefKey, it != null) }
                        .launchIn(dashboardFragment.lifecycleScope)
                )
                if (settingId == DeviceSettingId.DEVICE_SETTING_ID_ANC) {
                    // TODO(b/399316980): replace it with SegmentedButtonPreference once it's ready.
                    val pref =
                        ComposePreference(context)
                            .apply {
                                key = prefKey
                                order = row
                            }
                            .also { pref ->
                                pref.setContent {
                                    buildComposePreference(cachedDevice, settingId, prefKey)
                                }
                            }
                    dashboardFragment.preferenceScreen.addPreference(pref)
                } else {
                    viewModel
                        .getDeviceSetting(cachedDevice, settingId)
                        .onEach {
                            val existedPref =
                                dashboardFragment.preferenceScreen.findPreference<Preference>(
                                    prefKey
                                )
                            val item =
                                it
                                    ?: run {
                                        existedPref?.let {
                                            dashboardFragment.preferenceScreen.removePreference(
                                                existedPref
                                            )
                                        }
                                        return@onEach
                                    }
                            buildPreference(existedPref, item, prefKey, settingItem.highlighted)
                                ?.apply {
                                    key = prefKey
                                    order = row
                                }
                                ?.also { dashboardFragment.preferenceScreen.addPreference(it) }
                        }
                        .launchIn(dashboardFragment.lifecycleScope)
                }
            }
        }

        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            settingIdToXmlPreferences[settingId]?.let { pref ->
                if (settingId == DeviceSettingId.DEVICE_SETTING_ID_BLUETOOTH_PROFILES) {
                    (getController(pref.key) as? BluetoothDetailsProfilesController)?.run {
                        if (
                            settingItem
                                is DeviceSettingConfigItemModel.BuiltinItem.BluetoothProfilesItem
                        ) {
                            setInvisibleProfiles(settingItem.invisibleProfiles)
                            setHasExtraSpace(false)
                        }
                    }
                }
                getController(pref.key)?.displayPreference(dashboardFragment.preferenceScreen)
                logItemShown(pref.key, pref.isVisible)
            }
        }

        dashboardFragment.lifecycleScope.launch {
            if (isLoading) {
                scrollToTop()
                dashboardFragment.setLoading(false, false)
                isLoading = false
            }
        }
    }

    override fun getMenuItem(
        fragmentType: FragmentTypeModel
    ): Flow<DeviceSettingPreferenceModel.HelpPreference?> = flow {
        val t = viewModel.getHelpItem(fragmentType)

        t?.let { item ->
            emitAll(
                viewModel.getDeviceSetting(cachedDevice, item.settingId).map {
                    it as? DeviceSettingPreferenceModel.HelpPreference
                }
            )
        } ?: emit(null)
    }

    private fun buildPreference(
        existedPref: Preference?,
        model: DeviceSettingPreferenceModel,
        prefKey: String,
        highlighted: Boolean,
    ): Preference? =
        when (model) {
            is DeviceSettingPreferenceModel.PlainPreference -> {
                val pref =
                    existedPref
                        ?: run {
                            if (highlighted) SpotlightPreference(context) else Preference(context)
                        }
                pref.apply {
                    title = model.title
                    summary = model.summary
                    icon = getDrawable(model.icon)
                    onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                            model.action?.let { triggerAction(it) }
                            true
                        }
                }
            }
            is DeviceSettingPreferenceModel.SwitchPreference ->
                if (model.action == null) {
                    val pref =
                        existedPref as? SwitchPreferenceCompat ?: SwitchPreferenceCompat(context)
                    pref.apply {
                        title = model.title
                        summary = model.summary
                        icon = getDrawable(model.icon)
                        isChecked = model.checked
                        isEnabled = !model.disabled
                        onPreferenceChangeListener =
                            object : Preference.OnPreferenceChangeListener {
                                override fun onPreferenceChange(
                                    p: Preference,
                                    value: Any?,
                                ): Boolean {
                                    (p as? TwoStatePreference)?.let { newState ->
                                        val newState = value as? Boolean ?: return false
                                        logItemClick(
                                            prefKey,
                                            if (newState) EVENT_SWITCH_ON else EVENT_SWITCH_OFF,
                                        )
                                        model.onCheckedChange.invoke(newState)
                                    }
                                    return false
                                }
                            }
                    }
                } else {
                    val pref =
                        existedPref as? PrimarySwitchPreference ?: PrimarySwitchPreference(context)
                    pref.apply {
                        title = model.title
                        summary = model.summary
                        icon = getDrawable(model.icon)
                        isChecked = model.checked
                        isEnabled = !model.disabled
                        isSwitchEnabled = !model.disabled
                        onPreferenceClickListener =
                            Preference.OnPreferenceClickListener {
                                logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                                triggerAction(model.action)
                                true
                            }
                        onPreferenceChangeListener =
                            object : Preference.OnPreferenceChangeListener {
                                override fun onPreferenceChange(
                                    p: Preference,
                                    value: Any?,
                                ): Boolean {
                                    val newState = value as? Boolean ?: return false
                                    logItemClick(
                                        prefKey,
                                        if (newState) EVENT_SWITCH_ON else EVENT_SWITCH_OFF,
                                    )
                                    model.onCheckedChange.invoke(newState)
                                    return false
                                }
                            }
                    }
                }

            is DeviceSettingPreferenceModel.MultiTogglePreference -> {
                // TODO(b/399316980): implemented it by SegmentedButtonPreference
                null
            }
            is DeviceSettingPreferenceModel.FooterPreference -> {
                val pref = existedPref as? FooterPreference ?: FooterPreference(context)
                pref.apply { title = model.footerText }
            }
            is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
                val pref = existedPref ?: Preference(context)
                pref.apply {
                    title =
                        context.getString(R.string.bluetooth_device_more_settings_preference_title)
                    summary =
                        context.getString(
                            R.string.bluetooth_device_more_settings_preference_summary
                        )
                    icon = context.getDrawable(R.drawable.ic_chevron_right_24dp)
                    onPreferenceClickListener =
                        object : Preference.OnPreferenceClickListener {
                            override fun onPreferenceClick(p: Preference): Boolean {
                                logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                                SubSettingLauncher(context)
                                    .setDestination(
                                        DeviceDetailsMoreSettingsFragment::class.java.name
                                    )
                                    .setSourceMetricsCategory(
                                        dashboardFragment.getMetricsCategory()
                                    )
                                    .setArguments(
                                        Bundle().apply {
                                            putString(KEY_DEVICE_ADDRESS, cachedDevice.address)
                                        }
                                    )
                                    .launch()
                                return true
                            }
                        }
                }
            }
            is DeviceSettingPreferenceModel.HelpPreference -> {
                null
            }
        }

    private fun getDrawable(deviceSettingIcon: DeviceSettingIcon?): Drawable? =
        when (deviceSettingIcon) {
            is DeviceSettingIcon.BitmapIcon ->
                deviceSettingIcon.bitmap.toDrawable(context.resources)
            is DeviceSettingIcon.ResourceIcon -> context.getDrawable(deviceSettingIcon.resId)
            null -> null
        }?.apply {
            setTint(
                context.getColor(
                    com.android.settingslib.widget.theme.R.color.settingslib_materialColorOnSurfaceVariant
                )
            )
        }

    @Composable
    private fun buildComposePreference(
        cachedDevice: CachedBluetoothDevice,
        settingId: Int,
        prefKey: String,
    ) {
        val contents by
            remember(settingId) { viewModel.getDeviceSetting(cachedDevice, settingId) }
                .collectAsStateWithLifecycle(initialValue = null)

        val settings = contents
        AnimatedVisibility(visible = settings != null, enter = fadeIn(), exit = fadeOut()) {
            (settings as? DeviceSettingPreferenceModel.MultiTogglePreference)?.let {
                buildMultiTogglePreference(it, prefKey)
            }
        }
    }

    @Composable
    private fun buildMultiTogglePreference(
        pref: DeviceSettingPreferenceModel.MultiTogglePreference,
        prefKey: String,
    ) {
        MultiTogglePreference(
            pref.copy(
                onSelectedChange = { newState ->
                    logItemClick(prefKey, newState)
                    pref.onSelectedChange(newState)
                }
            )
        )
    }

    private fun logItemClick(preferenceKey: String, value: Int = 0) {
        logAction(preferenceKey, SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_CLICKED, value)
    }

    private fun logItemShown(preferenceKey: String, visible: Boolean) {
        if (!visible && !prefVisibility.containsKey(preferenceKey)) {
            return
        }
        prefVisibility
            .computeIfAbsent(preferenceKey) {
                MutableStateFlow(true).also { visibilityFlow ->
                    visibilityFlow
                        .onEach {
                            logAction(
                                preferenceKey,
                                SettingsEnums.ACTION_BLUETOOTH_DEVICE_DETAILS_ITEM_SHOWN,
                                if (it) EVENT_VISIBLE else EVENT_INVISIBLE,
                            )
                        }
                        .launchIn(dashboardFragment.lifecycleScope)
                }
            }
            .value = visible
    }

    private fun logAction(preferenceKey: String, action: Int, value: Int) {
        metricsFeatureProvider.action(SettingsEnums.PAGE_UNKNOWN, action, 0, preferenceKey, value)
    }

    private fun triggerAction(action: DeviceSettingActionModel) {
        when (action) {
            is DeviceSettingActionModel.IntentAction -> {
                action.intent.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(action.intent)
            }
            is DeviceSettingActionModel.PendingIntentAction -> {
                val options =
                    ActivityOptions.makeBasic()
                        .setPendingIntentBackgroundActivityStartMode(
                            ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
                        )
                action.pendingIntent.send(options.toBundle())
            }
        }
    }

    private fun getController(key: String): AbstractPreferenceController? {
        return prefKeyToController[key]
    }

    private fun disableController(controller: AbstractPreferenceController) {
        if (controller is LifecycleObserver) {
            dashboardFragment.settingsLifecycle.removeObserver(controller as LifecycleObserver)
        }

        if (controller is BlockingPrefWithSliceController) {
            // Make UiBlockListener finished, otherwise UI will flicker.
            controller.onChanged(null)
        }

        if (controller is OnPause) {
            (controller as OnPause).onPause()
        }

        if (controller is OnStop) {
            (controller as OnStop).onStop()
        }
    }

    private fun scrollToTop() {
        // Temporary fix to make sure the screen is scroll to the top when rendering.
        ComposePreference(context).apply {
            order = -1
            isEnabled = false
            isSelectable = false
            setContent { Spacer(modifier = Modifier.height(1.dp)) }
        }.also {
            dashboardFragment.preferenceScreen.addPreference(it)
            dashboardFragment.scrollToPreference(it)
        }
    }

    private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_${settingId}"

    private class SpotlightPreference(context: Context) : Preference(context) {

        init {
            layoutResource = R.layout.bluetooth_device_spotlight_preference
        }

        override fun onBindViewHolder(holder: PreferenceViewHolder) {
            super.onBindViewHolder(holder)
            holder.isDividerAllowedBelow = false
            holder.isDividerAllowedAbove = false
        }
    }

    private companion object {
        const val TAG = "DeviceDetailsFormatter"
        const val EVENT_SWITCH_OFF = 0
        const val EVENT_SWITCH_ON = 1
        const val EVENT_CLICK_PRIMARY = 2
        const val EVENT_INVISIBLE = 0
        const val EVENT_VISIBLE = 1
    }
}
