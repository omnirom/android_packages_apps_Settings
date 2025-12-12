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
package com.android.settings.deviceinfo.legal

import android.app.settings.SettingsEnums
import android.content.Context
import androidx.fragment.app.Fragment
import com.android.settings.LegalSettings
import com.android.settings.R
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.flags.Flags
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

@ProvidePreferenceScreen(LegalSettingsScreen.KEY)
open class LegalSettingsScreen : PreferenceScreenMixin {
    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.legal_information

    override val highlightMenuKey: Int
        get() = R.string.menu_key_about_device

    override fun getMetricsCategory() = SettingsEnums.ABOUT_LEGAL_SETTINGS

    override fun isFlagEnabled(context: Context) = Flags.catalystLegalInformation()

    override fun fragmentClass(): Class<out Fragment>? = LegalSettings::class.java

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            +LegalPreference("copyright", R.string.copyright_title, "android.settings.COPYRIGHT")
            +LegalPreference("license", R.string.license_title, "android.settings.LICENSE")
            +LegalPreference("terms", R.string.terms_title, "android.settings.TERMS")
            +ModuleLicensesScreen.KEY // Use screen key in case it is overlaid.
            +LegalPreference(
                "webview_license",
                R.string.webview_license_title,
                "android.settings.WEBVIEW_LICENSE",
            )
            +WallpaperAttributionsPreference()
        }

    companion object {
        const val KEY = "legal_information"
    }
}
