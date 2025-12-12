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

import android.content.Context
import android.icu.text.CaseMap
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.utils.LocaleUtils
import java.util.Locale

class ButtonNavigationSettingsOrderRadioButton(
    context: Context,
    val icons: List<Int>,
    val labels: List<Int>,
) : CheckBoxPreference(context) {
    var listener: OnClickListener? = null
    private var iconIds = listOf(R.id.icon_start, R.id.icon_center, R.id.icon_end)

    init {
        widgetLayoutResource =
            com.android.settingslib.widget.preference.selector.R.layout
                .settingslib_preference_widget_radiobutton
        layoutResource = R.layout.button_navigation_settings_custom_radiobutton
        require(icons.size >= 3) { "Must provide at least 3 icon resources." }
        require(labels.size >= 3) { "Must provide at least 3 label resources." }
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)

        val actualIconIds =
            if (context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                iconIds.reversed()
            } else {
                iconIds
            }

        for (i in 0..2) {
            bindIcon(holder, actualIconIds[i], icons[i], labels[i])
        }

        (holder.findViewById(R.id.order_label) as? TextView)?.text =
            getCompoundLabel(labels, context)
    }

    /** Processes a click on the preference. */
    public override fun onClick() {
        listener?.onRadioButtonClicked(this)
    }

    interface OnClickListener {
        fun onRadioButtonClicked(source: ButtonNavigationSettingsOrderRadioButton)
    }

    private fun bindIcon(holder: PreferenceViewHolder, iconId: Int, iconRes: Int, labelRes: Int) {
        (holder.findViewById(iconId) as? ImageView)?.also {
            it.setImageResource(iconRes)
            it.contentDescription = context.getString(labelRes)
        }
    }

    companion object {
        fun getCompoundLabel(labels: List<Int>, context: Context): String {
            val labelTexts = labels.map { res -> context.getString(res) }.toList()
            return CaseMap.toTitle()
                .wholeString()
                .noLowercase()
                .apply(
                    Locale.getDefault(),
                    /* iter= */ null,
                    LocaleUtils.getConcatenatedString(labelTexts),
                )
        }
    }
}
