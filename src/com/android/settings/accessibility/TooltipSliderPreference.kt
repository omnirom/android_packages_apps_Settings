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
package com.android.settings.accessibility

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.preference.PreferenceViewHolder
import com.android.settings.accessibility.extensions.isInSetupWizard
import com.android.settingslib.widget.SliderPreference

/** Custom version of {@link SliderPreference} with tool tip window. */
class TooltipSliderPreference
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    SliderPreference(context, attrs, defStyleAttr) {

    var needsQSTooltipReshow = false
    private var tooltipWindow: AccessibilityQuickSettingsTooltipWindow? = null

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        // A temp fix for b/421323125, we could potentially move this fix to SettingsLib.
        // Once move this fix to SettingsLib, revert the change here.
        if (context.isInSetupWizard()) {
            val iconStartFrame =
                holder.findViewById(
                    com.android.settingslib.widget.preference.slider.R.id.icon_start
                )?.parent as? ViewGroup
            val iconEndFrame =
                holder.findViewById(com.android.settingslib.widget.preference.slider.R.id.icon_end)
                    ?.parent as? ViewGroup
            if (iconStartFrame?.isVisible == true) {
                iconStartFrame.setOnClickListener { _ ->
                    if (value > 0) {
                        val newValue = value - sliderIncrement
                        // Set the Slider value here in order to trigger Slider.OnChangeListener
                        slider?.value = newValue.toFloat()
                        value = newValue
                    }
                }
            }

            if (iconEndFrame?.isVisible == true) {
                iconEndFrame.setOnClickListener { _ ->
                    if (value < max) {
                        val newValue = value + sliderIncrement
                        // Set the Slider value here in order to trigger Slider.OnChangeListener
                        slider?.value = newValue.toFloat()
                        value = newValue
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val myState = SavedState(super.onSaveInstanceState())
        if (needsQSTooltipReshow || tooltipWindow?.isShowing == true) {
            myState.needsQSTooltipReshow = true
        }
        return myState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        if (state == null || !state.javaClass.equals(SavedState::class.java)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state)
            return
        }
        val myState = state as SavedState
        super.onRestoreInstanceState(myState.superState)
        needsQSTooltipReshow = myState.needsQSTooltipReshow
    }

    /** To generate a tooltip window and return it. */
    fun createTooltipWindow(): AccessibilityQuickSettingsTooltipWindow =
        AccessibilityQuickSettingsTooltipWindow(context).also { tooltipWindow = it }

    /** To dismiss the tooltip window. */
    fun dismissTooltip() {
        val tooltip = tooltipWindow
        if (tooltip?.isShowing == true) {
            tooltip.dismiss()
            tooltipWindow = null
        }
    }

    class SavedState : BaseSavedState {
        var needsQSTooltipReshow = false

        internal constructor(source: Parcel) : super(source) {
            needsQSTooltipReshow = source.readBoolean()
        }

        internal constructor(superState: Parcelable?) : super(superState)

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeBoolean(needsQSTooltipReshow)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState?> =
                object : Parcelable.Creator<SavedState?> {
                    override fun createFromParcel(`in`: Parcel): SavedState {
                        return SavedState(`in`)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return kotlin.arrayOfNulls(size)
                    }
                }
        }
    }
}
