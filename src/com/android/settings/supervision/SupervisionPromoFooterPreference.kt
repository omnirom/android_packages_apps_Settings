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
package com.android.settings.supervision

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.widget.TextView
import androidx.core.net.toUri
import androidx.preference.Preference
import androidx.preference.PreferenceViewHolder
import com.android.settings.R
import com.android.settings.supervision.ipc.PreferenceData
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import com.android.settingslib.widget.CardPreference
import com.android.settingslib.widget.mainswitch.R as MainSwitchPreferenceR
import com.android.settingslib.widget.preference.card.R as CardPreferenceR
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A bottom banner promoting other supervision features offered by the supervision app. */
class SupervisionPromoFooterPreference(
    private val preferenceDataProvider: PreferenceDataProvider,
    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PreferenceMetadata, PreferenceBinding, PreferenceLifecycleProvider {

    /** Whether [preferenceData] holds an initialized value. */
    private var initialized = false

    private var preferenceData: PreferenceData? = null

    override val key: String
        get() = KEY

    // Non indexable as the metadata (title, summary, etc.) is provided by another app with IPC
    override val indexable
        get() = false

    override fun createWidget(context: Context): Preference = FooterPreference(context)

    override fun bind(preference: Preference, metadata: PreferenceMetadata) {
        super.bind(preference, metadata)

        var intent: Intent? = null
        if (initialized) {
            val context = preference.context
            val targetIntent =
                Intent(preferenceData?.action, preferenceData?.intentData?.toUri()).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            intent = if (targetIntent.isValid(context)) targetIntent else null

            val leadingIconResId = preferenceData?.icon
            val leadingIcon =
                leadingIconResId?.let {
                    val resourcePackage = preferenceDataProvider.packageName
                    val icon = Icon.createWithResource(resourcePackage, it)
                    icon.loadDrawable(context)
                }

            preference.intent = intent
            preference.title = preferenceData?.title ?: preference.title
            preference.summary = preferenceData?.summary ?: preference.summary
            preference.icon = leadingIcon ?: preference.icon
            val trailingIcon =
                preferenceData?.trailingIcon?.let {
                    val resourcePackage = preferenceDataProvider.packageName
                    Icon.createWithResource(resourcePackage, it).loadDrawable(context)
                }
            (preference as CardPreference).setAdditionalAction(
                trailingIcon,
                contentDescription = null,
                action = null,
            )
        }

        // Icon, Title, Summary may be null but at least one of title or summary must be valid
        // and the action has to be valid for the preference to be visible.
        preference.isVisible =
            intent != null && (preferenceData?.title != null || preferenceData?.summary != null)
    }

    override fun onResume(context: PreferenceLifecycleContext) {
        context.lifecycleScope.launch {
            // Immediately update the UI with cached data
            preferenceData = preferenceDataProvider.getCachedPreferenceData(listOf(KEY))[KEY]
            if (preferenceData != null) {
                initialized = true
            }
            context.notifyPreferenceChange(KEY)

            // Asynchronously fetch fresh data and update the UI
            val freshPreferenceData =
                withContext(coroutineDispatcher) {
                    preferenceDataProvider.getPreferenceData(listOf(KEY))[KEY]
                }
            if (preferenceData != freshPreferenceData) {
                preferenceData = freshPreferenceData
                initialized = true
                context.notifyPreferenceChange(KEY)
            }
        }
    }

    private fun Intent.isValid(context: Context) =
        context.packageManager.queryIntentActivitiesAsUser(this, 0, context.userId).isNotEmpty()

    companion object {
        const val KEY = "promo_footer"
    }
}

private class FooterPreference(context: Context) : CardPreference(context) {
    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        // Make colors consistent with main switch preference
        holder.findViewById(CardPreferenceR.id.card_container)?.background =
            context.getDrawable(MainSwitchPreferenceR.drawable.settingslib_expressive_switch_bar_bg)
        val textColor =
            context.resources.getColor(
                MainSwitchPreferenceR.color.settingslib_main_switch_text_color,
                context.theme,
            )
        (holder.findViewById(android.R.id.title) as? TextView)?.setTextColor(textColor)
        (holder.findViewById(android.R.id.summary) as? TextView)?.setTextColor(textColor)
    }
}
