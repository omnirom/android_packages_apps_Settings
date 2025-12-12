/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.connecteddevice.display

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.android.settings.R
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.MORE_OPTIONS_KEY
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.TOP_MODE_RES_MAX_COUNT
import com.android.settings.connecteddevice.display.ResolutionPreferenceFragment.TOP_OPTIONS_KEY
import com.android.settingslib.widget.SelectorWithWidgetPreference
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.never
import org.robolectric.Shadows.shadowOf

/** Unit tests for [ResolutionPreferenceFragment]. */
@RunWith(AndroidJUnit4::class)
class ResolutionPreferenceFragmentTest : ExternalDisplayTestBase() {

    @get:Rule
    val activityScenario: ActivityScenarioRule<EmptyFragmentActivity> =
        ActivityScenarioRule(EmptyFragmentActivity::class.java)

    private lateinit var fragment: TestableResolutionPreferenceFragment
    private val metricsLogger: MetricsLogger = mock(MetricsLogger::class.java)

    @Test
    @UiThreadTest
    fun testCreateAndStart_invalidDisplay() {
        initFragment(/* displayId= */ -1)
        mHandler.flush()
        assertThat(fragment.preferenceIdFromResource)
            .isEqualTo(EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE)
        var pref = mPreferenceScreen.findPreference<Preference>(TOP_OPTIONS_KEY)
        assertThat(pref).isNull()
        pref = mPreferenceScreen.findPreference(MORE_OPTIONS_KEY)
        assertThat(pref).isNull()
    }

    @Test
    @UiThreadTest
    fun testModePreferences_modeLimit() {
        initFragment(mDisplays[0].id)
        mHandler.flush()
        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)
        assertThat(topPref).isNotNull()
        val morePref = mPreferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)
        assertThat(morePref).isNotNull()
        assertThat(topPref!!.preferenceCount).isEqualTo(TOP_MODE_RES_MAX_COUNT)
        assertThat(morePref!!.preferenceCount).isEqualTo(2)
    }

    @Test
    @UiThreadTest
    fun testModePreferences_areSortedByResolution() {
        initFragment(mDisplays[0].id)
        mHandler.flush()

        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
        val morePref = mPreferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!

        val allPrefs = mutableListOf<Preference>()
        (0 until topPref.preferenceCount).mapTo(allPrefs) { topPref.getPreference(it) }
        (0 until morePref.preferenceCount).mapTo(allPrefs) { morePref.getPreference(it) }

        // Splits W X H and ensure descending width and descending height
        val resolutionComparator =
            Comparator.comparingInt { pref: Preference ->
                    pref.title.toString().split(" x ")[0].toInt()
                }
                .thenComparingInt { pref: Preference ->
                    pref.title.toString().split(" x ")[1].toInt()
                }
                .reversed()

        assertThat(allPrefs).isInOrder(resolutionComparator)
    }

    @Test
    @UiThreadTest
    fun testOnSameDisplayModeClicked_noModeChange_dialogNotShown() {
        val display = mDisplays[0]
        initFragment(display.id)
        mHandler.flush()
        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
        (topPref.getPreference(0) as SelectorWithWidgetPreference).onClick()
        shadowOf(Looper.getMainLooper()).idle()

        verify(mMockedInjector, never())
            .setUserPreferredDisplayMode(
                display.id,
                display.supportedModes[0],
                /* storeMode= */ false,
            )
        assertThat(
                fragment.parentFragmentManager.findFragmentByTag(ResolutionChangeDialogFragment.TAG)
            )
            .isNull()
    }

    @Test
    @UiThreadTest
    fun testOnModeChange_showsConfirmationDialog() {
        val display = mDisplays[0]
        initFragment(display.id)
        mHandler.flush()
        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
        (topPref.getPreference(1) as SelectorWithWidgetPreference).onClick()
        shadowOf(Looper.getMainLooper()).idle()

        verify(mMockedInjector)
            .setUserPreferredDisplayMode(
                display.id,
                display.supportedModes[1],
                /* storeMode= */ false,
            )
        assertThat(
                fragment.parentFragmentManager.findFragmentByTag(ResolutionChangeDialogFragment.TAG)
            )
            .isNotNull()
    }

    @Test
    @UiThreadTest
    fun testOnDialogConfirmed_storeUserPreferredDisplayMode() {
        val display = mDisplays[0]
        val displayId = display.id
        initFragment(displayId)
        mHandler.flush()
        val newMode = display.supportedModes[1]

        val bundle = Bundle()
        bundle.putBoolean(ResolutionChangeDialogFragment.KEY_CONFIRMED, true)
        bundle.putParcelable(ResolutionChangeDialogFragment.KEY_NEW_MODE, newMode)
        fragment.setFragmentResult(ResolutionChangeDialogFragment.KEY_RESULT, bundle)
        shadowOf(Looper.getMainLooper()).idle()

        verify(mMockedInjector)
            .setUserPreferredDisplayMode(displayId, newMode, /* storeMode= */ true)
    }

    @Test
    @UiThreadTest
    fun testOnDialogCancelled_resetModeAndUi() {
        val display = mDisplays[0]
        val displayId = display.id
        initFragment(displayId)
        mHandler.flush()
        val existingMode = display.supportedModes[0]

        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
        (topPref.getPreference(1) as SelectorWithWidgetPreference).onClick()
        shadowOf(Looper.getMainLooper()).idle()

        val bundle = Bundle()
        bundle.putBoolean(ResolutionChangeDialogFragment.KEY_CONFIRMED, false)
        bundle.putParcelable(ResolutionChangeDialogFragment.KEY_EXISTING_MODE, existingMode)
        fragment.setFragmentResult(ResolutionChangeDialogFragment.KEY_RESULT, bundle)
        shadowOf(Looper.getMainLooper()).idle()

        assertThat((topPref.getPreference(0) as SelectorWithWidgetPreference).isChecked).isTrue()
        verify(mMockedInjector).resetUserPreferredDisplayMode(displayId)
    }

    @Test
    @UiThreadTest
    fun testDisplayRemoved_activityIsFinishing() {
        val displayId = mDisplays[0].id
        initFragment(displayId)
        mHandler.flush()

        `when`(mMockedInjector.getDisplay(displayId)).thenReturn(null)
        mListener.update(displayId)
        mHandler.flush()

        activityScenario.scenario.onActivity { activity: EmptyFragmentActivity ->
            assertThat(activity.isFinishing).isTrue()
        }
    }

    @Test
    fun testModeSetFiltering() {
        val displayId = mDisplays[0].id
        initFragment(displayId)
        fragment.setupModeFiltering()
        fragment.onCreateCallback(null)
        mHandler.flush()

        mListener.update(displayId)
        mHandler.flush()

        val topPref = mPreferenceScreen.findPreference<PreferenceCategory>(TOP_OPTIONS_KEY)!!
        val morePref = mPreferenceScreen.findPreference<PreferenceCategory>(MORE_OPTIONS_KEY)!!
        val allPrefs = mutableListOf<Preference>()
        (0 until topPref.preferenceCount).mapTo(allPrefs) { topPref.getPreference(it) }
        (0 until morePref.preferenceCount).mapTo(allPrefs) { morePref.getPreference(it) }
        assertThat(allPrefs.map { it.toString() })
            .containsExactly("1920 x 1080", "800 x 600", "760 x 600")
            .inOrder()
    }

    private fun initFragment(displayId: Int) {
        if (::fragment.isInitialized) {
            fragment.clearModeFiltering()
            fragment.onCreateCallback(null)
            return
        }
        fragment =
            TestableResolutionPreferenceFragment(
                displayId,
                mPreferenceScreen,
                mContext,
                mMockedInjector,
                metricsLogger,
            )
        activityScenario.scenario.onActivity { activity: EmptyFragmentActivity ->
            // Dialog needs AppCompat theme set
            activity.setTheme(androidx.appcompat.R.style.Theme_AppCompat)
            activity.supportFragmentManager.beginTransaction().add(fragment, "tag").commitNow()
        }
        fragment.onCreateCallback(null)
        fragment.onActivityCreatedCallback(null)
        fragment.onStartCallback()
    }

    class TestableResolutionPreferenceFragment(
        private val displayId: Int,
        private val preferenceScreen: PreferenceScreen,
        context: Context,
        injector: ConnectedDisplayInjector,
        logger: MetricsLogger,
    ) : ResolutionPreferenceFragment(injector) {
        private val mockRootView: View
        private val emptyView: TextView
        private val mockResources: Resources = mock(Resources::class.java)
        private val logger: MetricsLogger
        var preferenceIdFromResource: Int = -1
            private set

        init {
            doReturn(61)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakRefreshRate)
            doReturn(1920)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakWidth)
            doReturn(1080)
                .`when`(mockResources)
                .getInteger(com.android.internal.R.integer.config_externalDisplayPeakHeight)
            doReturn(true)
                .`when`(mockResources)
                .getBoolean(com.android.internal.R.bool.config_refreshRateSynchronizationEnabled)
            mockRootView = mock(View::class.java)
            emptyView = TextView(context)
            doReturn(emptyView).`when`(mockRootView).findViewById<View>(android.R.id.empty)
            this.logger = logger
        }

        override fun getPreferenceScreen(): PreferenceScreen {
            return preferenceScreen
        }

        override fun getView(): View {
            return mockRootView
        }

        override fun setEmptyView(view: View?) {
            assertThat(view).isEqualTo(emptyView)
        }

        override fun getEmptyView(): View {
            return emptyView
        }

        override fun addPreferencesFromResource(resource: Int) {
            preferenceIdFromResource = resource
        }

        override fun getDisplayIdArg(): Int {
            return displayId
        }

        override fun writePreferenceClickMetric(preference: Preference) {
            logger.writePreferenceClickMetric(preference)
        }

        override fun getResources(context: Context): Resources {
            return mockResources
        }

        fun setupModeFiltering() {
            doReturn(intArrayOf(1920, 1080, 800, 600))
                .`when`(mockResources)
                .getIntArray(R.array.config_resolutionsShownOnExternalDisplay)
        }

        fun clearModeFiltering() {
            doReturn(null)
                .`when`(mockResources)
                .getIntArray(R.array.config_resolutionsShownOnExternalDisplay)
        }
    }

    /** Interface allowing to mock and spy on log events. */
    interface MetricsLogger {
        /** On preference click metric */
        fun writePreferenceClickMetric(preference: Preference)
    }
}
