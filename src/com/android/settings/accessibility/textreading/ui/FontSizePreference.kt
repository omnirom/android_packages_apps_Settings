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

package com.android.settings.accessibility.textreading.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import androidx.preference.Preference
import com.android.internal.accessibility.AccessibilityShortcutController
import com.android.settings.R
import com.android.settings.accessibility.AccessibilityQuickSettingUtils
import com.android.settings.accessibility.AccessibilityQuickSettingsTooltipWindow
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint
import com.android.settings.accessibility.TooltipSliderPreference
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_BUTTON_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.CHANGE_BY_SLIDER_DELAY
import com.android.settings.accessibility.shared.utils.DebounceConfigurationChangeCommitController.Companion.MIN_COMMIT_DELAY
import com.android.settings.accessibility.textreading.data.FontSizeDataStore
import com.android.settingslib.R as SettingsLibR
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.Permissions
import com.android.settingslib.metadata.IntRangeValuePreference
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.metadata.SensitivityLevel
import com.android.settingslib.widget.SliderPreference
import com.android.settingslib.widget.SliderPreferenceBinding
import com.google.android.material.slider.Slider
import kotlin.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class FontSizePreference(context: Context, @EntryPoint private val entryPoint: Int) :
    IntRangeValuePreference, SliderPreferenceBinding, PreferenceLifecycleProvider {
    private val fontSizeDataStore by lazy {
        FontSizeDataStore(context = context, entryPoint = entryPoint)
    }
    private val fontSizes by lazy { fontSizeDataStore.fontSizeData.value.values }
    private val fontSizesLabel by lazy {
        fontSizes
            .map { value ->
                context.getString(SettingsLibR.string.font_scale_percentage, (value * 100).toInt())
            }
            .toTypedArray()
    }
    private var isDraggingSlider = false
    private val _fontSizePreview by lazy { MutableStateFlow(fontSizeDataStore.fontSizeData.value) }
    /**
     * [fontSizePreview] is the temporary font size while the user is dragging and haven't commit
     * the change. This is useful when trying to display preview of the size changes.
     */
    val fontSizePreview by lazy { _fontSizePreview.asStateFlow() }

    private val debounceCommitController by lazy {
        DebounceConfigurationChangeCommitController(minCommitDelay = MIN_COMMIT_DELAY)
    }

    override fun getReadPermissions(context: Context) = Permissions.EMPTY

    override fun getWritePermissions(context: Context) =
        Permissions.allOf(
            Manifest.permission.WRITE_SETTINGS, // required to write System settings
            Manifest.permission.WRITE_SECURE_SETTINGS, // required to write Secure settings
        )

    override fun getReadPermit(context: Context, callingPid: Int, callingUid: Int) =
        ReadWritePermit.ALLOW

    override fun getWritePermit(
        context: Context,
        callingPid: Int,
        callingUid: Int,
    ): @ReadWritePermit Int {
        return ReadWritePermit.ALLOW
    }

    override val sensitivityLevel
        get() = SensitivityLevel.NO_SENSITIVITY

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.title_font_size

    override val summary: Int
        get() = R.string.short_summary_font_size

    override val keywords: Int
        get() = R.string.keywords_font_size

    override fun createWidget(context: Context) =
        TooltipSliderPreference(context).apply {
            setIconStart(R.drawable.ic_remove_24dp)
            setIconStartContentDescription(R.string.font_size_make_smaller_desc)
            setIconEnd(R.drawable.ic_add_24dp)
            setIconEndContentDescription(R.string.font_size_make_larger_desc)
            setTickVisible(true)
            setDefaultValue(_fontSizePreview.value.currentIndex)
            setExtraChangeListener { slider, value, fromUser ->
                onValueChange(preference = this@apply, value = value)
            }
            setExtraTouchListener(
                object : Slider.OnSliderTouchListener {
                    override fun onStartTrackingTouch(slider: Slider) {
                        onStartTrackingTouch()
                    }

                    override fun onStopTrackingTouch(slider: Slider) {
                        onStopTrackingTouch(preference = this@apply, slider = slider)
                    }
                }
            )
        }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        // The PreferenceMetadata is persistent to allow GET/SET api to access the storage.
        // Set the preference widget to non-persistent to prevent it trying to save the value to
        // datastore while the user is dragging, or when we want to have some delay to show the
        // preview before committing the changes.
        preference as SliderPreference
        preference.run {
            value = _fontSizePreview.value.currentIndex
            setSliderStateDescription(fontSizesLabel[value])
            isPersistent = false
        }
    }

    override fun onStart(context: PreferenceLifecycleContext) {
        super.onStart(context)
        context.findPreference<TooltipSliderPreference>(KEY)?.run {
            // This is needed to prevent slider value gets overwrites by
            // [View#onRestoreInstanceState].
            // When the display size changed, it triggers the configuration changes. The
            // SliderPreference widget is not a persistent preference, hence when the data is
            // changed outside of Settings app while the display size slider is visible, the Slider
            // widget won't save the correct index when
            // [View#onSaveInstanceState] is called.

            value = _fontSizePreview.value.currentIndex

            if (needsQSTooltipReshow) {
                context.lifecycleScope.launch(Dispatchers.Main) {
                    showQuickSettingsTooltipIfNeeded(preference = this@run)
                }
            }
        }
    }

    override fun onDestroy(context: PreferenceLifecycleContext) {
        super.onDestroy(context)
        context.findPreference<TooltipSliderPreference>(KEY)?.run { dismissTooltip() }
    }

    override fun getIncrementStep(context: Context): Int {
        return 1
    }

    override fun getMinValue(context: Context): Int {
        return 0
    }

    override fun getMaxValue(context: Context): Int {
        return fontSizes.size - 1
    }

    override fun storage(context: Context): KeyValueStore {
        return fontSizeDataStore
    }

    private fun onStartTrackingTouch() {
        isDraggingSlider = true
    }

    private fun onStopTrackingTouch(preference: TooltipSliderPreference, slider: Slider) {
        isDraggingSlider = false
        commitChange(CHANGE_BY_SLIDER_DELAY, preference, slider.value.toInt())
    }

    private fun onValueChange(preference: TooltipSliderPreference, value: Float) {
        preference.setSliderStateDescription(fontSizesLabel[value.toInt()])
        _fontSizePreview.value = _fontSizePreview.value.copy(currentIndex = value.toInt())

        if (!isDraggingSlider) {
            // if not dragging call datastore to save the value
            commitChange(CHANGE_BY_BUTTON_DELAY, preference, value.toInt())
        }
    }

    private fun commitChange(delay: Duration, preference: TooltipSliderPreference, index: Int) {
        if (index != fontSizeDataStore.getInt(KEY)) {
            showQuickSettingsTooltipIfNeeded(preference)
        }
        debounceCommitController.commitDelayed(delay) { fontSizeDataStore.setInt(KEY, index) }
    }

    private fun showQuickSettingsTooltipIfNeeded(preference: TooltipSliderPreference) {
        val context = preference.context
        if (context.isInSetupWizard()) {
            // Don't show Quick Settings tooltip in Setup Wizard
            return
        }

        val tileComponentName = AccessibilityShortcutController.FONT_SIZE_COMPONENT_NAME
        val shouldSkipShowingTooltip =
            !preference.needsQSTooltipReshow &&
                AccessibilityQuickSettingUtils.hasValueInSharedPreferences(
                    context,
                    tileComponentName,
                )

        if (shouldSkipShowingTooltip) {
            return
        }

        // TODO (b/287728819): Move tooltip showing to SystemUI
        val decorView = (context as? Activity)?.window?.peekDecorView()
        if (decorView != null) {
            val tooltipContent =
                context.getText(R.string.accessibility_font_scaling_auto_added_qs_tooltip_content)
            val tooltipWindow: AccessibilityQuickSettingsTooltipWindow =
                preference.createTooltipWindow()
            tooltipWindow.setup(
                tooltipContent,
                R.drawable.accessibility_auto_added_qs_tooltip_illustration,
            )
            tooltipWindow.showAtTopCenter(decorView)
        }
        AccessibilityQuickSettingUtils.optInValueToSharedPreferences(context, tileComponentName)
        preference.needsQSTooltipReshow = false
    }

    companion object {
        const val KEY = "font_size"
    }
}
