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

import static com.android.settings.accessibility.FontWeightAdjustmentPreferenceController.BOLD_TEXT_ADJUSTMENT;
import static com.android.settings.accessibility.TextReadingPreferenceFragment.EXTRA_LAUNCHED_FROM;
import static com.android.settingslib.metadata.PreferenceScreenBindingKeyProviderKt.EXTRA_BINDING_SCREEN_KEY;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.provider.Settings;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.preference.PreferenceManager;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.accessibility.TextReadingPreferenceFragment.EntryPoint;
import com.android.settings.accessibility.TextReadingResetController.ResetStateListener;
import com.android.settings.accessibility.textreading.ui.TextReadingScreen;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenFromNotification;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenInAnythingElse;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenInSuw;
import com.android.settings.accessibility.textreading.ui.TextReadingScreenOnAccessibility;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.XmlTestUtils;

import com.google.testing.junit.testparameterinjector.TestParameters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestParameterInjector;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowToast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Tests for {@link TextReadingPreferenceFragment}. */
@RunWith(RobolectricTestParameterInjector.class)
@Config(shadows = {
        com.android.settings.testutils.shadow.ShadowFragment.class,
})
public class TextReadingPreferenceFragmentTest {

    @Rule
    public final MockitoRule mMockito = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PreferenceManager mPreferenceManager;
    private Context mContext = ApplicationProvider.getApplicationContext();
    private TextReadingPreferenceFragment mFragment;

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);

        mFragment = spy(new TextReadingPreferenceFragment());
        when(mFragment.getPreferenceManager()).thenReturn(mPreferenceManager);
        when(mFragment.getPreferenceManager().getContext()).thenReturn(mContext);
        when(mFragment.getContext()).thenReturn(mContext);
        when(mFragment.getActivity()).thenReturn(Robolectric.setupActivity(FragmentActivity.class));

        // Avoid a NPE is happened in ShadowWindowManagerGlobal
        doReturn(mock(DisplaySizeData.class)).when(mFragment).createDisplaySizeData(mContext);
        mFragment.createPreferenceControllers(mContext);
    }

    @DisableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_needResetSettings() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
        ShadowLooper.idleMainLooper();

        assertThat(mFragment.mNeedResetSettings).isTrue();
    }

    @DisableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void onDialogPositiveButtonClicked_boldTextDisabled_resetAllListeners() {
        final ResetStateListener listener1 = mock(ResetStateListener.class);
        final ResetStateListener listener2 = mock(ResetStateListener.class);
        mFragment.mResetStateListeners = new ArrayList<>(Arrays.asList(listener1, listener2));
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
        ShadowLooper.idleMainLooper();

        verify(listener1).resetState();
        verify(listener2).resetState();
    }

    @DisableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void onDialogPositiveButtonClicked_boldTextEnabled_showToast() {
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.FONT_WEIGHT_ADJUSTMENT, BOLD_TEXT_ADJUSTMENT);
        final AlertDialog dialog = (AlertDialog) mFragment.onCreateDialog(
                DialogEnums.DIALOG_RESET_SETTINGS);
        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).callOnClick();
        ShadowLooper.idleMainLooper();

        assertThat(ShadowToast.getTextOfLatestToast())
                .isEqualTo(mContext.getString(R.string.accessibility_text_reading_reset_message));
    }

    @Test
    public void getMetricsCategory_returnsCorrectCategory() {
        assertThat(mFragment.getMetricsCategory()).isEqualTo(
                SettingsEnums.ACCESSIBILITY_TEXT_READING_OPTIONS);
    }

    @Test
    public void getPreferenceScreenResId_returnsCorrectXml() {
        assertThat(mFragment.getPreferenceScreenResId()).isEqualTo(
                R.xml.accessibility_text_reading_options);
    }

    @Test
    public void getLogTag_returnsCorrectTag() {
        assertThat(mFragment.getLogTag()).isEqualTo("TextReadingPreferenceFragment");
    }

    @DisableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void getNonIndexableKeys_existInXmlLayout() {
        final List<String> niks = TextReadingPreferenceFragment.SEARCH_INDEX_DATA_PROVIDER
                .getNonIndexableKeys(mContext);
        final List<String> keys =
                XmlTestUtils.getKeysFromPreferenceXml(mContext,
                        R.xml.accessibility_text_reading_options);

        assertThat(keys).containsAtLeastElementsIn(niks);
    }

    @DisableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void getPreferenceScreenBindingKey_flagOff() {
        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isNull();
    }

    @EnableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @Test
    public void getPreferenceScreenBindingKey_fromCatalystScreen_returnsCorrectKey() {
        final String bindingKey = TextReadingScreen.KEY;
        final Bundle bundle = new Bundle();
        bundle.putString(EXTRA_BINDING_SCREEN_KEY, bindingKey);
        mFragment.setArguments(bundle);

        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(bindingKey);
    }

    @EnableFlags(Flags.FLAG_CATALYST_TEXT_READING_SCREEN)
    @TestParameters({
            "{entryPoint: " + EntryPoint.UNKNOWN_ENTRY + ", expectedScreenKey: "
                    + TextReadingScreen.KEY + "}",
            "{entryPoint: " + EntryPoint.SUW_VISION_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreenInSuw.KEY + "}",
            "{entryPoint: " + EntryPoint.SUW_ANYTHING_ELSE + ", expectedScreenKey: "
                    + TextReadingScreenInAnythingElse.KEY + "}",
            "{entryPoint: " + EntryPoint.HIGH_CONTRAST_TEXT_NOTIFICATION + ", expectedScreenKey: "
                    + TextReadingScreenFromNotification.KEY + "}",
            "{entryPoint: " + EntryPoint.ACCESSIBILITY_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreenOnAccessibility.KEY + "}",
            "{entryPoint: " + EntryPoint.DISPLAY_SETTINGS + ", expectedScreenKey: "
                    + TextReadingScreen.KEY + "}",
    })
    @Test
    public void getPreferenceScreenBindingKey_fromNonCatalystScreen(int entryPoint,
            String expectedScreenKey) {
        final Bundle bundle = new Bundle();
        bundle.putInt(EXTRA_LAUNCHED_FROM, entryPoint);
        mFragment.setArguments(bundle);

        assertThat(mFragment.getPreferenceScreenBindingKey(mContext)).isEqualTo(expectedScreenKey);
    }


}
