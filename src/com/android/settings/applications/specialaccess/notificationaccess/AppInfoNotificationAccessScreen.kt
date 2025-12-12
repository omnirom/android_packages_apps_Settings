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

package com.android.settings.applications.specialaccess.notificationaccess

import android.app.NotificationManager
import android.app.settings.SettingsEnums
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS
import android.provider.Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ALERTING
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_CONVERSATIONS
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_ONGOING
import android.service.notification.NotificationListenerService.FLAG_FILTER_TYPE_SILENT
import com.android.settings.R
import com.android.settings.contract.TAG_DEVICE_STATE_PREFERENCE
import com.android.settings.contract.TAG_DEVICE_STATE_SCREEN
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.notification.NotificationBackend
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.NoOpKeyedObservable
import com.android.settingslib.metadata.BooleanValuePreference
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.PreferenceTitleProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import com.android.settingslib.preference.SwitchPreferenceBinding
import com.android.settingslib.widget.MainSwitchPreferenceBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** "Apps" -> "Special app access" -> "Notification read, reply & control" -> {app name} */
@ProvidePreferenceScreen(AppInfoNotificationAccessScreen.KEY, parameterized = true)
open class AppInfoNotificationAccessScreen(context: Context, override val arguments: Bundle) :
    PreferenceScreenMixin, PreferenceSummaryProvider, PreferenceTitleProvider {

    private val packageName = arguments.getString("app")!!

    private val serviceName = arguments.getString("serviceName")!!

    private val appInfo = context.packageManager.getApplicationInfo(packageName, 0)

    private val storage: KeyValueStore =
        NotificationAccessStorage(context, packageName, serviceName)

    override val key: String
        get() = KEY

    override val screenTitle: Int
        get() = R.string.manage_notification_access_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_apps

    override fun getMetricsCategory() = SettingsEnums.NOTIFICATION_ACCESS_DETAIL

    override fun tags(context: Context) =
        arrayOf(TAG_DEVICE_STATE_SCREEN, TAG_DEVICE_STATE_PREFERENCE)

    override fun getTitle(context: Context): CharSequence? =
        appInfo.loadLabel(context.packageManager)

    override fun getSummary(context: Context): CharSequence? =
        context.getString(
            when (storage.getBoolean(NotificationAccessApprovalPreference.KEY)) {
                true -> R.string.notification_listener_allowed
                else -> R.string.notification_listener_not_allowed
            }
        )

    override fun isFlagEnabled(context: Context) = false

    override fun extras(context: Context): Bundle? =
        Bundle(1).apply { putString(KEY_EXTRA_PACKAGE_NAME, packageName) }

    override fun hasCompleteHierarchy() = false

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        Intent(ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS).apply {
            putExtra(EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, packageName)
        }

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {
            // Main switch preference
            +NotificationAccessApprovalPreference(storage)
            // Filter type preferences
            +NotificationAccessOngoingPreference(storage)
            +NotificationAccessConversationsPreference(storage)
            +NotificationAccessAlertingPreference(storage)
            +NotificationAccessSilentPreference(storage)
        }

    companion object {
        const val KEY = "device_state_app_info_notification_access"

        const val KEY_EXTRA_PACKAGE_NAME = "package_name"

        @JvmStatic
        fun parameters(context: Context): Flow<Bundle> = flow {
            val services = AppsNotificationAccessScreen.loadNotificationListenerServices(context)
            for (service in services) {
                emit(
                    Bundle(1).apply {
                        putString("app", service.packageName)
                        putString("serviceName", service.name)
                    }
                )
            }
        }
    }
}

/**
 * Notification access main switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/ApprovalPreferenceController.java
 */
class NotificationAccessApprovalPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, MainSwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.notification_access_detail_switch

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_notification_access_approval_preference"
    }
}

/**
 * Notification access "Real-time" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/OngoingTypeFilterPreferenceController.java
 */
class NotificationAccessOngoingPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.notif_type_ongoing

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_notification_access_ongoing_preference"
    }
}

/**
 * Notification access "Conversations" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/ConversationTypeFilterPreferenceController.java
 */
class NotificationAccessConversationsPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.notif_type_conversation

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_notification_access_conversations_preference"
    }
}

/**
 * Notification access "Notifications" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/AlertingTypeFilterPreferenceController.java
 */
class NotificationAccessAlertingPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.notif_type_alerting

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_notification_access_alerting_preference"
    }
}

/**
 * Notification access "Silent" switch.
 *
 * Current implementation see:
 * https://source.corp.google.com/h/googleplex-android/platform/superproject/main/+/main:packages/apps/Settings/src/com/android/settings/applications/specialaccess/notificationaccess/SilentTypeFilterPreferenceController.java
 */
class NotificationAccessSilentPreference(private val storage: KeyValueStore) :
    BooleanValuePreference, SwitchPreferenceBinding {

    override val key
        get() = KEY

    override val title
        get() = R.string.notif_type_silent

    override fun storage(context: Context) = storage

    companion object {
        const val KEY = "device_state_notification_access_silent_preference"
    }
}

private class NotificationAccessStorage(
    private val context: Context,
    private val packageName: String,
    private val serviceName: String,
) : NoOpKeyedObservable<String>(), KeyValueStore {

    override fun contains(key: String): Boolean =
        when (key) {
            NotificationAccessApprovalPreference.KEY,
            NotificationAccessOngoingPreference.KEY,
            NotificationAccessConversationsPreference.KEY,
            NotificationAccessAlertingPreference.KEY,
            NotificationAccessSilentPreference.KEY -> true
            else -> false
        }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getValue(key: String, valueType: Class<T>): T =
        when (key) {
            NotificationAccessApprovalPreference.KEY ->
                notificationAccessApproval(context, packageName, serviceName)
            NotificationAccessOngoingPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_ONGOING,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessConversationsPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_CONVERSATIONS,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessAlertingPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_ALERTING,
                    context,
                    packageName,
                    serviceName,
                )
            NotificationAccessSilentPreference.KEY ->
                notificationAccessTypeFilter(
                    FLAG_FILTER_TYPE_SILENT,
                    context,
                    packageName,
                    serviceName,
                )
            else -> throw IllegalArgumentException("Unknown key: $key")
        }
            as T

    override fun <T : Any> setValue(key: String, valueType: Class<T>, value: T?) {}

    companion object {
        fun notificationAccessApproval(
            context: Context,
            packageName: String,
            serviceName: String,
        ): Boolean {
            val componentName = ComponentName(packageName, serviceName)
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            return notificationManager.isNotificationListenerAccessGranted(componentName)
        }

        fun notificationAccessTypeFilter(
            type: Int,
            context: Context,
            packageName: String,
            serviceName: String,
        ): Boolean {
            val componentName = ComponentName(packageName, serviceName)
            val notificationBackend = NotificationBackend()
            val listenerFilter =
                notificationBackend.getListenerFilter(componentName, context.userId)
            return isFlagSet(listenerFilter.types, type)
        }

        private fun isFlagSet(flagData: Int, flag: Int) = (flagData and flag) != 0
    }
}
