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
package com.android.settings.widget

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.Spinner
import com.android.settingslib.widget.SettingsSpinnerAdapter

class EnhancedSettingsSpinnerAdapter<T>(context: Context?, items: Array<T>) :
    SettingsSpinnerAdapter<T>(context) {

    init {
        addAll(items.toList())
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        if (parent is Spinner) {
            setSelectedPosition(parent.selectedItemPosition)
        }
        return super.getDropDownView(position, convertView, parent)
    }

    companion object {
        @JvmStatic
        fun adjustDropDownOffset(spinner: Spinner) {
            spinner.viewTreeObserver.addOnGlobalLayoutListener(
                object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        spinner.dropDownVerticalOffset = spinner.height + 2
                        spinner.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    }
                }
            )
        }
    }
}
