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

package com.android.settings.dream

import android.app.settings.SettingsEnums
import android.content.Context
import android.provider.Settings.Secure.SCREENSAVER_COMPONENTS
import android.provider.Settings.Secure.SCREENSAVER_ENABLED
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import com.android.internal.R.bool.config_dreamsDisabledByAmbientModeSuppressionConfig
import com.android.settings.R
import com.android.settings.Settings.DreamSettingsActivity
import com.android.settings.Utils
import com.android.settings.core.PreferenceScreenMixin
import com.android.settings.display.AmbientDisplayAlwaysOnPreferenceController
import com.android.settings.flags.Flags
import com.android.settings.utils.makeLaunchIntent
import com.android.settingslib.datastore.AbstractKeyedDataObservable
import com.android.settingslib.datastore.HandlerExecutor
import com.android.settingslib.datastore.KeyValueStore
import com.android.settingslib.datastore.KeyedObserver
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.dream.DreamBackend
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.metadata.PreferenceSummaryProvider
import com.android.settingslib.metadata.ProvidePreferenceScreen
import com.android.settingslib.metadata.preferenceHierarchy
import kotlinx.coroutines.CoroutineScope

// LINT.IfChange
@ProvidePreferenceScreen(ScreensaverScreen.KEY)
open class ScreensaverScreen(private val context: Context) :
    PreferenceScreenMixin,
    AbstractKeyedDataObservable<String>(),
    PreferenceAvailabilityProvider,
    PreferenceSummaryProvider {

    private var dreamBackend: DreamBackend = DreamBackend.getInstance(context)
    private var settingsStore: KeyValueStore = SettingsSecureStore.get(context)

    private val observer =
        KeyedObserver<String> { _, _ -> notifyChange(KEY, PreferenceChangeReason.STATE) }

    private var ambientModeSuppressionProvider: AmbientModeSuppressionProvider =
        object : AmbientModeSuppressionProvider {
            override fun isSuppressedByBedtime(context: Context) =
                AmbientDisplayAlwaysOnPreferenceController.isAodSuppressedByBedtime(context)
        }

    private var summaryStringsProvider: SummaryStringsProvider =
        object : SummaryStringsProvider {
            override fun dreamOff(context: Context) =
                context.resources.getString(R.string.screensaver_settings_summary_off)

            override fun dreamOn(context: Context, activeDreamName: CharSequence) =
                context.resources.getString(
                    R.string.screensaver_settings_summary_on,
                    activeDreamName,
                )

            override fun dreamOffBedtime(context: Context) =
                context.resources.getString(R.string.screensaver_unavailable_due_to_mode)
        }

    private val screenSaverSettingKeys
        get() = listOf(SCREENSAVER_ENABLED, SCREENSAVER_COMPONENTS)

    override val key: String
        get() = KEY

    override val title: Int
        get() = R.string.screensaver_settings_title

    override val highlightMenuKey: Int
        get() = R.string.menu_key_display

    override fun getMetricsCategory() = SettingsEnums.DREAM

    override fun isFlagEnabled(context: Context) = Flags.catalystScreensaver()

    override fun hasCompleteHierarchy() = false

    override fun fragmentClass(): Class<out Fragment>? = DreamSettings::class.java

    override fun getLaunchIntent(context: Context, metadata: PreferenceMetadata?) =
        makeLaunchIntent(context, DreamSettingsActivity::class.java, metadata?.key)

    override fun getPreferenceHierarchy(context: Context, coroutineScope: CoroutineScope) =
        preferenceHierarchy(context) {}

    override fun onFirstObserverAdded() {
        // update summary when any of the screen saver settings has changed
        for (key in screenSaverSettingKeys) {
            settingsStore.addObserver(key, observer, HandlerExecutor.main)
        }
    }

    override fun onLastObserverRemoved() {
        for (key in screenSaverSettingKeys) {
            settingsStore.removeObserver(key, observer)
        }
    }

    override fun getSummary(context: Context): CharSequence {
        val dreamsDisabledByAmbientModeSuppression =
            context.resources.getBoolean(config_dreamsDisabledByAmbientModeSuppressionConfig)
        return if (
            dreamsDisabledByAmbientModeSuppression &&
                ambientModeSuppressionProvider.isSuppressedByBedtime(context)
        ) {
            summaryStringsProvider.dreamOffBedtime(context)
        } else {
            getSummaryTextWithDreamName(context)
        }
    }

    override val keywords: Int
        get() = R.string.keywords_screensaver

    override fun isAvailable(context: Context) = Utils.areDreamsAvailableToCurrentUser(context)

    @VisibleForTesting
    fun setDreamBackend(backend: DreamBackend) {
        this.dreamBackend = backend
    }

    @VisibleForTesting
    fun setScreensaverStore(settingsStore: KeyValueStore) {
        this.settingsStore = settingsStore
    }

    @VisibleForTesting
    fun setAmbientModeSuppressionProvider(provider: AmbientModeSuppressionProvider) {
        ambientModeSuppressionProvider = provider
    }

    @VisibleForTesting
    fun setSummaryStringsProvider(provider: SummaryStringsProvider) {
        summaryStringsProvider = provider
    }

    private fun getSummaryTextWithDreamName(context: Context): CharSequence {
        return if (dreamBackend.isEnabled) {
            // In practice, activeDreamName would not be null, but it can be in tests and in that
            // case it needs to have the same behavior as the corresponding method in
            // DreamSettings (see b/411793272).
            summaryStringsProvider.dreamOn(context, dreamBackend.activeDreamName ?: "null")
        } else {
            summaryStringsProvider.dreamOff(context)
        }
    }

    interface AmbientModeSuppressionProvider {
        fun isSuppressedByBedtime(context: Context): Boolean
    }

    interface SummaryStringsProvider {
        fun dreamOff(context: Context): CharSequence

        fun dreamOn(context: Context, activeDreamName: CharSequence): CharSequence

        fun dreamOffBedtime(context: Context): CharSequence
    }

    companion object {
        const val KEY = "screensaver"
    }
}
// LINT.ThenChange(DreamSettings.java)
