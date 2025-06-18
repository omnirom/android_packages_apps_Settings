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

package com.android.settings.accessibility;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode;
import android.text.TextUtils;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.preference.Preference;
import androidx.preference.PreferenceManager;
import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.MagnificationCursorFollowingModePreferenceController.ModeInfo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MagnificationCursorFollowingModePreferenceController}. */
@RunWith(RobolectricTestRunner.class)
public class MagnificationCursorFollowingModePreferenceControllerTest {
    private static final String PREF_KEY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE;

    @Rule
    public MockitoRule mocks = MockitoJUnit.rule();

    @Spy
    private TestDialogHelper mDialogHelper = new TestDialogHelper();

    private PreferenceScreen mScreen;
    private Context mContext;
    private MagnificationCursorFollowingModePreferenceController mController;
    private Preference mModePreference;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        final PreferenceManager preferenceManager = new PreferenceManager(mContext);
        mScreen = preferenceManager.createPreferenceScreen(mContext);
        mModePreference = new Preference(mContext);
        mModePreference.setKey(PREF_KEY);
        mScreen.addPreference(mModePreference);
        mController = new MagnificationCursorFollowingModePreferenceController(mContext, PREF_KEY);
        mController.setDialogHelper(mDialogHelper);
        mDialogHelper.setDialogDelegate(mController);
        showPreferenceOnTheScreen();
    }

    private void showPreferenceOnTheScreen() {
        mController.displayPreference(mScreen);
    }

    @AccessibilityMagnificationCursorFollowingMode
    private int getCheckedModeFromDialog() {
        final ListView listView = mController.mModeListView;
        assertThat(listView).isNotNull();

        final int checkedPosition = listView.getCheckedItemPosition();
        assertWithMessage("No mode is checked").that(checkedPosition)
                .isNotEqualTo(AdapterView.INVALID_POSITION);

        final ModeInfo modeInfo = (ModeInfo) listView.getAdapter().getItem(checkedPosition);
        return modeInfo.mMode;
    }

    private void performItemClickWith(@AccessibilityMagnificationCursorFollowingMode int mode) {
        final ListView listView = mController.mModeListView;
        assertThat(listView).isNotNull();

        int modeIndex = AdapterView.NO_ID;
        for (int i = 0; i < listView.getAdapter().getCount(); i++) {
            final ModeInfo modeInfo = (ModeInfo) listView.getAdapter().getItem(i);
            if (modeInfo != null && modeInfo.mMode == mode) {
                modeIndex = i;
                break;
            }
        }
        assertWithMessage("The mode could not be found").that(modeIndex)
                .isNotEqualTo(AdapterView.NO_ID);

        listView.performItemClick(listView.getChildAt(modeIndex), modeIndex, modeIndex);
    }

    @Test
    public void clickPreference_defaultMode_selectionIsDefault() {
        mController.handlePreferenceTreeClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS);
    }

    @Test
    public void clickPreference_nonDefaultMode_selectionIsExpected() {
        Settings.Secure.putInt(mContext.getContentResolver(), PREF_KEY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER);

        mController.handlePreferenceTreeClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER);
    }

    @Test
    public void selectItemInDialog_selectionIsExpected() {
        mController.handlePreferenceTreeClick(mModePreference);

        performItemClickWith(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE);
    }

    @Test
    public void selectItemInDialog_dismissWithoutSave_selectionNotPersists() {
        mController.handlePreferenceTreeClick(mModePreference);

        performItemClickWith(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE);

        showPreferenceOnTheScreen();

        mController.handlePreferenceTreeClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS);
        assertThat(TextUtils.equals(mController.getSummary(), mContext.getString(
                R.string.accessibility_magnification_cursor_following_continuous))).isTrue();
    }

    @Test
    public void selectItemInDialog_saveAndDismiss_selectionPersists() {
        mController.handlePreferenceTreeClick(mModePreference);

        performItemClickWith(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE);
        mController.onMagnificationCursorFollowingModeDialogPositiveButtonClicked(
                mDialogHelper.getDialog(), DialogInterface.BUTTON_POSITIVE);

        showPreferenceOnTheScreen();

        mController.handlePreferenceTreeClick(mModePreference);

        assertThat(getCheckedModeFromDialog()).isEqualTo(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE);
        assertThat(TextUtils.equals(mController.getSummary(), mContext.getString(
                R.string.accessibility_magnification_cursor_following_edge))).isTrue();
    }
}
