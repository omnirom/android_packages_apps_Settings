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

package com.android.settings.accessibility.extradim.ui

import android.content.Context
import android.hardware.display.ColorDisplayManager
import android.provider.Settings
import androidx.preference.Preference
import androidx.preference.SwitchPreferenceCompat
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.testutils.SettingsStoreRule
import com.android.settingslib.datastore.SettingsSecureStore
import com.android.settingslib.metadata.PreferenceChangeReason
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.ReadWritePermit
import com.android.settingslib.preference.PreferenceBindingFactory
import com.android.settingslib.preference.createAndBindWidget
import com.android.settingslib.testutils.shadow.ShadowColorDisplayManager
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadow.api.Shadow

/** Tests for [PersistentAfterRestartsPreference]. */
@RunWith(RobolectricTestRunner::class)
class PersistentAfterRestartsPreferenceTest {
    @get:Rule val settingsStoreRule = SettingsStoreRule()
    private lateinit var context: Context
    private lateinit var shadowColorDisplayManager: ShadowColorDisplayManager
    private lateinit var preference: PersistentAfterRestartsPreference

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preference = PersistentAfterRestartsPreference(context)
        shadowColorDisplayManager =
            Shadow.extract(context.getSystemService(ColorDisplayManager::class.java))
    }

    @Test
    fun getKey_equalsSettingKey() {
        assertThat(preference.key)
            .isEqualTo(Settings.Secure.REDUCE_BRIGHT_COLORS_PERSIST_ACROSS_REBOOTS)
    }

    @Test
    fun getTitle_returnsCorrectStringResId() {
        assertThat(preference.title)
            .isEqualTo(R.string.reduce_bright_colors_persist_preference_title)
    }

    @Test
    fun isIndexable_returnFalse() {
        assertThat(preference.indexable).isFalse()
    }

    @Test
    fun isEnabled_extraDimOn_returnTrue() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true

        assertThat(preference.isEnabled(context)).isTrue()
    }

    @Test
    fun isEnabled_extraDimOff_returnFalse() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false

        assertThat(preference.isEnabled(context)).isFalse()
    }

    @Test
    fun onCreate_extraDimTurnedOff_widgetBecomesDisabled() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, preference)
        preference.onCreate(lifecycleContext)
        assertThat(widget.isEnabled).isTrue()

        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        SettingsSecureStore.get(context).notifyChange(PreferenceChangeReason.STATE)

        assertThat(widget.isEnabled).isFalse()
    }

    @Test
    fun onCreate_extraDimTurnedOn_widgetBecomesEnabled() {
        shadowColorDisplayManager.isReduceBrightColorsActivated = false
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, preference)
        preference.onCreate(lifecycleContext)
        assertThat(widget.isEnabled).isFalse()

        shadowColorDisplayManager.isReduceBrightColorsActivated = true
        SettingsSecureStore.get(context).notifyChange(PreferenceChangeReason.STATE)

        assertThat(widget.isEnabled).isTrue()
    }

    @Test
    fun onDestroy_observerRemoved() {
        val widget = preference.createAndBindWidget<SwitchPreferenceCompat>(context)
        val lifecycleContext = getPreferenceLifecycleContext(widget, preference)

        preference.onCreate(lifecycleContext)
        assertThat(SettingsSecureStore.get(context).hasAnyObserver()).isTrue()

        preference.onDestroy(lifecycleContext)
        assertThat(SettingsSecureStore.get(context).hasAnyObserver()).isFalse()
    }

    @Test
    fun getReadPermissions_returnsSettingsSecureStoreReadPermissions() {
        assertThat(preference.getReadPermissions(context))
            .isEqualTo(SettingsSecureStore.getReadPermissions())
    }

    @Test
    fun getWritePermissions_returnsSettingsSecureStoreWritePermissions() {
        assertThat(preference.getWritePermissions(context))
            .isEqualTo(SettingsSecureStore.getWritePermissions())
    }

    @Test
    fun getReadPermit_returnsAllow() {
        assertThat(preference.getReadPermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    @Test
    fun getWritePermit_returnsAllow() {
        assertThat(preference.getWritePermit(context, 0, 0)).isEqualTo(ReadWritePermit.ALLOW)
    }

    private fun getPreferenceLifecycleContext(
        preference: Preference,
        metadata: PersistentAfterRestartsPreference,
    ): PreferenceLifecycleContext {
        val binding = PreferenceBindingFactory.defaultFactory.getPreferenceBinding(metadata)!!

        return mock {
            on { notifyPreferenceChange(preference.key) }
                .then { binding.bind(preference, metadata) }
        }
    }
}
