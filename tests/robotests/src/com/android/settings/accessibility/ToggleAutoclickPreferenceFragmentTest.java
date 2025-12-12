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

package com.android.settings.accessibility;


import static com.android.internal.accessibility.AccessibilityShortcutController.AUTOCLICK_COMPONENT_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.ALL;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.settings.testutils.AccessibilityTestUtils.assertEditShortcutsScreenShown;
import static com.android.settings.testutils.AccessibilityTestUtils.assertShortcutsTutorialDialogShown;
import static com.android.settings.testutils.AccessibilityTestUtils.inflateShortcutPreferenceView;

import static com.google.common.truth.Truth.assertThat;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.testutils.XmlTestUtils;
import com.android.settings.testutils.shadow.ShadowAccessibilityManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowDialog;

import java.util.List;
import java.util.Set;

/**
 * Tests for {@link ToggleAutoclickPreferenceFragment}.
 */
@RunWith(RobolectricTestRunner.class)
public class ToggleAutoclickPreferenceFragmentTest {
    private static final String KEY_AUTOCLICK_SHORTCUT_PREFERENCE = "autoclick_shortcut_preference";

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentScenario<ToggleAutoclickPreferenceFragment> mFragScenario = null;
    private ToggleAutoclickPreferenceFragment mFragment;
    private ShadowAccessibilityManager mA11yManager =
            Shadow.extract(mContext.getSystemService(AccessibilityManager.class));

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
    }

    @After
    public void cleanUp() {
        if (mFragScenario != null) {
            mFragScenario.close();
        }
    }

    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    @Test
    public void verifyFragmentUI_flagOff_doesNotContainShortcutToggle() {
        launchFragment();
        Preference pref = mFragment.findPreference(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
        assertThat(pref).isNotNull();
        assertThat(pref.isVisible()).isFalse();
    }

    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    @Test
    public void verifyFragmentUI_containsShortcutToggle() {
        launchFragment();
        Preference pref = mFragment.findPreference(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
        assertThat(pref).isNotNull();
        assertThat(pref.isVisible()).isTrue();
        assertThat(pref.getTitle().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_autoclick_shortcut_title));
    }

    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    @Test
    public void shortcutOff_clickShortcutToggle_turnOnShortcutAndShowShortcutTutorial() {
        mA11yManager.enableShortcutsForTargets(
                /* enable= */ false, ALL, Set.of(AUTOCLICK_COMPONENT_NAME.flattenToString()),
                mContext.getUserId());
        launchFragment();

        ShortcutPreference pref = mFragment.findPreference(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
        assertThat(pref).isNotNull();
        assertThat(pref.isChecked()).isFalse();
        PreferenceViewHolder viewHolder =
                inflateShortcutPreferenceView(mFragment.getContext(), pref);

        View widget = viewHolder.findViewById(pref.getSwitchResId());
        assertThat(widget).isNotNull();
        widget.performClick();

        assertThat(pref.isChecked()).isTrue();
        assertShortcutsTutorialDialogShown(mFragment);
    }

    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    @Test
    public void shortcutOn_clickShortcutToggle_turnOffShortcutAndNoTutorialShown() {
        mA11yManager.enableShortcutsForTargets(
                /* enable= */ true, HARDWARE,
                Set.of(AUTOCLICK_COMPONENT_NAME.flattenToString()), mContext.getUserId());
        launchFragment();

        ShortcutPreference pref = mFragment.findPreference(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
        assertThat(pref).isNotNull();
        assertThat(pref.isChecked()).isTrue();
        PreferenceViewHolder viewHolder = inflateShortcutPreferenceView(
                mFragment.getContext(), pref);

        View widget = viewHolder.findViewById(pref.getSwitchResId());
        assertThat(widget).isNotNull();
        widget.performClick();

        assertThat(pref.isChecked()).isFalse();
        assertThat(mA11yManager.getAccessibilityShortcutTargets(HARDWARE)).isEmpty();
        assertThat(ShadowDialog.getLatestDialog()).isNull();
    }

    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    @Test
    public void clickShortcutSettings_showEditShortcutsScreenWithoutChangingShortcutToggleState() {
        launchFragment();
        ShortcutPreference pref = mFragment.findPreference(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
        assertThat(pref).isNotNull();
        final boolean shortcutToggleState = pref.isChecked();
        pref.performClick();

        assertEditShortcutsScreenShown(mFragment);
        assertThat(pref.isChecked()).isEqualTo(shortcutToggleState);
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        launchFragment();
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TOGGLE_AUTOCLICK);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        launchFragment();
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_autoclick_settings);
    }

    @Test
    public void getHelpResource_returnsCorrectHelpResource() {
        launchFragment();
        assertThat(mFragment.getHelpResource()).isEqualTo(R.string.help_url_autoclick);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        launchFragment();
        assertThat(mFragment.getLogTag()).isEqualTo("AutoclickPrefFragment");
    }

    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = ToggleAutoclickPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_autoclick_settings);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @Test
    @DisableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getNonIndexableKeys_flagDisabled_returnsOnlyShortcutKey() {
        final List<String> niks = ToggleAutoclickPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).contains(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
    }

    @Test
    @EnableFlags(com.android.server.accessibility.Flags.FLAG_ENABLE_AUTOCLICK_INDICATOR)
    public void getNonIndexableKeys_doesNotContainShortcut() {
        final List<String> niks = ToggleAutoclickPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);

        assertThat(niks).doesNotContain(KEY_AUTOCLICK_SHORTCUT_PREFERENCE);
    }

    private void launchFragment() {
        mFragScenario = FragmentScenario.launch(
                ToggleAutoclickPreferenceFragment.class,
                /* bundle= */ null,
                androidx.appcompat.R.style.Theme_AppCompat,
                (FragmentFactory) null).moveToState(Lifecycle.State.RESUMED);
        mFragScenario.onFragment(frag -> mFragment = frag);
    }
}
