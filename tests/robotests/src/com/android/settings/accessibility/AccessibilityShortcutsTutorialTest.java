/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.GESTURE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.HARDWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.QUICK_SETTINGS;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.SOFTWARE;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TRIPLETAP;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.TWOFINGER_DOUBLETAP;

import static com.google.common.truth.Truth.assertThat;

import static org.robolectric.Shadows.shadowOf;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.text.SpannableStringBuilder;
import android.util.ArrayMap;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.TextSwitcher;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentFactory;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.viewpager.widget.ViewPager;

import com.android.internal.accessibility.common.ShortcutConstants;
import com.android.server.accessibility.Flags;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SubSettings;
import com.android.settings.testutils.AccessibilityTestUtils;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.utils.StringUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAccessibilityManager;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;

import java.util.Map;

/** Tests for {@link AccessibilityShortcutsTutorial}. */
@Config(shadows = SettingsShadowResources.class)
@RunWith(RobolectricTestRunner.class)
public final class AccessibilityShortcutsTutorialTest {
    private static final String FAKE_FEATURE_NAME = "Fake Feature Name";

    @Rule
    public final MockitoRule mMockitoRule = MockitoJUnit.rule();
    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();
    @Mock
    private DialogInterface.OnDismissListener mOnDismissListener;

    private final Context mContext = ApplicationProvider.getApplicationContext();
    private FragmentScenario<Fragment> mFragmentScenario = null;

    @Before
    public void setUp() {
        mContext.setTheme(androidx.appcompat.R.style.Theme_AppCompat);
        mFragmentScenario = FragmentScenario.launch(Fragment.class, /* bundle= */ null,
                androidx.appcompat.R.style.Theme_AppCompat, (FragmentFactory) null);
    }

    @After
    public void cleanUp() {
        if (mFragmentScenario != null) {
            mFragmentScenario.close();
        }
    }

    @Test
    public void createTutorialPages_turnOnTripleTapShortcut_hasOnePage() {
        showShortcutsTutorialDialog(TRIPLETAP, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        assertTutorialPageSize(1);
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void createTutorialPages_turnOnTwoFingerTripleTapShortcut_hasOnePage() {
        showShortcutsTutorialDialog(TWOFINGER_DOUBLETAP, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        assertTutorialPageSize(1);
    }

    @Test
    public void createTutorialPages_turnOnQuickSettingShortcut_hasOnePage() {
        showShortcutsTutorialDialog(QUICK_SETTINGS, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        assertTutorialPageSize(1);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_hasOnePage() {
        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        assertTutorialPageSize(1);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareAndHardwareShortcuts_hasTwoPages() {
        showShortcutsTutorialDialog(SOFTWARE | HARDWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        assertTutorialPageSize(2);
    }

    @Test
    public void createTutorialPages_turnOnA11yNavButtonShortcut_linkButtonShownWithText() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);

        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        Button btn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(btn).isNotNull();
        assertThat(btn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(btn.getText().toString()).isEqualTo(
                mContext.getString(
                        R.string.accessibility_tutorial_dialog_configure_software_shortcut_type));
    }

    @Test
    public void createTutorialPages_turnOnFloatingButtonShortcut_linkButtonShownWithText() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);

        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        Button btn = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
        assertThat(btn).isNotNull();
        assertThat(btn.getVisibility()).isEqualTo(View.VISIBLE);
        assertThat(btn.getText().toString()).isEqualTo(
                mContext.getString(R.string.accessibility_tutorial_dialog_link_button));
    }

    @Test
    public void createTutorialPages_turnOnHardwareShortcut_linkButtonGone() {
        showShortcutsTutorialDialog(HARDWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void createTutorialPages_turnOnSoftwareShortcut_showFromSuW_linkButtonGone() {
        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ true);

        AlertDialog alertDialog = assertDialogShown();
        assertThat(alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).getVisibility())
                .isEqualTo(View.GONE);
    }

    @Test
    public void createAccessibilityTutorialDialog_qsShortcut_inSuwTalkbackOn_verifyText() {
        setTouchExplorationEnabled(true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 2);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence instruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);
        final SpannableStringBuilder expectedInstruction = new SpannableStringBuilder();
        expectedInstruction
                .append(mContext.getText(
                        R.string.accessibility_tutorial_dialog_shortcut_unavailable_in_suw))
                .append("\n\n");
        expectedInstruction.append(instruction);

        showShortcutsTutorialDialog(QUICK_SETTINGS, FAKE_FEATURE_NAME, /* isInSetupWizard= */ true);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_qsShortcut_notInSuwTalkbackOn_verifyText() {
        setTouchExplorationEnabled(true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 2);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence expectedInstruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);

        showShortcutsTutorialDialog(QUICK_SETTINGS, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_qsShortcut_inSuwTalkbackOff_verifyText() {
        setTouchExplorationEnabled(false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 1);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence instruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);
        final SpannableStringBuilder expectedInstruction = new SpannableStringBuilder();
        expectedInstruction.append(mContext.getText(
                        R.string.accessibility_tutorial_dialog_shortcut_unavailable_in_suw))
                .append("\n\n");
        expectedInstruction.append(instruction);

        showShortcutsTutorialDialog(QUICK_SETTINGS, FAKE_FEATURE_NAME, /* isInSetupWizard= */ true);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_qsShortcut_notInSuwTalkbackOff_verifyText() {
        setTouchExplorationEnabled(false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_quick_setting);
        Map<String, Object> arguments = new ArrayMap<>();
        arguments.put("count", 1);
        arguments.put("featureName", FAKE_FEATURE_NAME);
        final CharSequence expectedInstruction = StringUtil.getIcuPluralsString(mContext,
                arguments,
                R.string.accessibility_tutorial_dialog_message_quick_setting);

        showShortcutsTutorialDialog(QUICK_SETTINGS, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_volumeKeysShortcut_verifyText() {
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_volume);
        final CharSequence expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_volume);

        showShortcutsTutorialDialog(HARDWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    public void createAccessibilityTutorialDialog_tripleTapShortcut_verifyText() {
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_triple);
        final CharSequence expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_tripletap_instruction, 3);

        showShortcutsTutorialDialog(TRIPLETAP, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction.toString());
    }

    @Test
    @EnableFlags(Flags.FLAG_ENABLE_MAGNIFICATION_MULTIPLE_FINGER_MULTIPLE_TAP_GESTURE)
    public void createAccessibilityTutorialDialog_twoFingerDoubleTapShortcut_verifyText() {
        final int numFingers = 2;
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_two_finger_double, numFingers);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_twofinger_doubletap_instruction, numFingers);

        showShortcutsTutorialDialog(TWOFINGER_DOUBLETAP, FAKE_FEATURE_NAME, /* isInSetupWizard= */
                false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_floatingButtonShortcut_verifyText() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ true);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_button);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_floating_button);

        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_navA11yButtonShortcut_verifyText() {
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ false, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_button);
        final String expectedInstruction = mContext.getString(
                R.string.accessibility_tutorial_dialog_message_button);

        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_gestureShortcut_talkbackOn_flag_verifyText() {
        setTouchExplorationEnabled(true);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_gesture);
        final String expectedInstruction = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 3,
                R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);

        showShortcutsTutorialDialog(GESTURE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void createAccessibilityTutorialDialog_gestureShortcut_talkbackOff_flag_verifyText() {
        setTouchExplorationEnabled(false);
        AccessibilityTestUtils.setSoftwareShortcutMode(
                mContext, /* gestureNavEnabled= */ true, /* floatingButtonEnabled= */ false);
        final String expectedTitle = mContext.getString(
                R.string.accessibility_tutorial_dialog_title_gesture);
        final String expectedInstruction = StringUtil.getIcuPluralsString(
                mContext,
                /* count= */ 2,
                R.string.accessibility_tutorial_dialog_gesture_shortcut_instruction);

        showShortcutsTutorialDialog(GESTURE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        verifyTutorialTitleAndInstruction(
                alertDialog,
                expectedTitle,
                expectedInstruction);
    }

    @Test
    public void performClickOnPositiveButton_turnOnSoftwareShortcut_dismiss() {
        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
        ShadowLooper.idleMainLooper();

        assertThat(alertDialog.isShowing()).isFalse();
    }

    @Test
    public void performClickOnNegativeButton_turnOnSoftwareShortcut_directToSettingsPage() {
        showShortcutsTutorialDialog(SOFTWARE, FAKE_FEATURE_NAME, /* isInSetupWizard= */ false);

        AlertDialog alertDialog = assertDialogShown();
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).performClick();
        ShadowLooper.idleMainLooper();

        final Intent intent = shadowOf(
                (ContextWrapper) alertDialog.getContext()).peekNextStartedActivity();
        assertThat(intent.getComponent().getClassName()).isEqualTo(SubSettings.class.getName());
        assertThat(intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT))
                .isEqualTo(AccessibilityButtonFragment.class.getName());
        assertThat(intent.getIntExtra(MetricsFeatureProvider.EXTRA_SOURCE_METRICS_CATEGORY, -1))
                .isEqualTo(SettingsEnums.SWITCH_SHORTCUT_DIALOG_ACCESSIBILITY_BUTTON_SETTINGS);
    }

    private void setTouchExplorationEnabled(boolean enable) {
        ShadowAccessibilityManager am = shadowOf(
                mContext.getSystemService(AccessibilityManager.class));
        am.setTouchExplorationEnabled(enable);
    }

    private void verifyTutorialTitleAndInstruction(AlertDialog alertDialog, String expectedTitle,
            String expectedInstruction) {
        TextSwitcher titleView = alertDialog.findViewById(R.id.title);
        assertThat(titleView).isNotNull();
        assertThat(((TextView) titleView.getCurrentView()).getText().toString()).isEqualTo(
                expectedTitle);
        TextSwitcher instructionView = alertDialog.findViewById(R.id.instruction);
        assertThat(instructionView).isNotNull();
        assertThat(((TextView) instructionView.getCurrentView()).getText().toString()).isEqualTo(
                expectedInstruction);
    }

    private void showShortcutsTutorialDialog(@ShortcutConstants.UserShortcutType int shortcutTypes,
            CharSequence featureName, boolean isInSetupWizard) {
        FragmentManager fragmentManager = getFragmentManager();
        AccessibilityShortcutsTutorial.DialogFragment.showDialog(
                fragmentManager, shortcutTypes, featureName, isInSetupWizard);
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();
        Fragment frag = fragmentManager.findFragmentByTag(
                AccessibilityShortcutsTutorial.DialogFragment.class.getSimpleName());
        assertThat(frag).isNotNull();
    }

    private FragmentManager getFragmentManager() {
        final FragmentManager[] fragmentManager = {null};
        mFragmentScenario.onFragment(fragment ->
                fragmentManager[0] = fragment.getChildFragmentManager());
        return fragmentManager[0];
    }

    private AlertDialog assertDialogShown() {
        Dialog alertDialog = ShadowDialog.getLatestDialog();
        assertThat(alertDialog).isNotNull();
        assertThat(alertDialog).isInstanceOf(AlertDialog.class);
        return (AlertDialog) alertDialog;
    }

    private void assertTutorialPageSize(int size) {
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertThat(ShadowDialog.getLatestDialog()).isNotNull();
        ViewPager viewPager = dialog.findViewById(R.id.view_pager);
        assertThat(viewPager).isNotNull();
        assertThat(viewPager.getAdapter()).isNotNull();
        assertThat(viewPager.getAdapter().getCount()).isEqualTo(size);
    }
}
