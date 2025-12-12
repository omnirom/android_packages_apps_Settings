/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.spa.system

import android.os.Bundle
import android.util.FeatureFlagUtils
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.common.SpaEnvironmentFactory
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.framework.compose.rememberContext
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.ui.SettingsBody
import com.android.settingslib.spa.widget.ui.SettingsIntro
import com.android.settingslib.spaprivileged.template.app.AppListPage

object AppLanguagesPageProvider : SettingsPageProvider {
    override val name = "AppLanguages"

    @Composable
    override fun Page(arguments: Bundle?) {
        AppListPage(
            title = stringResource(R.string.app_locales_picker_menu_title),
            listModel = rememberContext(::AppLanguagesListModel),
            noMoreOptions = true,
            header = {
                Box(Modifier.padding(vertical = SettingsDimension.itemPaddingVertical)) {
                    // if the feature is in opt-out mode, show the disclaimer.
                    val titleId =
                        if (FeatureFlagUtils.isEnabled(
                                SpaEnvironmentFactory.instance.appContext,
                                FeatureFlagUtils.SETTINGS_APP_LOCALE_OPT_IN_ENABLED
                            )
                        ) {
                            R.string.desc_app_locale_selection_supported
                        } else {
                            R.string.desc_app_locale_selection_supported_with_disclaimer
                        }
                    SettingsIntro(stringResource(titleId))
                }
            },
        )
    }

    @Composable
    fun EntryItem() {
        val summary = stringResource(R.string.app_locale_picker_summary)
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.app_locales_picker_menu_title)
            override val summary = { summary }
            override val onClick = navigator(name)
        })
    }
}
