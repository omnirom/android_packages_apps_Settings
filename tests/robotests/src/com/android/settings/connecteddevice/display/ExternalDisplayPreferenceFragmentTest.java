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
package com.android.settings.connecteddevice.display;

import static android.view.Display.INVALID_DISPLAY;

import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.PrefBasics;
import com.android.settingslib.widget.MainSwitchPreference;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

/** Unit tests for {@link ExternalDisplayPreferenceFragment}.  */
@RunWith(AndroidJUnit4.class)
public class ExternalDisplayPreferenceFragmentTest extends ExternalDisplayTestBase {
    @Nullable
    private ExternalDisplayPreferenceFragment mFragment;
    private int mPreferenceIdFromResource;
    private boolean mLaunchedBuiltinSettings;
    private int mResolutionSelectorDisplayId = INVALID_DISPLAY;
    @Mock
    private MetricsLogger mMockedMetricsLogger;

    @Test
    @UiThreadTest
    public void testCreateAndStart() {
        initFragment();
        assertThat(mPreferenceIdFromResource).isEqualTo(EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    private PreferenceCategory getExternalDisplayCategory(int positionIndex) {
        return mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_LIST.keyForNth(positionIndex));
    }

    private void assertDisplayListCount(int expectedCount) {
        int actualCount = 0;
        for (int i = 0; i < mPreferenceScreen.getPreferenceCount(); i++) {
            Preference child = mPreferenceScreen.getPreference(i);
            if (child.getKey().startsWith(PrefBasics.EXTERNAL_DISPLAY_LIST.key)) {
                actualCount++;
            }
        }
        assertThat(actualCount).isEqualTo(expectedCount);
    }

    @Test
    @UiThreadTest
    public void testShowDisplayList() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, false);

        var fragment = initFragment();
        var outState = new Bundle();
        assertThat(mHandler.getPendingMessages().size()).isEqualTo(1);

        assertDisplayListCount(0);

        verify(mMockedInjector, never()).getConnectedDisplays();
        mHandler.flush();
        assertThat(mHandler.getPendingMessages().size()).isEqualTo(0);
        verify(mMockedInjector).getConnectedDisplays();
        assertDisplayListCount(2);

        Preference pref = mPreferenceScreen.findPreference(PrefBasics.DISPLAY_TOPOLOGY.key);
        assertThat(pref).isNull();

        pref = mPreferenceScreen.findPreference(PrefBasics.BUILTIN_DISPLAY_LIST.key);
        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    public void testShowDisplayListWithPane_OneExternalDisplay() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);

        initFragment();
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getConnectedDisplays();
        mHandler.flush();

        var pref = mPreferenceScreen.findPreference(PrefBasics.DISPLAY_TOPOLOGY.key);
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(PrefBasics.MIRROR.key);
        assertThat(pref).isNotNull();

        assertDisplayListCount(1);
        assertThat("" + getExternalDisplayCategory(0).getTitle()).isEqualTo("HDMI");

        PreferenceCategory listPref =
                mPreferenceScreen.findPreference(PrefBasics.BUILTIN_DISPLAY_LIST.key);
        var builtinPref = listPref.getPreference(0);
        assertThat(builtinPref.getOnPreferenceClickListener().onPreferenceClick(builtinPref))
                .isTrue();
        assertThat(mLaunchedBuiltinSettings).isTrue();
    }

    @Test
    @UiThreadTest
    public void testDontShowDisplayListOrPane_NoExternalDisplays() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);

        initFragment();
        doReturn(List.of()).when(mMockedInjector).getConnectedDisplays();
        mHandler.flush();

        // When no external display is attached, interactive preferences are omitted.
        var pref = mPreferenceScreen.findPreference(PrefBasics.DISPLAY_TOPOLOGY.key);
        assertThat(pref).isNull();
        pref = mPreferenceScreen.findPreference(PrefBasics.MIRROR.key);
        assertThat(pref).isNull();

        assertDisplayListCount(0);

        var listPref = mPreferenceScreen.findPreference(PrefBasics.BUILTIN_DISPLAY_LIST.key);
        assertThat(listPref).isNull();
    }

    @Test
    @UiThreadTest
    public void testShowDisplayControlsDisabled() {
        doReturn(List.of(
                        createExternalDisplay(DisplayIsEnabled.NO),
                        createOverlayDisplay(DisplayIsEnabled.YES)))
                .when(mMockedInjector).getConnectedDisplays();
        initFragment();
        mHandler.flush();

        assertDisplayListCount(2);
        Preference pref;
        for (int disp = 0; disp < 2; disp++) {
            pref = mPreferenceScreen.findPreference(
                    PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(disp));
            assertWithMessage("resolution " + disp).that(pref.isEnabled()).isEqualTo(disp == 1);

            pref = mPreferenceScreen.findPreference(
                    PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(disp));
            assertWithMessage("rotation " + disp).that(pref.isEnabled()).isEqualTo(disp == 1);

            pref = mPreferenceScreen.findPreference(
                    PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(disp));
            assertWithMessage("size " + disp).that(pref.isEnabled()).isEqualTo(disp == 1);
        }
    }

    @Test
    @UiThreadTest
    public void testLaunchDisplaySettingFromList() {
        initFragment();
        mHandler.flush();
        assertDisplayListCount(2);
        var display1Category = getExternalDisplayCategory(0);
        var display2Category = getExternalDisplayCategory(1);
        assertThat("" + display1Category.getTitle()).isEqualTo("HDMI");
        var display1Resolution = display1Category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        display1Resolution.performClick();
        assertThat(mResolutionSelectorDisplayId).isEqualTo(1);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(display1Resolution);
        var display2Resolution = display2Category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(1));
        assertThat("" + display2Category.getTitle()).isEqualTo("Overlay #1");
        assertThat("" + display2Resolution.getSummary()).isEqualTo("1240 x 780");
        display2Resolution.performClick();
        assertThat(mResolutionSelectorDisplayId).isEqualTo(2);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(display2Resolution);
    }

    @Test
    @UiThreadTest
    public void testShowDisplayListForOnlyOneDisplay_PreviouslyShownList() {
        var fragment = initFragment();
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getConnectedDisplays();
        mHandler.flush();
        int attachedId = mDisplays.get(0).getId();
        assertDisplayListCount(1);
        assertThat("" + getExternalDisplayCategory(0).getTitle()).isEqualTo("HDMI");
    }

    @Test
    @UiThreadTest
    public void testShowEnabledDisplay_OnlyOneDisplayAvailable_displaySizeDisabled() {
        mFlags.setFlag(FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING, false);
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getConnectedDisplays();
        // Init
        initFragment();
        mHandler.flush();
        assertDisplayListCount(1);
        var category = getExternalDisplayCategory(0);
        var pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat(pref).isNotNull();
        var footerPref = category.findPreference(PrefBasics.FOOTER.key);
        assertThat(footerPref).isNotNull();
        var sizePref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat(sizePref).isNull();
        assertThat("" + footerPref.getTitle())
                .isEqualTo(getText(EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE));
    }

    @Test
    @UiThreadTest
    public void testShowEnabledDisplay_OnlyOneDisplayAvailable() {
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getConnectedDisplays();
        // Init
        initFragment();
        mHandler.flush();
        assertDisplayListCount(1);
        var category = getExternalDisplayCategory(0);
        assertThat("" + category.getTitle()).isEqualTo("HDMI");
        var footerPref = category.findPreference(PrefBasics.FOOTER.key);
        assertThat(footerPref).isNotNull();
        var sizePref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat(sizePref).isNotNull();
        assertThat("" + footerPref.getTitle())
                .isEqualTo(getText(EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE));
    }

    @Test
    @UiThreadTest
    public void testShowOneEnabledDisplay_FewAvailable() {
        initFragment();
        verify(mMockedInjector, never()).getConnectedDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getConnectedDisplays();
        var pref = mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat(pref).isNotNull();
        var footerPref = mPreferenceScreen.findPreference(PrefBasics.FOOTER.key);
        // No footer for showing multiple displays.
        assertThat(footerPref).isNull();
        var sizePref = mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat(sizePref).isNotNull();
    }

    @Test
    @UiThreadTest
    public void testShowDisabledDisplay() {
        initFragment();
        var disabledDisplays = List.of(
                createExternalDisplay(DisplayIsEnabled.NO),
                createOverlayDisplay(DisplayIsEnabled.NO));
        doReturn(disabledDisplays).when(mMockedInjector).getConnectedDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getConnectedDisplays();
        var category = getExternalDisplayCategory(0);
        var mainPref = (MainSwitchPreference) category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(0));
        assertThat(mainPref).isNotNull();
        assertThat("" + mainPref.getTitle()).isEqualTo(
                getText(PrefBasics.EXTERNAL_DISPLAY_USE.titleResource));
        assertThat(mainPref.isChecked()).isFalse();
        assertThat(mainPref.isEnabled()).isTrue();
        assertThat(mainPref.getOnPreferenceChangeListener()).isNotNull();
        var pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat(pref.isEnabled()).isFalse();
        pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat(pref.isEnabled()).isFalse();
        var footerPref = category.findPreference(PrefBasics.FOOTER.key);
        assertThat(footerPref).isNull();
        var sizePref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat(sizePref.isEnabled()).isFalse();
    }

    @Test
    @UiThreadTest
    public void testNoDisplays() {
        doReturn(List.of()).when(mMockedInjector).getConnectedDisplays();
        initFragment();
        mHandler.flush();
        var mainPref = (MainSwitchPreference) mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(0));
        assertThat(mainPref).isNotNull();
        assertThat("" + mainPref.getTitle()).isEqualTo(
                getText(PrefBasics.EXTERNAL_DISPLAY_USE.titleResource));
        assertThat(mainPref.isChecked()).isFalse();
        assertThat(mainPref.isEnabled()).isFalse();
        assertThat(mainPref.getOnPreferenceChangeListener()).isNull();
        var footerPref = mPreferenceScreen.findPreference(PrefBasics.FOOTER.key);
        assertThat(footerPref).isNotNull();
        assertThat("" + footerPref.getTitle())
                .isEqualTo(getText(EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE));
    }

    @Test
    @UiThreadTest
    public void testDisplayRotationPreference() {
        final int displayId = 1;
        var fragment = initFragment();
        mHandler.flush();
        var category = getExternalDisplayCategory(0);
        ListPreference pref = category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat("" + pref.getTitle()).isEqualTo(
                getText(PrefBasics.EXTERNAL_DISPLAY_ROTATION.titleResource));
        assertThat(pref.getEntries().length).isEqualTo(4);
        assertThat(pref.getEntryValues().length).isEqualTo(4);
        assertThat(pref.getEntryValues()[0].toString()).isEqualTo("0");
        assertThat(pref.getEntryValues()[1].toString()).isEqualTo("1");
        assertThat(pref.getEntryValues()[2].toString()).isEqualTo("2");
        assertThat(pref.getEntryValues()[3].toString()).isEqualTo("3");
        assertThat(pref.getEntries()[0].length()).isGreaterThan(0);
        assertThat(pref.getEntries()[1].length()).isGreaterThan(0);
        assertThat("" + pref.getSummary()).isEqualTo(pref.getEntries()[0].toString());
        assertThat(pref.getValue()).isEqualTo("0");
        assertThat(pref.getOnPreferenceChangeListener()).isNotNull();
        assertThat(pref.isEnabled()).isTrue();
        var rotation = 1;
        doReturn(true).when(mMockedInjector).freezeDisplayRotation(displayId, rotation);
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, rotation + ""))
                .isTrue();
        verify(mMockedInjector).freezeDisplayRotation(displayId, rotation);
        assertThat(pref.getValue()).isEqualTo(rotation + "");
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testDisplayResolutionPreference() {
        final int displayId = 1;
        var fragment = initFragment();
        mHandler.flush();
        var category = getExternalDisplayCategory(0);
        var pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat("" + pref.getTitle()).isEqualTo(
                getText(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.titleResource));
        assertThat("" + pref.getSummary()).isEqualTo("1920 x 1080");
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.getOnPreferenceClickListener()).isNotNull();
        assertThat(pref.getOnPreferenceClickListener().onPreferenceClick(pref)).isTrue();
        assertThat(mResolutionSelectorDisplayId).isEqualTo(displayId);
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testDisplaySizePreference() {
        var fragment = initFragment();
        mHandler.flush();
        var category = getExternalDisplayCategory(0);
        var pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat("" + pref.getTitle())
                .isEqualTo(getText(PrefBasics.EXTERNAL_DISPLAY_SIZE.titleResource));
        assertThat("" + pref.getSummary())
                .isEqualTo(getText(EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE));
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.getOnPreferenceClickListener()).isNotNull();
        assertThat(pref.getOnPreferenceClickListener().onPreferenceClick(pref)).isTrue();
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testUseDisplayPreference_EnabledDisplay() {
        final int displayId = 1;
        doReturn(true).when(mMockedInjector).enableConnectedDisplay(displayId);
        doReturn(true).when(mMockedInjector).disableConnectedDisplay(displayId);
        var fragment = initFragment();
        mHandler.flush();
        MainSwitchPreference pref = getExternalDisplayCategory(0)
                .findPreference(PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(0));
        assertThat(pref.getKey()).isEqualTo(PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(0));
        assertThat("" + pref.getTitle())
                .isEqualTo(getText(PrefBasics.EXTERNAL_DISPLAY_USE.titleResource));
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getOnPreferenceChangeListener()).isNotNull();
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, false)).isTrue();
        verify(mMockedInjector).disableConnectedDisplay(displayId);
        assertThat(pref.isChecked()).isFalse();
        assertThat(pref.getOnPreferenceChangeListener().onPreferenceChange(pref, true)).isTrue();
        verify(mMockedInjector).enableConnectedDisplay(displayId);
        assertThat(pref.isChecked()).isTrue();
        verify(mMockedMetricsLogger, times(2)).writePreferenceClickMetric(pref);
    }

    @NonNull
    private ExternalDisplayPreferenceFragment initFragment() {
        if (mFragment != null) {
            return mFragment;
        }
        mFragment = new TestableExternalDisplayPreferenceFragment();
        mFragment.onCreateCallback(null);
        mFragment.onActivityCreatedCallback(null);
        mFragment.onStartCallback();
        return mFragment;
    }

    @NonNull
    private String getText(int id) {
        return mContext.getResources().getText(id).toString();
    }

    private class TestableExternalDisplayPreferenceFragment extends
            ExternalDisplayPreferenceFragment {
        private final View mMockedRootView;
        private final TextView mEmptyView;
        private final Activity mMockedActivity;
        private final MetricsLogger mLogger;

        TestableExternalDisplayPreferenceFragment() {
            super(mMockedInjector);
            mMockedActivity = mock(Activity.class);
            mMockedRootView = mock(View.class);
            mEmptyView = new TextView(mContext);
            doReturn(mEmptyView).when(mMockedRootView).findViewById(android.R.id.empty);
            mLogger = mMockedMetricsLogger;
        }

        @Override
        public PreferenceScreen getPreferenceScreen() {
            return mPreferenceScreen;
        }

        @Override
        protected Activity getCurrentActivity() {
            return mMockedActivity;
        }

        @Override
        public View getView() {
            return mMockedRootView;
        }

        @Override
        public void setEmptyView(View view) {
            assertThat(view).isEqualTo(mEmptyView);
        }

        @Override
        public View getEmptyView() {
            return mEmptyView;
        }

        @Override
        public void addPreferencesFromResource(int resource) {
            mPreferenceIdFromResource = resource;
        }

        @Override
        protected void launchResolutionSelector(@NonNull Context context, int displayId) {
            mResolutionSelectorDisplayId = displayId;
        }

        @Override
        Preference newFooterPreference(Context context) {
            return new Preference(context);
        }

        @Override
        protected void launchBuiltinDisplaySettings() {
            mLaunchedBuiltinSettings = true;
        }

        @Override
        protected void writePreferenceClickMetric(Preference preference) {
            mLogger.writePreferenceClickMetric(preference);
        }
    }

    /**
     * Interface allowing to mock and spy on log events.
     */
    public interface MetricsLogger {

        /**
         * On preference click metric
         */
        void writePreferenceClickMetric(Preference preference);
    }
}
