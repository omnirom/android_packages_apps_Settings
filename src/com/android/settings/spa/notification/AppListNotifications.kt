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

package com.android.settings.spa.notification

import android.os.Bundle
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.android.settings.R
import com.android.settingslib.spa.framework.common.SettingsPageProvider
import com.android.settingslib.spa.framework.compose.navigator
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spaprivileged.template.app.AppList
import com.android.settingslib.spaprivileged.template.app.AppListInput
import com.android.settingslib.spaprivileged.template.app.AppListPage

sealed class AppListNotificationsPageProvider(private val type: ListType) : SettingsPageProvider {
    @Composable
    override fun Page(arguments: Bundle?) {
        NotificationsAppListPage(type)
    }

    object AllApps : AppListNotificationsPageProvider(ListType.Apps) {
        override val name = "AppListNotifications"

        @Composable
        fun EntryItem() {
            val summary = stringResource(R.string.app_notification_field_summary)
            Preference(object : PreferenceModel {
                override val title = stringResource(ListType.Apps.titleResource)
                override val summary = { summary }
                override val onClick = navigator(name)
            })
        }
    }

    object ExcludeSummarization : AppListNotificationsPageProvider(ListType.ExcludeSummarization) {
        override val name = "NotificationsExcludeSummarization"
    }

    object ExcludeClassification : AppListNotificationsPageProvider(ListType.ExcludeClassification) {
        override val name = "NotificationsExcludeClassification"
    }
}

@Composable
fun NotificationsAppListPage(
    type: ListType,
    appList: @Composable AppListInput<AppNotificationsRecord>.() -> Unit = { AppList() }
) {
    val context = LocalContext.current
    AppListPage(
        title = stringResource(type.titleResource),
        listModel = remember(context) { AppNotificationsListModel(context, type) },
        appList = appList,
    )
}

sealed class ListType(
    @StringRes val titleResource: Int
) {
    object Apps : ListType(
        titleResource = R.string.app_notifications_title,
    )
    object ExcludeSummarization : ListType(
        titleResource = R.string.notification_summarization_manage_excluded_apps_title,
    )
    object ExcludeClassification : ListType(
        titleResource = R.string.notification_bundle_manage_excluded_apps_title,
    )
}