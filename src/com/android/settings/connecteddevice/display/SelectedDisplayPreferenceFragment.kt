/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.app.ActivityManager.LOCK_TASK_MODE_LOCKED
import android.app.admin.EnforcingAdmin
import android.app.settings.SettingsEnums
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY
import android.window.DesktopExperienceFlags
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModelProvider
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.SwitchPreferenceCompat
import com.android.settings.R
import com.android.settings.RestrictedListPreference
import com.android.settings.SettingsPreferenceFragmentBase
import com.android.settings.Utils.createAccessibleSequence
import com.android.settings.accessibility.TextReadingPreferenceFragment
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL
import com.android.settings.core.SubSettingLauncher

/**
 * Fragment containing list of preferences for a single display, which gets updated dynamically,
 * based on the currently selected display
 *
 * Keep [parentViewModel] param as nullable since config changes could only re-create Fragment with
 * empty constructor, even though [parentViewModel] will always be passed
 */
open class SelectedDisplayPreferenceFragment(
    private val parentViewModel: DisplayPreferenceViewModel? = null
) : SettingsPreferenceFragmentBase() {

    private lateinit var viewModel: DisplayPreferenceViewModel

    private lateinit var selectedDisplayPreference: PreferenceCategory
    private lateinit var rotationEntries: Array<String>
    private lateinit var connectionPreferenceEntries: Array<String>
    // Late init - Only updated once in onCreate
    private var shouldShowDisplayConnectionPref: Boolean = false

    private val prefComponents = mutableListOf<PrefComponent>()

    override fun getMetricsCategory(): Int {
        return SettingsEnums.SETTINGS_EXTERNAL_DISPLAY_CATEGORY
    }

    override fun getHelpResource(): Int {
        return EXTERNAL_DISPLAY_HELP_URL
    }

    override fun getPreferenceScreenResId(): Int {
        return R.xml.external_display_settings
    }

    open fun launchResolutionSelector(displayId: Int) {
        val args =
            Bundle().apply {
                putInt(ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG, displayId)
            }
        SubSettingLauncher(requireContext())
            .setDestination(ResolutionPreferenceFragment::class.java.name)
            .setArguments(args)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    open fun launchBuiltinDisplaySettings() {
        val args = Bundle()
        SubSettingLauncher(requireContext())
            .setDestination(TextReadingPreferenceFragment::class.java.getName())
            .setArguments(args)
            .setSourceMetricsCategory(metricsCategory)
            .launch()
    }

    override fun onActivityCreatedCallback(savedInstanceState: Bundle?) {
        // No-op, nothing specific to be done here
    }

    override fun onCreateCallback(icicle: Bundle?) {
        addPreferencesFromResource(R.xml.external_display_settings)
        if (parentViewModel != null) {
            viewModel = parentViewModel
        } else {
            // ViewModel will be passed from parent fragment, but when fragment is recreated from
            // config changes, constructor param will be empty, and has to re-fetch the ViewModel
            // here
            viewModel =
                ViewModelProvider(requireParentFragment())
                    .get(DisplayPreferenceViewModel::class.java)
        }
        shouldShowDisplayConnectionPref =
            DesktopExperienceFlags.ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG.isTrue() &&
                viewModel.injector.isProjectedModeEnabled()
        setup()
    }

    override fun onStartCallback() {
        viewModel.uiState.observe(viewLifecycleOwner) { state -> update(state) }
    }

    override fun onStopCallback() {
        // No-op, viewModel observer should be managed by view lifecycle
    }

    private fun setup() {
        selectedDisplayPreference = PreferenceCategory(requireContext())
        preferenceScreen.addPreference(selectedDisplayPreference)

        // Built-in display preferences
        prefComponents.add(PrefComponent(mirroringPreference(), PrefInfo.DISPLAY_MIRRORING))
        prefComponents.add(
            PrefComponent(
                includeDefaultDisplayInTopologyPreference(),
                PrefInfo.INCLUDE_DEFAULT_DISPLAY,
            )
        )
        // Built-in display preferences - sub-category
        val builtinDisplaySubCategory = PreferenceCategory(requireContext())
        builtinDisplaySubCategory.key = PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY.key
        prefComponents.add(
            PrefComponent(builtinDisplaySubCategory, PrefInfo.BUILTIN_DISPLAY_SUB_CATEGORY)
        )
        prefComponents.add(
            PrefComponent(builtinDisplayDensityPreference(), PrefInfo.BUILTIN_DISPLAY_DENSITY)
        )
        // External display preferences
        prefComponents.add(
            PrefComponent(externalDisplayDensityPreference(), PrefInfo.EXTERNAL_DISPLAY_DENSITY)
        )
        prefComponents.add(PrefComponent(resolutionPreference(), PrefInfo.DISPLAY_RESOLUTION))
        prefComponents.add(PrefComponent(rotationPreference(), PrefInfo.DISPLAY_ROTATION))
        if (shouldShowDisplayConnectionPref) {
            // Since `shouldShowDisplayConnectionPref` is static until reboot, instead of updating
            // visibility like the other preferences, just skip setting up connection pref
            // altogether
            prefComponents.add(
                PrefComponent(connectionPreference(), PrefInfo.DISPLAY_CONNECTION_PREFERENCE)
            )
        }

        // Add all pref components
        prefComponents.forEach {
            when (it.prefInfo.parentPrefCategory) {
                ParentPrefCategory.ROOT -> selectedDisplayPreference.addPreference(it.preference)
                ParentPrefCategory.BUILTIN_DISPLAY_SUB_CATEGORY ->
                    builtinDisplaySubCategory.addPreference(it.preference)
            }
            it.preference.setPersistent(false)
        }
    }

    /**
     * Note that update will be called on every tab switch as well. So all the operations to update
     * UI should be fast and not fetching any new data (use only what's available from
     * [DisplayPreferenceViewModel.DisplayUiState])
     */
    private fun update(state: DisplayPreferenceViewModel.DisplayUiState) {
        val displayId = state.selectedDisplayId
        val enabledDisplays = state.enabledDisplays
        val isMirroring = state.isMirroring

        val display = enabledDisplays[displayId]
        // By design, if there's one or more enabled connected displays, `displayId` should always
        // be a valid key of `enabledDisplays`. In case where there's no enabled connected display,
        // parent fragment should set this fragment to INVISIBLE.
        // This is just a fail-safe for unexpected behavior
        if (display == null) {
            prefComponents.forEach { it.preference.setVisible(false) }
            selectedDisplayPreference.setTitle("")
            return
        }
        val displayType =
            if (display.isConnectedDisplay) DisplayType.EXTERNAL_DISPLAY
            else DisplayType.BUILTIN_DISPLAY
        prefComponents.forEach { it.preference.setVisible(it.prefInfo.displayType == displayType) }

        if (displayType == DisplayType.BUILTIN_DISPLAY) {
            selectedDisplayPreference
                .findPreference<SwitchPreferenceCompat>(PrefInfo.INCLUDE_DEFAULT_DISPLAY.key)
                ?.let { updateIncludeDefaultDisplayInTopologyPreference(it, state) }
            selectedDisplayPreference
                .findPreference<MirrorPreference>(PrefInfo.DISPLAY_MIRRORING.key)
                ?.let { updateMirroringPreference(it, isMirroring, state.lockTaskPolicyInfo) }
        } else {
            selectedDisplayPreference
                .findPreference<ExternalDisplaySizePreference>(
                    PrefInfo.EXTERNAL_DISPLAY_DENSITY.key
                )
                ?.let { updateExternalDisplayDensityPreference(it, display, isMirroring) }
            selectedDisplayPreference
                .findPreference<Preference>(PrefInfo.DISPLAY_RESOLUTION.key)
                ?.let { updateResolutionPreference(it, display) }
            selectedDisplayPreference
                .findPreference<ListPreference>(PrefInfo.DISPLAY_ROTATION.key)
                ?.let { updateRotationPreference(it, display) }
            if (shouldShowDisplayConnectionPref) {
                selectedDisplayPreference
                    .findPreference<RestrictedListPreference>(
                        PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key
                    )
                    ?.let { updateConnectionPreference(it, display, state.lockTaskPolicyInfo) }
            }
        }
    }

    private fun mirroringPreference(): MirrorPreference {
        return MirrorPreference(
                requireContext(),
                DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue(),
            )
            .apply {
                setTitle(PrefInfo.DISPLAY_MIRRORING.titleResource)
                key = PrefInfo.DISPLAY_MIRRORING.key
            }
    }

    private fun updateMirroringPreference(
        preference: MirrorPreference,
        isMirroring: Boolean,
        lockTaskPolicyInfo: DisplayPreferenceViewModel.LockTaskPolicyInfo,
    ) {
        preference.isChecked = isMirroring
        if (lockTaskPolicyInfo.lockTaskMode == LOCK_TASK_MODE_LOCKED) {
            preference.setDisabledByAdmin(lockTaskPolicyInfo.enforcingAdmin)
        } else {
            preference.setDisabledByAdmin(null as EnforcingAdmin?)
            // Reset the pref state when it was previously disabled by lock task policy
            preference.setEnabled(true)
            preference.setSummary("")
        }
    }

    private fun includeDefaultDisplayInTopologyPreference(): SwitchPreferenceCompat {
        return SwitchPreferenceCompat(requireContext()).apply {
            setTitle(PrefInfo.INCLUDE_DEFAULT_DISPLAY.titleResource)
            setSummary(R.string.builtin_display_settings_universal_cursor_description)
            key = PrefInfo.INCLUDE_DEFAULT_DISPLAY.key
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        writePreferenceClickMetric(preference)
                        Settings.Secure.putInt(
                            requireContext().getContentResolver(),
                            INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
                            if ((preference as SwitchPreferenceCompat).isChecked()) 1 else 0,
                        )
                        return true
                    }
                }
        }
    }

    private fun updateIncludeDefaultDisplayInTopologyPreference(
        preference: SwitchPreferenceCompat,
        state: DisplayPreferenceViewModel.DisplayUiState,
    ) {
        if (state.showIncludeDefaultDisplayInTopologyPref) {
            preference.setVisible(true)
            preference.setChecked(state.includeDefaultDisplayInTopology)
        } else {
            preference.setVisible(false)
            return
        }
    }

    private fun builtinDisplayDensityPreference(): Preference {
        return Preference(requireContext()).apply {
            isPersistent = false
            setTitle(PrefInfo.BUILTIN_DISPLAY_DENSITY.titleResource)
            key = PrefInfo.BUILTIN_DISPLAY_DENSITY.key
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        launchBuiltinDisplaySettings()
                        return true
                    }
                }
        }
    }

    private fun externalDisplayDensityPreference(): ExternalDisplaySizePreference {
        return ExternalDisplaySizePreference(requireContext(), /* attrs= */ null).apply {
            setTitle(PrefInfo.EXTERNAL_DISPLAY_DENSITY.titleResource)
            key = PrefInfo.EXTERNAL_DISPLAY_DENSITY.key
            setSummary(R.string.screen_zoom_short_summary)
        }
    }

    private fun updateExternalDisplayDensityPreference(
        preference: ExternalDisplaySizePreference,
        display: DisplayDeviceAdditionalInfo,
        isMirroring: Boolean,
    ) {
        if (isMirroring) {
            preference.setVisible(false)
            return
        }
        val displayMode = display.mode ?: return
        preference.setStateForPreference(
            displayMode.physicalWidth,
            displayMode.physicalHeight,
            display.id,
        )
    }

    private fun resolutionPreference(): Preference {
        return Preference(requireContext()).apply {
            setTitle(PrefInfo.DISPLAY_RESOLUTION.titleResource)
            key = PrefInfo.DISPLAY_RESOLUTION.key
            onPreferenceClickListener =
                object : Preference.OnPreferenceClickListener {
                    override fun onPreferenceClick(preference: Preference): Boolean {
                        writePreferenceClickMetric(preference)
                        val displayId = viewModel.uiState.value?.selectedDisplayId ?: return false
                        launchResolutionSelector(displayId)
                        return true
                    }
                }
        }
    }

    private fun updateResolutionPreference(
        preference: Preference,
        display: DisplayDeviceAdditionalInfo,
    ) {
        val displayMode = display.mode ?: return
        val width = displayMode.getPhysicalWidth()
        val height = displayMode.getPhysicalHeight()
        preference.setSummary(
            createAccessibleSequence(
                "$width x $height",
                getResources().getString(R.string.screen_resolution_delimiter_a11y, width, height),
            )
        )
    }

    private fun rotationPreference(): ListPreference {
        val context = requireContext()
        rotationEntries =
            arrayOf(
                context.getString(R.string.external_display_standard_rotation),
                context.getString(R.string.external_display_rotation_90),
                context.getString(R.string.external_display_rotation_180),
                context.getString(R.string.external_display_rotation_270),
            )
        val rotationEntryValues = arrayOf("0", "1", "2", "3")
        return ListPreference(context).apply {
            setTitle(PrefInfo.DISPLAY_ROTATION.titleResource)
            key = PrefInfo.DISPLAY_ROTATION.key
            setEntries(rotationEntries)
            setEntryValues(rotationEntryValues)
            onPreferenceChangeListener =
                object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(
                        preference: Preference,
                        newValue: Any?,
                    ): Boolean {
                        writePreferenceClickMetric(preference)
                        val displayId = viewModel.uiState.value?.selectedDisplayId ?: return false
                        val rotation = Integer.parseInt(newValue as String)
                        if (!viewModel.injector.freezeDisplayRotation(displayId, rotation)) {
                            return false
                        }
                        setValueIndex(rotation)
                        return true
                    }
                }
        }
    }

    private fun updateRotationPreference(
        preference: ListPreference,
        display: DisplayDeviceAdditionalInfo,
    ) {
        val rotation = display.rotation
        preference.apply {
            setValueIndex(rotation)
            setSummary(rotationEntries[rotation])
        }
    }

    private fun connectionPreference(): RestrictedListPreference {
        val context = requireContext()
        connectionPreferenceEntries =
            arrayOf(
                context.getString(R.string.external_display_connection_preference_show_dialog),
                context.getString(R.string.external_display_connection_preference_desktop),
                context.getString(R.string.external_display_connection_preference_mirroring),
            )
        // Entry values only accept array of string
        val connectionEntryValues =
            arrayOf(
                DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK.toString(),
                DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_DESKTOP.toString(),
                DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR.toString(),
            )
        return RestrictedListPreference(context).apply {
            setTitle(PrefInfo.DISPLAY_CONNECTION_PREFERENCE.titleResource)
            key = PrefInfo.DISPLAY_CONNECTION_PREFERENCE.key
            setEntries(connectionPreferenceEntries)
            setEntryValues(connectionEntryValues)
            onPreferenceChangeListener =
                object : Preference.OnPreferenceChangeListener {
                    override fun onPreferenceChange(
                        preference: Preference,
                        newValue: Any?,
                    ): Boolean {
                        writePreferenceClickMetric(preference)
                        val displayId = viewModel.uiState.value?.selectedDisplayId ?: return false
                        val uniqueDisplayId =
                            viewModel.uiState.value?.enabledDisplays[displayId]?.uniqueId
                                ?: return false
                        val connectionPreference = Integer.parseInt(newValue as String)
                        viewModel.injector.updateDisplayConnectionPreference(
                            uniqueDisplayId,
                            connectionPreference,
                        )
                        setValueIndex(connectionPreference)
                        setSummary(connectionPreferenceEntries[connectionPreference])
                        return true
                    }
                }
        }
    }

    private fun updateConnectionPreference(
        preference: RestrictedListPreference,
        display: DisplayDeviceAdditionalInfo,
        lockTaskPolicyInfo: DisplayPreferenceViewModel.LockTaskPolicyInfo,
    ) {
        if (lockTaskPolicyInfo.lockTaskMode == LOCK_TASK_MODE_LOCKED) {
            val connectionPreference = DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR
            preference.apply {
                setDisabledByAdmin(lockTaskPolicyInfo.enforcingAdmin)
                setValueIndex(connectionPreference)
                setSummary(connectionPreferenceEntries[connectionPreference])
            }
        } else {
            val connectionPreference = display.connectionPreference
            preference.apply {
                // Reset the pref state when it was previously disabled by lock task policy
                setDisabledByAdmin(null as EnforcingAdmin?)
                setEnabled(true)
                setValueIndex(connectionPreference)
                setSummary(connectionPreferenceEntries[connectionPreference])
            }
        }
    }

    internal enum class ParentPrefCategory {
        ROOT,
        BUILTIN_DISPLAY_SUB_CATEGORY,
    }

    @VisibleForTesting
    internal enum class DisplayType {
        BUILTIN_DISPLAY,
        EXTERNAL_DISPLAY,
    }

    @VisibleForTesting
    internal enum class PrefInfo(
        val titleResource: Int,
        val key: String,
        val displayType: DisplayType,
        val parentPrefCategory: ParentPrefCategory,
    ) {
        DISPLAY_MIRRORING(
            R.string.external_display_mirroring_title,
            "pref_key_builtin_display_mirroring",
            DisplayType.BUILTIN_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        INCLUDE_DEFAULT_DISPLAY(
            R.string.builtin_display_settings_universal_cursor_title,
            "pref_key_builtin_display_include_default_display_in_topology",
            DisplayType.BUILTIN_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        BUILTIN_DISPLAY_SUB_CATEGORY(
            -1,
            "pref_key_builtin_display_sub_category",
            DisplayType.BUILTIN_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        BUILTIN_DISPLAY_DENSITY(
            R.string.accessibility_text_reading_options_title,
            "pref_key_builtin_display_density",
            DisplayType.BUILTIN_DISPLAY,
            ParentPrefCategory.BUILTIN_DISPLAY_SUB_CATEGORY,
        ),
        EXTERNAL_DISPLAY_DENSITY(
            R.string.screen_zoom_title,
            "pref_key_external_display_density",
            DisplayType.EXTERNAL_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        DISPLAY_RESOLUTION(
            R.string.external_display_resolution_settings_title,
            "pref_key_external_display_resolution",
            DisplayType.EXTERNAL_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        DISPLAY_ROTATION(
            R.string.external_display_rotation,
            "pref_key_external_display_rotation",
            DisplayType.EXTERNAL_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
        DISPLAY_CONNECTION_PREFERENCE(
            R.string.external_display_connection_preference,
            "pref_key_external_display_connection_preference",
            DisplayType.EXTERNAL_DISPLAY,
            ParentPrefCategory.ROOT,
        ),
    }

    private data class PrefComponent(val preference: Preference, val prefInfo: PrefInfo)
}
