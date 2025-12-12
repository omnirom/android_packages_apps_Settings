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

package com.android.settings.applications

import android.content.Context
import android.text.BidiFormatter
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.preference.Preference
import com.android.settings.R.string.install_type_instant
import com.android.settings.Utils
import com.android.settingslib.applications.AppUtils
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.LayoutPreference
import com.android.settingslib.widget.preference.layout.R

class AppInfoHeaderPreference(private val packageInfoProvider: PackageInfoProvider) :
    PreferenceMetadata, PreferenceBinding {

    override val key
        get() = KEY

    override fun createWidget(context: Context): Preference {
        val view = LayoutInflater.from(context).inflate(R.layout.settings_entity_header, null)
        return LayoutPreference(context, view).apply {
            isSelectable = false
            isAllowDividerBelow = true
        }
    }

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)
        val packageInfo = packageInfoProvider.packageInfo ?: return
        val applicationInfo = packageInfo.applicationInfo ?: return

        preference as LayoutPreference
        fun setText(viewId: Int, text: CharSequence?) {
            preference.findViewById<TextView>(viewId).apply {
                setText(text)
                visibility = if (text?.isNotEmpty() == true) View.VISIBLE else View.GONE
            }
        }

        val context = preference.context
        preference
            .findViewById<ImageView>(R.id.entity_header_icon)
            .setImageDrawable(Utils.getBadgedIcon(context, applicationInfo))
        setText(R.id.entity_header_title, applicationInfo.loadLabel(context.packageManager))
        // Wrapped the version name to support RTL
        val summary = BidiFormatter.getInstance().unicodeWrap(packageInfo.versionName)
        setText(R.id.entity_header_summary, summary)
        val installType =
            when (AppUtils.isInstant(applicationInfo)) {
                true -> context.getString(install_type_instant)
                else -> null
            }
        setText(R.id.install_type, installType)
        preference.findViewById<View>(android.R.id.button1).visibility = View.GONE
        preference.findViewById<View>(android.R.id.button2).visibility = View.GONE
    }

    companion object {
        const val KEY = "app_info_header"
    }
}
