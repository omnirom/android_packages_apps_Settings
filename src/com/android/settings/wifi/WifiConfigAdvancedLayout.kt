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

package com.android.settings.wifi

import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.android.settings.R

class WifiConfigAdvancedLayout(val view: View) {

    private val layout = view.requireViewById<LinearLayout>(R.id.advanced_options_layout)
    private val icon = view.requireViewById<ImageView>(R.id.advanced_options_icon)
    private val fields = view.requireViewById<LinearLayout>(R.id.wifi_advanced_fields)

    private val expandedBackground =
        ContextCompat.getDrawable(view.context, R.drawable.advanced_options_expanded_background)
    private val collapsedBackground =
        ContextCompat.getDrawable(view.context, R.drawable.advanced_options_collapsed_background)

    private var isExpanded = false

    init {
        layout.setOnClickListener { expanded = !isExpanded }
    }

    var expanded: Boolean
        get() = isExpanded
        set(value) {
            if (value == isExpanded) return
            isExpanded = value
            if (value) {
                layout.background = expandedBackground
                icon.setImageResource(R.drawable.keyboard_arrow_up_24px)
                fields.visibility = View.VISIBLE
                hideSoftKeyboard()
            } else {
                layout.background = collapsedBackground
                icon.setImageResource(R.drawable.ic_keyboard_arrow_down)
                fields.visibility = View.GONE
            }
        }

    private fun hideSoftKeyboard() {
        view.context.getSystemService(InputMethodManager::class.java)?.let {
            it.hideSoftInputFromWindow(view.windowToken, 0 /* flags */)
        }
    }

    companion object {
        const val TAG = "AdvancedOptionsHelper"
    }
}
