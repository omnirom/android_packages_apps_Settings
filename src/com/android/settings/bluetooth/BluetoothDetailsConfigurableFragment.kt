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

package com.android.settings.bluetooth

import android.app.ActivityOptions
import android.app.settings.SettingsEnums
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.UserManager
import android.util.Log
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceGroup
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreferenceCompat
import androidx.preference.TwoStatePreference
import com.android.settings.R
import com.android.settings.bluetooth.ui.composable.MultiTogglePreference
import com.android.settings.bluetooth.ui.model.DeviceSettingPreferenceModel
import com.android.settings.bluetooth.ui.model.FragmentTypeModel
import com.android.settings.bluetooth.ui.view.DeviceDetailsMoreSettingsFragment
import com.android.settings.bluetooth.ui.viewmodel.BluetoothDeviceDetailsViewModel
import com.android.settings.connecteddevice.ConnectedDeviceDashboardFragment
import com.android.settings.core.SubSettingLauncher
import com.android.settings.dashboard.RestrictedDashboardFragment
import com.android.settings.flags.Flags
import com.android.settings.overlay.FeatureFactory
import com.android.settings.spa.preference.ComposePreference
import com.android.settingslib.PrimarySwitchPreference
import com.android.settingslib.bluetooth.CachedBluetoothDevice
import com.android.settingslib.bluetooth.LocalBluetoothManager
import com.android.settingslib.bluetooth.devicesettings.DeviceSettingId
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingActionModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingConfigItemModel
import com.android.settingslib.bluetooth.devicesettings.shared.model.DeviceSettingIcon
import com.android.settingslib.spa.widget.ui.LinearLoadingBar
import com.android.settingslib.widget.BannerMessagePreference
import com.android.settingslib.widget.CardPreference
import com.android.settingslib.widget.FooterPreference
import com.android.settingslib.widget.SegmentedButtonPreference
import com.android.settingslib.widget.UntitledPreferenceCategory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.withIndex
import kotlinx.coroutines.launch

/** Base class for bluetooth settings which makes the preference visibility/order configurable. */
abstract class BluetoothDetailsConfigurableFragment :
    RestrictedDashboardFragment(UserManager.DISALLOW_CONFIG_BLUETOOTH) {
    protected lateinit var localBluetoothManager: LocalBluetoothManager
    protected lateinit var deviceAddress: String
    protected lateinit var cachedDevice: CachedBluetoothDevice
    private var displayOrder: List<String>? = null
    private lateinit var originalDisplayOrder: List<String>
    private val metricsFeatureProvider = FeatureFactory.featureFactory.metricsFeatureProvider
    private val prefVisibility = mutableMapOf<String, MutableStateFlow<Boolean>>()
    private val uiJobs = mutableListOf<Job>()

    private lateinit var viewModel: BluetoothDeviceDetailsViewModel

    private val invisiblePrefCategory: PreferenceGroup by lazy {
        preferenceScreen.findPreference<PreferenceGroup>(INVISIBLE_CATEGORY)
            ?: run {
                PreferenceCategory(requireContext())
                    .apply {
                        key = INVISIBLE_CATEGORY
                        isVisible = false
                        isOrderingAsAdded = true
                    }
                    .also {
                        preferenceScreen.addPreference(it)
                        it.addPreference(
                            ComposePreference(requireContext()).apply {
                                key = LOADING_PREF
                                setContent { LinearLoadingBar(isLoading = true) }
                            }
                        )
                    }
            }
    }

    override fun onAttach(context: Context) {
        localBluetoothManager = Utils.getLocalBtManager(context)
        deviceAddress =
            arguments?.getString(KEY_DEVICE_ADDRESS)
                ?: run {
                    Log.w(TAG, "onAttach() address is null!")
                    super.onAttach(context)
                    // Go to connected devices screen if address is null.
                    launchConnectedDevicesScreen()
                    finish()
                    return
                }
        cachedDevice =
            getCachedDevice(deviceAddress)
                ?: run {
                    Log.w(TAG, "onAttach() CachedDevice is null!")
                    super.onAttach(context)
                    // Go to connected devices screen if the device is not found.
                    launchConnectedDevicesScreen()
                    finish()
                    return
                }
        super.onAttach(context)
        viewModel =
            ViewModelProvider(
                    this,
                    BluetoothDeviceDetailsViewModel.Factory(
                        requireActivity().application,
                        cachedDevice,
                        Dispatchers.IO,
                    ),
                )
                .get(BluetoothDeviceDetailsViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        originalDisplayOrder = preferenceScreen.getAllChildren().map { it.key }
        updatePreferenceOrder()
    }

    fun requestUpdateLayout(prefKeyOrder: List<String>?) {
        if (displayOrder == prefKeyOrder) {
            return
        }
        displayOrder = prefKeyOrder
        updatePreferenceOrder()
    }

    fun requestUpdateLayout(fragmentType: FragmentTypeModel) {
        lifecycleScope.launch { updateLayoutInternal(fragmentType) }
    }

    private suspend fun updateLayoutInternal(fragmentType: FragmentTypeModel) {
        val items =
            viewModel.getItems(fragmentType)
                ?: run {
                    displayOrder = originalDisplayOrder
                    updatePreferenceOrder()
                    return
                }

        val prefKeyToSettingId =
            items
                .filterIsInstance<DeviceSettingConfigItemModel.BuiltinItem>()
                .associateBy({ it.preferenceKey }, { it.settingId })

        val settingIdToPreferences: MutableMap<Int, Preference> = HashMap()
        for (pref in getAllPreferences()) {
            prefKeyToSettingId[pref.key]?.let { id -> settingIdToPreferences[id] = pref }
        }
        for (job in uiJobs) {
            job.cancel()
        }
        uiJobs.clear()
        val configDisplayOrder = mutableListOf<String>()
        var currentContainer: PreferenceGroup = preferenceScreen
        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId

            val existingPrefKey = settingIdToPreferences[settingId]?.key
            if (settingId == DeviceSettingId.DEVICE_SETTING_ID_ANC) {
                configDisplayOrder.add(getPreferenceCategoryKey(settingId))
                currentContainer = preferenceScreen
            } else if (existingPrefKey != null) {
                configDisplayOrder.add(existingPrefKey)
                currentContainer = preferenceScreen
            } else if (currentContainer === preferenceScreen) {
                // The padding is problematic when we mix standalone preference and preference
                // category in preference screen, so we wrap the preferences with a
                // UntitledPreferenceCategory here.
                val categoryKey = getPreferenceCategoryKey(settingId)
                configDisplayOrder.add(categoryKey)
                currentContainer =
                    UntitledPreferenceCategory(requireContext()).apply { key = categoryKey }
                preferenceScreen.addPreference(currentContainer)
            }
            if (existingPrefKey != null) {
                continue
            }

            val container = currentContainer
            val prefKey = getPreferenceKey(settingId)
            val deviceSetting =
                viewModel.getDeviceSetting(cachedDevice, settingId).dropWhile { it == null }
            deviceSetting
                .onEach { logItemShown(prefKey, it != null) }
                .launchIn(lifecycleScope)
                .also { uiJobs.add(it) }
            if (
                settingId == DeviceSettingId.DEVICE_SETTING_ID_ANC &&
                    !Flags.enableBluetoothSettingsExpressiveDesign()
            ) {
                val pref =
                    ComposePreference(requireContext())
                        .apply {
                            key = prefKey
                            order = row
                        }
                        .also { pref ->
                            pref.setContent {
                                buildComposePreference(cachedDevice, settingId, prefKey)
                            }
                        }
                container.addPreference(pref)
            } else {
                deviceSetting
                    .withIndex()
                    .debounce {
                        // Debounce here otherwise ANC toggle may flicker after user clicks.
                        if (
                            it.index > 0 &&
                                it.value is DeviceSettingPreferenceModel.MultiTogglePreference
                        ) {
                            200
                        } else {
                            0
                        }
                    }
                    .map { it.value }
                    .onEach {
                        val existedPref = container.findPreference<Preference>(prefKey)
                        val item =
                            it
                                ?: run {
                                    existedPref?.let { container.removePreference(existedPref) }
                                    return@onEach
                                }
                        addPreference(
                            container,
                            existedPref,
                            row,
                            item,
                            prefKey,
                            settingItem.highlighted,
                        )
                    }
                    .launchIn(lifecycleScope)
                    .also { uiJobs.add(it) }
            }
        }

        for (row in items.indices) {
            val settingItem = items[row]
            val settingId = settingItem.settingId
            settingIdToPreferences[settingId]?.let { pref ->
                if (settingId == DeviceSettingId.DEVICE_SETTING_ID_BLUETOOTH_PROFILES) {
                    use(BluetoothDetailsProfilesController::class.java)?.run {
                        if (
                            settingItem
                                is DeviceSettingConfigItemModel.BuiltinItem.BluetoothProfilesItem
                        ) {
                            setInvisibleProfiles(settingItem.invisibleProfiles)
                        }
                    }
                }
                logItemShown(pref.key, pref.isVisible)
            }
        }
        displayOrder = configDisplayOrder
        updatePreferenceOrder()
    }

    private fun addPreference(
        container: PreferenceGroup,
        existedPref: Preference?,
        prefOrder: Int,
        model: DeviceSettingPreferenceModel,
        prefKey: String,
        highlighted: Boolean,
    ) =
        when (model) {
            is DeviceSettingPreferenceModel.PlainPreference -> {
                val pref =
                    existedPref
                        ?: run {
                            if (highlighted) {
                                if (Flags.enableBluetoothSettingsExpressiveDesign()) {
                                    CardPreference(requireContext())
                                } else {
                                    SpotlightPreference(requireContext())
                                }
                            } else {
                                Preference(requireContext())
                            }
                        }
                pref.apply {
                    key = prefKey
                    order = prefOrder
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
                container.addPreference(pref)
            }

            is DeviceSettingPreferenceModel.SwitchPreference ->
                if (model.action == null) {
                    val pref =
                        existedPref as? SwitchPreferenceCompat
                            ?: SwitchPreferenceCompat(requireContext())
                    pref.apply {
                        key = prefKey
                        order = prefOrder
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
                    container.addPreference(pref)
                } else {
                    val pref =
                        existedPref as? PrimarySwitchPreference
                            ?: PrimarySwitchPreference(requireContext())
                    pref.apply {
                        key = prefKey
                        order = prefOrder
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
                    container.addPreference(pref)
                }

            is DeviceSettingPreferenceModel.MultiTogglePreference -> {
                val prefCategory =
                    existedPref as? PreferenceCategory ?: PreferenceCategory(requireContext())
                prefCategory.apply {
                    title = model.title
                    key = prefKey
                    order = prefOrder
                }
                container.addPreference(prefCategory)
                val pref =
                    if (prefCategory.preferenceCount == 0) {
                        SegmentedButtonPreference(requireContext()).also {
                            prefCategory.addPreference(it)
                        }
                    } else {
                        prefCategory.getPreference(0) as SegmentedButtonPreference
                    }
                pref.apply {
                    for (idx in 0..3) {
                        if (idx >= model.toggles.size) {
                            pref.setButtonVisibility(idx, false)
                            continue
                        }
                        pref.setButtonVisibility(idx, true)
                        pref.setButtonEnabled(idx, model.isAllowedChangingState)
                        getDrawable(model.toggles[idx].icon)?.let {
                            pref.setUpButton(idx, model.toggles[idx].label, it)
                        }
                        pref.setCheckedIndex(
                            if (model.isAllowedChangingState) model.selectedIndex else -1
                        )
                        pref.setOnButtonClickListener { _, checkedId, isChecked ->
                            val checkedIndex =
                                when (checkedId) {
                                    com.android.settingslib.widget.preference.segmentedbutton.R.id
                                        .button_1 -> {
                                        0
                                    }

                                    com.android.settingslib.widget.preference.segmentedbutton.R.id
                                        .button_2 -> {
                                        1
                                    }

                                    com.android.settingslib.widget.preference.segmentedbutton.R.id
                                        .button_3 -> {
                                        2
                                    }

                                    com.android.settingslib.widget.preference.segmentedbutton.R.id
                                        .button_4 -> {
                                        3
                                    }

                                    else -> {
                                        return@setOnButtonClickListener
                                    }
                                }
                            if (isChecked) {
                                for (idx in 0..3) {
                                    pref.setButtonEnabled(idx, false)
                                }
                                model.onSelectedChange(checkedIndex)
                            }
                        }
                    }
                }
            }

            is DeviceSettingPreferenceModel.FooterPreference -> {
                val pref = existedPref as? FooterPreference ?: FooterPreference(requireContext())
                pref.apply {
                    key = prefKey
                    order = prefOrder
                    title = model.footerText
                }
                container.addPreference(pref)
            }

            is DeviceSettingPreferenceModel.MoreSettingsPreference -> {
                val pref = existedPref ?: Preference(requireContext())
                pref.apply {
                    key = prefKey
                    order = prefOrder
                    title =
                        context.getString(R.string.bluetooth_device_more_settings_preference_title)
                    summary =
                        context.getString(
                            R.string.bluetooth_device_more_settings_preference_summary
                        )
                    icon =
                        context.getDrawable(
                            if (Flags.enableBluetoothSettingsExpressiveDesign()) {
                                R.drawable.ic_bluetooth_more_vert
                            } else {
                                R.drawable.ic_chevron_right_24dp
                            }
                        )
                    onPreferenceClickListener =
                        Preference.OnPreferenceClickListener {
                            logItemClick(prefKey, EVENT_CLICK_PRIMARY)
                            SubSettingLauncher(context)
                                .setDestination(DeviceDetailsMoreSettingsFragment::class.java.name)
                                .setSourceMetricsCategory(getMetricsCategory())
                                .setArguments(
                                    Bundle().apply {
                                        putString(KEY_DEVICE_ADDRESS, cachedDevice.address)
                                    }
                                )
                                .launch()
                            true
                        }
                }
                container.addPreference(pref)
            }

            is DeviceSettingPreferenceModel.HelpPreference -> {}

            is DeviceSettingPreferenceModel.BannerPreference -> {
                val pref =
                    existedPref as? BannerMessagePreference
                        ?: BannerMessagePreference(requireContext())
                pref.apply {
                    setAttentionLevel(BannerMessagePreference.AttentionLevel.NORMAL)
                    key = prefKey
                    order = prefOrder
                    title = model.title
                    summary = model.message
                    icon = getDrawable(model.icon, false)
                    if (model.positiveButton != null && model.positiveButton.action != null) {
                        setPositiveButtonText(model.positiveButton.label)
                        setPositiveButtonOnClickListener {
                            model.positiveButton.action?.let { triggerAction(it) }
                        }
                        setPositiveButtonEnabled(true)
                        setPositiveButtonVisible(true)
                    }
                    if (model.negativeButton != null && model.negativeButton.action != null) {
                        setNegativeButtonText(model.negativeButton.label)
                        setNegativeButtonOnClickListener {
                            model.negativeButton.action?.let { triggerAction(it) }
                        }
                        setNegativeButtonEnabled(true)
                        setNegativeButtonVisible(true)
                    }
                }
                container.addPreference(pref)
            }
        }

    private fun getDrawable(
        deviceSettingIcon: DeviceSettingIcon?,
        applyTint: Boolean = true,
    ): Drawable? =
        when (deviceSettingIcon) {
            is DeviceSettingIcon.BitmapIcon ->
                deviceSettingIcon.bitmap.toDrawable(requireContext().resources)

            is DeviceSettingIcon.ResourceIcon -> context?.getDrawable(deviceSettingIcon.resId)
            null -> null
        }?.apply {
            if (applyTint) {
                setTint(
                    requireContext()
                        .getColor(
                            com.android.settingslib.widget.theme.R.color
                                .settingslib_materialColorOnSurfaceVariant
                        )
                )
            }
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
                        .launchIn(lifecycleScope)
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
                startActivity(action.intent)
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

    private fun updatePreferenceOrder() {
        val order = displayOrder ?: return
        if (preferenceScreen == null) {
            return
        }
        val allPrefs =
            (invisiblePrefCategory.getAndRemoveAllChildren() +
                    preferenceScreen.getAndRemoveAllChildren())
                .filter { it != invisiblePrefCategory }
        val visiblePrefs = allPrefs.filter { order.contains(it.key) }
        visiblePrefs.forEach { it.order = order.indexOf(it.key) }
        val invisiblePrefs = allPrefs.filter { !order.contains(it.key) }
        preferenceScreen.addPreferences(visiblePrefs)
        preferenceScreen.addPreference(invisiblePrefCategory)
        invisiblePrefCategory.addPreferences(invisiblePrefs)
    }

    private fun PreferenceGroup.getAllChildren(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0..<preferenceCount) {
            prefs.add(getPreference(i))
        }
        return prefs
    }

    private fun PreferenceGroup.getAndRemoveAllChildren(): List<Preference> {
        val prefs = getAllChildren()
        removeAll()
        return prefs
    }

    private fun PreferenceGroup.addPreferences(prefs: List<Preference>) {
        for (pref in prefs) {
            addPreference(pref)
        }
    }

    private fun getAllPreferences(): List<Preference> {
        val prefs = mutableListOf<Preference>()
        for (i in 0 until preferenceScreen.preferenceCount) {
            val pref = preferenceScreen.getPreference(i)
            if (pref.key == INVISIBLE_CATEGORY && pref is PreferenceCategory) {
                prefs.addAll(pref.getAllChildren())
            } else {
                prefs.add(pref)
            }
        }
        return prefs
    }

    fun getCachedDevice(deviceAddress: String): CachedBluetoothDevice? {
        val remoteDevice: BluetoothDevice =
            localBluetoothManager.bluetoothAdapter.getRemoteDevice(deviceAddress) ?: return null
        val cachedDevice: CachedBluetoothDevice? =
            localBluetoothManager.cachedDeviceManager.findDevice(remoteDevice)
        if (cachedDevice != null) {
            return cachedDevice
        }
        if (remoteDevice.bondState != BluetoothDevice.BOND_BONDED) {
            return null
        }
        Log.i(TAG, "Add device to cached device manager: " + remoteDevice.anonymizedAddress)
        return localBluetoothManager.cachedDeviceManager.addDevice(remoteDevice)
    }

    protected fun isCachedDeviceInitialized() = ::cachedDevice.isInitialized

    private fun launchConnectedDevicesScreen() {
        SubSettingLauncher(context)
            .setDestination(ConnectedDeviceDashboardFragment::class.java.getName())
            .setTitleRes(R.string.connected_devices_dashboard_title)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setSourceMetricsCategory(SettingsEnums.BLUETOOTH_DEVICE_DETAILS)
            .launch()
    }

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

    companion object {
        const val KEY_DEVICE_ADDRESS = "device_address"
        const val LOADING_PREF = "loading_pref"

        private const val TAG = "BtDetailsConfigFrg"
        private const val INVISIBLE_CATEGORY = "invisible_profile_category"
        private const val EVENT_SWITCH_OFF = 0
        private const val EVENT_SWITCH_ON = 1
        private const val EVENT_CLICK_PRIMARY = 2
        private const val EVENT_INVISIBLE = 0
        private const val EVENT_VISIBLE = 1

        private fun getPreferenceKey(settingId: Int) = "DEVICE_SETTING_$settingId"

        private fun getPreferenceCategoryKey(settingId: Int) = "CATEGORY_STARTS_WITH_$settingId"
    }
}
