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

package com.android.settings.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothHapClient
import android.bluetooth.BluetoothHapPresetInfo
import android.content.Context
import android.content.DialogInterface
import android.os.Parcel
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AlertDialog
import com.android.settings.R
import com.android.settings.overlay.FeatureFactory
import com.android.settingslib.CustomDialogPreferenceCompat
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_LEFT
import com.android.settingslib.bluetooth.HearingAidInfo.DeviceSide.SIDE_RIGHT
import com.android.settingslib.bluetooth.hearingdevices.ui.ExpandableControlUi.Companion.SIDE_UNIFIED
import com.android.settingslib.bluetooth.hearingdevices.ui.PresetSpinner
import com.android.settingslib.bluetooth.hearingdevices.ui.PresetUi
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap

/**
 * A custom preference that displays a dialog for managing hearing device presets.
 *
 * This class extends [CustomDialogPreferenceCompat] and implements the [PresetUi] interface to act
 * as a view for a controller. It dynamically creates and manages [PresetSpinner] controls, handles
 * their state, and saves/restores the state across configuration changes to ensure a consistent
 * user experience.
 */
@SuppressLint("InflateParams")
class PresetPreference(private val context: Context) :
    CustomDialogPreferenceCompat(context), PresetUi {

    private val sideToControlMap: BiMap<Int, PresetSpinner> = HashBiMap.create()
    private val customTitleView: View
    private var uiListener: PresetUi.PresetUiListener? = null
    private var expanded = false
    private var metricsCategory = 0
    private val onChangeListener: PresetSpinner.OnChangeListener =
        PresetSpinner.OnChangeListener { spinner: PresetSpinner, value: Int ->
            val side = sideToControlMap.inverse()[spinner]
            if (side != null) {
                logMetrics(side)
                uiListener?.onPresetChangedFromUi(side, value)
                notifyChanged()
            }
        }

    init {
        title = context.getString(R.string.bluetooth_hearing_aids_presets)
        dialogLayoutResource = R.layout.hearing_device_preset_dialog
        positiveButtonText = context.getString(R.string.done)
        customTitleView =
            LayoutInflater.from(context)
                .inflate(R.layout.hearing_device_preset_dialog_custom_title, null)
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.expanded = expanded
        savedState.presetDataList =
            sideToControlMap.map { (side, control) ->
                val presetList = control.getList() ?: emptyList()
                val activePreset = control.getValue()
                PresetData(side, presetList, activePreset)
            }
        return savedState
    }

    public override fun onRestoreInstanceState(state: Parcelable?) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }
        super.onRestoreInstanceState(state.superState)
        expanded = state.expanded
        state.presetDataList?.forEach { data ->
            createControl(context, data.side)
            sideToControlMap[data.side]?.apply {
                setList(data.presetList)
                setValue(data.activePreset)
            }
        }
    }

    public override fun onBindDialogView(view: View?) {
        super.onBindDialogView(view)
        val controlLayout: ViewGroup? = view?.requireViewById(R.id.preset_control_layout)
        if (controlLayout != null) {
            val controlSides = if (expanded) listOf(SIDE_LEFT, SIDE_RIGHT) else listOf(SIDE_UNIFIED)
            controlSides.forEach { side ->
                sideToControlMap[side]?.let { control ->
                    (control.parent as? ViewGroup)?.removeView(control)
                    controlLayout.addView(control)
                }
            }
        }
    }

    override fun onPrepareDialogBuilder(
        builder: AlertDialog.Builder?,
        listener: DialogInterface.OnClickListener?,
    ) {
        super.onPrepareDialogBuilder(builder, listener)
        (customTitleView.parent as? ViewGroup)?.removeView(customTitleView)
        builder?.apply { setCustomTitle(customTitleView) }
    }

    override fun getSummary(): CharSequence? {
        if (expanded) {
            val leftControlValid = sideToControlMap[SIDE_LEFT]?.getList()?.isNotEmpty() == true
            val rightControlValid = sideToControlMap[SIDE_RIGHT]?.getList()?.isNotEmpty() == true
            if (leftControlValid || rightControlValid) {
                return context.getString(
                    R.string.bluetooth_hearing_aids_presets_binaural_summary,
                    sideToControlMap[SIDE_LEFT]?.getValueName(),
                    sideToControlMap[SIDE_RIGHT]?.getValueName(),
                )
            }
        } else {
            val controlValid = sideToControlMap[SIDE_UNIFIED]?.getList()?.isNotEmpty() == true
            if (controlValid) {
                return sideToControlMap[SIDE_UNIFIED]?.getValueName()
            }
        }
        return context.getString(R.string.bluetooth_hearing_aids_presets_empty_list_message)
    }

    override fun setListener(listener: PresetUi.PresetUiListener?) {
        uiListener = listener
    }

    override fun setupControls(sides: Set<Int>) {
        sides.forEach { side -> createControl(context, side) }
        createControl(context, SIDE_UNIFIED)
    }

    override fun setControlEnabled(side: Int, enabled: Boolean) {
        sideToControlMap[side]?.apply {
            if (isEnabled != enabled) {
                isEnabled = enabled

                val controls = if (expanded) listOf(SIDE_LEFT, SIDE_RIGHT) else listOf(SIDE_UNIFIED)
                val hasEnabledControl = controls.any { sideToControlMap[it]?.isEnabled == true }
                this@PresetPreference.isEnabled = hasEnabledControl
                notifyChanged()
            }
        }
    }

    override fun setControlList(side: Int, presetInfos: List<BluetoothHapPresetInfo>) {
        sideToControlMap[side]?.apply {
            setList(presetInfos)
            if (presetInfos.isEmpty()) {
                setControlEnabled(side, false)
            }
        }
    }

    override fun setControlValue(side: Int, presetIndex: Int) {
        sideToControlMap[side]?.apply {
            if (getValue() != presetIndex) {
                setValue(presetIndex)
                notifyChanged()
            }
        }
    }

    override fun getControlValue(side: Int): Int {
        return sideToControlMap[side]?.getValue() ?: BluetoothHapClient.PRESET_INDEX_UNAVAILABLE
    }

    override fun setControlExpanded(expanded: Boolean) {
        if (this.expanded != expanded) {
            this.expanded = expanded
            notifyChanged()
        }
    }

    override fun isControlExpanded(): Boolean {
        return expanded
    }

    /** Sets the metrics category. */
    fun setMetricsCategory(category: Int) {
        metricsCategory = category
    }

    fun getMetricsCategory(): Int {
        return metricsCategory
    }

    private fun createControl(context: Context, side: Int) {
        if (sideToControlMap.containsKey(side)) {
            return
        }
        PresetSpinner(context).apply {
            val titleRes =
                when (side) {
                    SIDE_LEFT -> R.string.bluetooth_hearing_aids_presets_left
                    SIDE_RIGHT -> R.string.bluetooth_hearing_aids_presets_right
                    else -> null
                }
            if (titleRes != null) {
                setTitle(context.getString(titleRes))
            }
            setOnChangeListener(onChangeListener)
            sideToControlMap[side] = this
        }
    }

    private fun logMetrics(side: Int) {
        if (metricsCategory != 0) {
            FeatureFactory.featureFactory.metricsFeatureProvider.changed(
                metricsCategory,
                KEY_HEARING_AIDS_PRESETS,
                side,
            )
        }
    }

    @VisibleForTesting
    fun getControls(): Map<Int, PresetSpinner> {
        return sideToControlMap
    }

    /** A [BaseSavedState] subclass for storing the state of [PresetPreference]. */
    private class SavedState : BaseSavedState {

        var presetDataList: List<PresetData>? = null
        var expanded: Boolean = false

        constructor(superState: Parcelable?) : super(superState)

        constructor(source: Parcel) : super(source) {
            expanded = source.readBoolean()
            presetDataList = source.createTypedArrayList(PresetData.CREATOR)
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeBoolean(expanded)
            out.writeTypedList(presetDataList)
        }

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel): SavedState {
                return SavedState(parcel)
            }

            override fun newArray(size: Int): Array<SavedState?> {
                return arrayOfNulls(size)
            }
        }
    }

    /** A data class for storing the state of a single preset control. */
    private data class PresetData(
        val side: Int,
        val presetList: List<BluetoothHapPresetInfo>,
        val activePreset: Int,
    ) : Parcelable {
        constructor(
            parcel: Parcel
        ) : this(
            parcel.readInt(),
            parcel.createTypedArrayList(BluetoothHapPresetInfo.CREATOR) ?: emptyList(),
            parcel.readInt(),
        )

        override fun writeToParcel(parcel: Parcel, flags: Int) {
            parcel.writeInt(side)
            parcel.writeTypedList(presetList)
            parcel.writeInt(activePreset)
        }

        override fun describeContents(): Int {
            return 0
        }

        companion object CREATOR : Parcelable.Creator<PresetData> {
            override fun createFromParcel(parcel: Parcel): PresetData {
                return PresetData(parcel)
            }

            override fun newArray(size: Int): Array<PresetData?> {
                return arrayOfNulls(size)
            }
        }
    }

    companion object {
        private const val KEY_HEARING_AIDS_PRESETS = "hearing_aids_presets"
    }
}
