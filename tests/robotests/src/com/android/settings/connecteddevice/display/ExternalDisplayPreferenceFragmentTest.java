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

import static android.app.ActivityManager.LOCK_TASK_MODE_LOCKED;
import static android.app.ActivityManager.LOCK_TASK_MODE_NONE;
import static android.hardware.display.DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK;
import static android.hardware.display.DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR;
import static android.provider.Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY;
import static android.provider.Settings.Secure.MIRROR_BUILT_IN_DISPLAY;
import static android.view.Display.INVALID_DISPLAY;

import static com.android.server.display.feature.flags.Flags.FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.CONNECTION_PREF_DESKTOP;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.CONNECTION_PREF_MIRROR;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.CONNECTION_PREF_NONE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_TITLE_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY_SUMMARY_RESOURCE;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING;
import static com.android.settings.flags.Flags.FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST;
import static com.android.window.flags.Flags.FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.DevicePolicyResourcesManager;
import android.app.admin.DpcAuthority;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.test.annotation.UiThreadTest;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.connecteddevice.display.ExternalDisplayPreferenceFragment.PrefBasics;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.MainSwitchPreference;

import kotlin.Unit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.List;

/** Unit tests for {@link ExternalDisplayPreferenceFragment}.  */
@RunWith(AndroidJUnit4.class)
public class ExternalDisplayPreferenceFragmentTest extends ExternalDisplayTestBase {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    @Nullable
    private ExternalDisplayPreferenceFragment mFragment;
    private int mPreferenceIdFromResource;
    private boolean mLaunchedBuiltinSettings;
    private int mResolutionSelectorDisplayId = INVALID_DISPLAY;
    @Mock
    private MetricsLogger mMockedMetricsLogger;
    @Mock private DevicePolicyManager mMockDpm;

    @Before
    public void setUp() throws RemoteException {
        super.setUp();
        EnforcingAdmin enforcingAdmin =
                new EnforcingAdmin(
                        "pkg.test",
                        DpcAuthority.DPC_AUTHORITY,
                        UserHandle.of(UserHandle.myUserId()),
                        new ComponentName("admin", "adminclass"));
        PolicyEnforcementInfo mockPolicyInfo = mock(PolicyEnforcementInfo.class);
        DevicePolicyResourcesManager mockPolicyResourcesManager =
                mock(DevicePolicyResourcesManager.class);
        doReturn(enforcingAdmin).when(mockPolicyInfo).getMostImportantEnforcingAdmin();
        doReturn(mockPolicyInfo)
                .when(mMockDpm)
                .getEnforcingAdminsForPolicy(
                        eq(DevicePolicyIdentifiers.LOCK_TASK_POLICY), anyInt());
        doReturn("").when(mockPolicyResourcesManager).getString(any(), any());
        doReturn(mockPolicyResourcesManager).when(mMockDpm).getResources();
        doReturn(mMockDpm).when(mContext).getSystemService(Context.DEVICE_POLICY_SERVICE);
    }

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

        verify(mMockedInjector, never()).getDisplays();
        mHandler.flush();
        assertThat(mHandler.getPendingMessages().size()).isEqualTo(0);
        verify(mMockedInjector).getDisplays();
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
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
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
        doReturn(List.of()).when(mMockedInjector).getDisplays();
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
    @DisableFlags(FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG)
    public void testShowDisplayControlsDisabled() {
        doReturn(List.of(
                        createExternalDisplay(DisplayIsEnabled.NO),
                        createOverlayDisplay(DisplayIsEnabled.YES)))
                .when(mMockedInjector).getDisplays();
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
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testShowDisplayControlsDisabled_updatedDialogEnabled() {
        doReturn(List.of(
                createExternalDisplay(DisplayIsEnabled.NO),
                createOverlayDisplay(DisplayIsEnabled.YES)))
                .when(mMockedInjector).getDisplays();
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
                    PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(disp));
            assertWithMessage("connection " + disp).that(pref.isEnabled()).isEqualTo(disp == 1);

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
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
        mHandler.flush();
        int attachedId = mDisplays.get(0).getId();
        assertDisplayListCount(1);
        assertThat("" + getExternalDisplayCategory(0).getTitle()).isEqualTo("HDMI");
    }

    @Test
    @UiThreadTest
    @DisableFlags(FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG)
    public void testShowEnabledDisplay_OnlyOneDisplayAvailable_displaySizeDisabled() {
        mFlags.setFlag(FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING, false);
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
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
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testShowEnabledDisplay_OnlyOneDisplayAvailable_displaySizeDisabled_updatedDialog() {
        mFlags.setFlag(FLAG_DISPLAY_SIZE_CONNECTED_DISPLAY_SETTING, false);
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
        // Init
        initFragment();
        mHandler.flush();
        assertDisplayListCount(1);
        var category = getExternalDisplayCategory(0);
        var pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));
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
    @EnableFlags(FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT)
    public void testShowEnabledDisplay_displaySizeDisabled_isInMirrorMode_doNotShowSizePref() {
        // Mirror built in display
        Settings.Secure.putInt(
                mContext.getContentResolver(), MIRROR_BUILT_IN_DISPLAY, 1);
        // Only one display available
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
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
        doReturn(List.of(mDisplays.get(0))).when(mMockedInjector).getDisplays();
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
    @DisableFlags(FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG)
    public void testShowOneEnabledDisplay_FewAvailable() {
        initFragment();
        verify(mMockedInjector, never()).getDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getDisplays();
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
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testShowOneEnabledDisplay_FewAvailable_updatedDialogEnabled() {
        initFragment();
        verify(mMockedInjector, never()).getDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getDisplays();
        var pref = mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(0));
        assertThat(pref).isNotNull();
        pref = mPreferenceScreen.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));
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
    @DisableFlags(FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG)
    public void testShowDisabledDisplay() {
        initFragment();
        var disabledDisplays = List.of(
                createExternalDisplay(DisplayIsEnabled.NO),
                createOverlayDisplay(DisplayIsEnabled.NO));
        doReturn(disabledDisplays).when(mMockedInjector).getDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getDisplays();
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
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testShowDisabledDisplay_updatedDialogEnabled() {
        initFragment();
        var disabledDisplays = List.of(
                createExternalDisplay(DisplayIsEnabled.NO),
                createOverlayDisplay(DisplayIsEnabled.NO));
        doReturn(disabledDisplays).when(mMockedInjector).getDisplays();
        mHandler.flush();
        verify(mMockedInjector, never()).getDisplay(anyInt());
        verify(mMockedInjector).getDisplays();
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
        pref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));
        assertThat(pref.isEnabled()).isFalse();
        var footerPref = category.findPreference(PrefBasics.FOOTER.key);
        assertThat(footerPref).isNull();
        var sizePref = category.findPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(0));
        assertThat(sizePref.isEnabled()).isFalse();
    }

    @Test
    @UiThreadTest
    public void testNoDisplays() {
        doReturn(List.of()).when(mMockedInjector).getDisplays();
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
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testDisplayConnectionPreference() {
        final int[] savedPreference = {EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK};
        doAnswer(invocation -> savedPreference[0])
                .when(mMockedInjector).getDisplayConnectionPreference(anyString());
        doAnswer(invocation -> {
            savedPreference[0] = invocation.getArgument(1);
            return Unit.INSTANCE;
        }).when(mMockedInjector).updateDisplayConnectionPreference(anyString(), anyInt());

        initFragment();
        mHandler.flush();

        var category = getExternalDisplayCategory(0);
        RestrictedListPreference pref = category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));
        assertThat(pref.getTitle().toString()).isEqualTo(
                getText(PrefBasics.EXTERNAL_DISPLAY_CONNECTION.titleResource));
        assertThat(pref.getEntries().length).isEqualTo(3);
        assertThat(pref.getEntryValues().length).isEqualTo(3);
        assertThat(pref.getEntryValues()[0].toString()).isEqualTo(CONNECTION_PREF_NONE);
        assertThat(pref.getEntryValues()[1].toString()).isEqualTo(CONNECTION_PREF_DESKTOP);
        assertThat(pref.getEntryValues()[2].toString()).isEqualTo(CONNECTION_PREF_MIRROR);
        assertThat(pref.getSummary().toString()).isEqualTo(pref.getEntries()[0].toString());
        assertThat(pref.getValue()).isEqualTo(CONNECTION_PREF_NONE);
        assertThat(pref.getOnPreferenceChangeListener()).isNotNull();
        assertThat(pref.isEnabled()).isTrue();

        assertThat(pref.getOnPreferenceChangeListener()
                .onPreferenceChange(pref, CONNECTION_PREF_MIRROR)).isTrue();
        verify(mMockedInjector).updateDisplayConnectionPreference(
                mDisplays.getFirst().getUniqueId(),
                EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR);

        mHandler.flush(); // manually update UI since no display change has occurred
        assertThat(pref.getValue()).isEqualTo(CONNECTION_PREF_MIRROR);
        assertThat(pref.getSummary().toString()).isEqualTo(pref.getEntries()[2].toString());
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

    @Test
    @UiThreadTest
    public void testIncludeDefaultDisplayInTopologyPreference() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        setIncludeDefaultDisplayInTopologySettings(true);
        initFragment();
        mHandler.flush();
        var pref =
                (SwitchPreferenceCompat)
                        mPreferenceScreen.findPreference(
                                PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.key);
        assertThat("" + pref.getTitle())
                .isEqualTo(getText(PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.titleResource));
        assertThat("" + pref.getSummary())
                .isEqualTo(getText(INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY_SUMMARY_RESOURCE));
        assertThat(pref.isEnabled()).isTrue();
        assertThat(pref.isChecked()).isTrue();
        assertThat(pref.getOnPreferenceClickListener()).isNotNull();
        assertThat(pref.getOnPreferenceClickListener().onPreferenceClick(pref)).isTrue();
        verify(mMockedMetricsLogger).writePreferenceClickMetric(pref);
    }

    @Test
    @UiThreadTest
    public void testIncludeDefaultDisplayInTopologyPreference_verifySettingsChange() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        setIncludeDefaultDisplayInTopologySettings(false);
        initFragment();
        mHandler.flush();
        var pref =
                (SwitchPreferenceCompat)
                        mPreferenceScreen.findPreference(
                                PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.key);

        pref.setChecked(true);
        pref.getOnPreferenceClickListener().onPreferenceClick(pref);

        assertThat(getIncludeDefaultDisplayInTopologySettings()).isEqualTo(1);

        pref.setChecked(false);
        pref.getOnPreferenceClickListener().onPreferenceClick(pref);

        assertThat(getIncludeDefaultDisplayInTopologySettings()).isEqualTo(0);
    }

    @Test
    @UiThreadTest
    public void testIncludeDefaultDisplayInTopologyPreference_mirroringMode_notAdding() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        Settings.Secure.putInt(mContext.getContentResolver(), MIRROR_BUILT_IN_DISPLAY, 1);
        initFragment();
        mHandler.flush();
        var pref =
                mPreferenceScreen.findPreference(
                        PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.key);

        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    public void testIncludeDefaultDisplayInTopologyPreference_notProjectedMode_notAdding() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        doReturn(false).when(mMockedInjector).isProjectedModeEnabled();
        initFragment();
        mHandler.flush();
        var pref =
                mPreferenceScreen.findPreference(
                        PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.key);

        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testAddConnectionPreference_notProjectedMode_notAdding() {
        doReturn(false).when(mMockedInjector).isProjectedModeEnabled();
        initFragment();
        mHandler.flush();

        var category = getExternalDisplayCategory(0);
        var pref = category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));

        assertThat(pref).isNull();
    }

    @Test
    @UiThreadTest
    public void testLockTaskModeLocked_disableMirroringMode() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        ExternalDisplayPreferenceFragment fragment = initFragment();
        mHandler.flush();

        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED);
        mHandler.flush();
        var pref = mPreferenceScreen.findPreference(PrefBasics.MIRROR.key);

        assertThat(pref.isEnabled()).isEqualTo(false);
    }

    @Test
    @UiThreadTest
    public void testLockTaskModeNone_enableMirroringMode() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        ExternalDisplayPreferenceFragment fragment = initFragment();
        mHandler.flush();

        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED);
        mHandler.flush();
        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_NONE);
        mHandler.flush();
        var pref = mPreferenceScreen.findPreference(PrefBasics.MIRROR.key);

        assertThat(pref.isEnabled()).isEqualTo(true);
    }

    @Test
    @UiThreadTest
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testLockTaskModeLocked_disableConnectionPreference() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        ExternalDisplayPreferenceFragment fragment = initFragment();
        mHandler.flush();

        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED);
        mHandler.flush();
        var category = getExternalDisplayCategory(0);
        ListPreference pref = category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));

        assertThat(pref.isEnabled()).isEqualTo(false);
    }

    @Test
    @UiThreadTest
    @EnableFlags({
            FLAG_ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT,
            FLAG_ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG,
    })
    public void testLockTaskModeNone_enableConnectionPreference() {
        mFlags.setFlag(FLAG_DISPLAY_TOPOLOGY_PANE_IN_DISPLAY_LIST, true);
        ExternalDisplayPreferenceFragment fragment = initFragment();
        mHandler.flush();

        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_LOCKED);
        mHandler.flush();
        fragment.mLockTaskModeChangedListener.onLockTaskModeChanged(LOCK_TASK_MODE_NONE);
        mHandler.flush();
        var category = getExternalDisplayCategory(0);
        ListPreference pref = category.findPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(0));

        assertThat(pref.isEnabled()).isEqualTo(true);
    }

    @Test
    public void testSearchIndexProvider_getXmlResourcesToIndex() {
        final Indexable.SearchIndexProvider provider =
                ExternalDisplayPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER;

        final List<SearchIndexableResource> resources = provider.getXmlResourcesToIndex(mContext,
                true);

        assertThat(resources).hasSize(1);
        assertThat(resources.get(0).xmlResId).isEqualTo(
                ExternalDisplayPreferenceFragment.EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    @Test
    public void testSearchIndexProvider_getRawIndexData() {
        final Indexable.SearchIndexProvider provider =
                ExternalDisplayPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER;

        final List<SearchIndexableRaw> indexData = provider.getRawDataToIndex(
                mContext, /* enabled= */ true);
        assertThat(indexData).hasSize(1);
        assertThat(indexData.getFirst().screenTitle).contains(
                mContext.getString(R.string.connected_devices_dashboard_title));
        assertThat(indexData.getFirst().keywords).isEqualTo(
                mContext.getString(R.string.keywords_external_display_settings));
        assertThat(indexData.getFirst().title).isEqualTo(
                mContext.getString(EXTERNAL_DISPLAY_TITLE_RESOURCE));
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
        public Context getContext() {
            return mContext;
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
        protected void launchResolutionSelector(int displayId) {
            mResolutionSelectorDisplayId = displayId;
        }

        @Override
        Preference newFooterPreference() {
            return new Preference(mContext);
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

    private int getIncludeDefaultDisplayInTopologySettings() {
        return Settings.Secure.getInt(
                mContext.getContentResolver(),
                INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
                0);
    }

    private boolean setIncludeDefaultDisplayInTopologySettings(boolean value) {
        return Settings.Secure.putInt(
                mContext.getContentResolver(), INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY, value ? 1 : 0);
    }
}
