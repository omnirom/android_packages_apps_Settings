/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_CONTROLLER_NAME;
import static com.android.internal.accessibility.common.ShortcutConstants.UserShortcutType.DEFAULT;
import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;
import static com.android.settings.accessibility.AccessibilityUtil.getShortcutSummaryList;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.icu.text.MessageFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DeviceConfig;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.util.Preconditions;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.accessibility.util.ShortcutUtils;
import com.android.internal.annotations.VisibleForTesting;
import com.android.server.accessibility.Flags;
import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.search.Indexable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SettingsThemeHelper;

import com.google.android.setupcompat.util.WizardManagerHelper;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Fragment that shows the actual UI for providing basic magnification accessibility service setup
 * and does not have toggle bar to turn on service to use.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ToggleScreenMagnificationPreferenceFragment extends
        ToggleFeaturePreferenceFragment implements DialogHelper {

    private static final String TAG =
            ToggleScreenMagnificationPreferenceFragment.class.getSimpleName();
    @VisibleForTesting
    static final String KEY_MAGNIFICATION_SHORTCUT_PREFERENCE = "magnification_shortcut_preference";
    private static final char COMPONENT_NAME_SEPARATOR = ':';
    private static final TextUtils.SimpleStringSplitter sStringColonSplitter =
            new TextUtils.SimpleStringSplitter(COMPONENT_NAME_SEPARATOR);

    // TODO(b/147021230): Move duplicated functions with android/internal/accessibility into util.
    private TouchExplorationStateChangeListener mTouchExplorationStateChangeListener;
    @Nullable
    private DialogCreatable mMagnificationModeDialogDelegate;
    @Nullable
    private DialogCreatable mMagnificationCursorFollowingModeDialogDelegate;

    @Nullable
    MagnificationOneFingerPanningPreferenceController mOneFingerPanningPreferenceController;

    private boolean mInSetupWizard;

    @VisibleForTesting
    public void setMagnificationModeDialogDelegate(@NonNull DialogCreatable delegate) {
        mMagnificationModeDialogDelegate = delegate;
    }

    @VisibleForTesting
    public void setMagnificationCursorFollowingModeDialogDelegate(
            @NonNull DialogCreatable delegate) {
        mMagnificationCursorFollowingModeDialogDelegate = delegate;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().setTitle(R.string.accessibility_screen_magnification_title);
        mInSetupWizard = WizardManagerHelper.isAnySetupWizard(getIntent());
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @NonNull Bundle savedInstanceState) {
        mFeatureName = getString(R.string.accessibility_screen_magnification_title);
        final boolean useExpressiveTheme = WizardManagerHelper.isAnySetupWizard(getIntent())
                ? ThemeHelper.shouldApplyGlifExpressiveStyle(getPrefContext())
                : SettingsThemeHelper.isExpressiveTheme(getPrefContext());
        mImageUri = new Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(getPrefContext().getPackageName())
                .appendPath(String.valueOf(useExpressiveTheme
                        ? R.raw.accessibility_magnification_banner_expressive
                        : R.raw.a11y_magnification_banner))
                .build();
        mTouchExplorationStateChangeListener = isTouchExplorationEnabled -> {
            mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        };

        final View view = super.onCreateView(inflater, container, savedInstanceState);
        updateFooterPreference();
        return view;
    }

    private void updateFooterPreference() {
        final String title = getPrefContext().getString(
                R.string.accessibility_screen_magnification_about_title);
        final String learnMoreText = getPrefContext().getString(
                R.string.accessibility_screen_magnification_footer_learn_more_content_description);
        mFooterPreferenceController.setIntroductionTitle(title);
        mFooterPreferenceController.setupHelpLink(getHelpResource(), learnMoreText);
        mFooterPreferenceController.displayPreference(getPreferenceScreen());
    }

    @Override
    public void onResume() {
        super.onResume();
        final IllustrationPreference illustrationPreference =
                getPreferenceScreen().findPreference(KEY_ANIMATED_IMAGE);
        if (illustrationPreference != null) {
            illustrationPreference.applyDynamicColor();
        }

        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.addTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);
    }

    @Override
    protected int getPreferenceScreenResId() {
        // TODO(b/171272809): Add back when controllers move to static type
        return 0;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onPause() {
        final AccessibilityManager am = getPrefContext().getSystemService(
                AccessibilityManager.class);
        am.removeTouchExplorationStateChangeListener(mTouchExplorationStateChangeListener);

        super.onPause();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case DialogEnums.DIALOG_MAGNIFICATION_MODE:
            case DialogEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING:
                return Preconditions.checkNotNull(mMagnificationModeDialogDelegate)
                        .onCreateDialog(dialogId);
            case DialogEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING_MODE:
                return Preconditions.checkNotNull(mMagnificationCursorFollowingModeDialogDelegate)
                        .onCreateDialog(dialogId);
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return AccessibilityShortcutsTutorial
                        .showAccessibilityGestureTutorialDialog(getPrefContext());
            default:
                return super.onCreateDialog(dialogId);
        }
    }

    private static boolean isWindowMagnificationSupported(Context context) {
        return context.getResources().getBoolean(
                com.android.internal.R.bool.config_magnification_area)
                && context.getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_WINDOW_MAGNIFICATION);
    }

    private static boolean isMagnificationCursorFollowingModeDialogSupported() {
        return com.android.settings.accessibility.Flags.enableMagnificationCursorFollowingDialog()
                && hasMouse();
    }

    @Override
    protected void initSettingsPreference() {
        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        if (isWindowMagnificationSupported(getContext())) {
            // LINT.IfChange(preference_list)
            addMagnificationModeSetting(generalCategory);
            addFollowTypingSetting(generalCategory);
            addOneFingerPanningSetting(generalCategory);
            addAlwaysOnSetting(generalCategory);
            addJoystickSetting(generalCategory);
            // LINT.ThenChange(:search_data)
        }
        addCursorFollowingSetting(generalCategory);
        addFeedbackSetting(generalCategory);
    }

    @Override
    protected void onProcessArguments(Bundle arguments) {
        Context context = getContext();

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_PREFERENCE_KEY)) {
            arguments.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
                    Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        }

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_INTRO)) {
            arguments.putCharSequence(AccessibilitySettings.EXTRA_INTRO,
                    context.getString(R.string.accessibility_screen_magnification_intro_text));
        }

        if (!arguments.containsKey(AccessibilitySettings.EXTRA_HTML_DESCRIPTION)
                && !Flags.enableMagnificationOneFingerPanningGesture()) {
            String summary = "";
            boolean hasTouchscreen = hasTouchscreen();
            if (Flags.enableMagnificationKeyboardControl() && hasHardKeyboard()) {
                // Include the keyboard summary when a keyboard is plugged in.
                final String meta = context.getString(R.string.modifier_keys_meta);
                final String alt = context.getString(R.string.modifier_keys_alt);
                summary += MessageFormat.format(
                        context.getString(
                                R.string.accessibility_screen_magnification_keyboard_summary,
                                meta, alt, meta, alt),
                        new Object[]{1, 2, 3, 4});
                if (hasTouchscreen) {
                    // Add a newline before the touchscreen text.
                    summary += "<br/><br/>";
                }

            }
            if (hasTouchscreen || TextUtils.isEmpty(summary)) {
                // Always show the touchscreen summary if there is no summary yet, even if the
                // touchscreen is missing.
                // If the keyboard summary is present and there is no touchscreen, then we can
                // ignore the touchscreen summary.
                summary += MessageFormat.format(
                        context.getString(R.string.accessibility_screen_magnification_summary),
                        new Object[]{1, 2, 3, 4, 5});
            }
            arguments.putCharSequence(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, summary);
        }

        super.onProcessArguments(arguments);
    }

    private static Preference createMagnificationModePreference(Context context) {
        final Preference pref = new Preference(context);
        pref.setTitle(R.string.accessibility_magnification_mode_title);
        pref.setKey(MagnificationModePreferenceController.PREF_KEY);
        pref.setPersistent(false);
        return pref;
    }

    private void addMagnificationModeSetting(PreferenceCategory generalCategory) {
        mSettingsPreference = createMagnificationModePreference(getPrefContext());
        generalCategory.addPreference(mSettingsPreference);

        final MagnificationModePreferenceController magnificationModePreferenceController =
                new MagnificationModePreferenceController(Preconditions.checkNotNull(getContext()),
                        MagnificationModePreferenceController.PREF_KEY);
        magnificationModePreferenceController.setDialogHelper(/* dialogHelper= */this);
        mMagnificationModeDialogDelegate = magnificationModePreferenceController;
        getSettingsLifecycle().addObserver(magnificationModePreferenceController);
        magnificationModePreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(magnificationModePreferenceController);
    }

    private static Preference createCursorFollowingPreference(Context context) {
        final Preference pref = new Preference(context);
        pref.setTitle(R.string.accessibility_magnification_cursor_following_title);
        pref.setKey(MagnificationCursorFollowingModePreferenceController.PREF_KEY);
        pref.setPersistent(false);
        return pref;
    }

    private void addCursorFollowingSetting(PreferenceCategory generalCategory) {
        if (!isMagnificationCursorFollowingModeDialogSupported()) {
            return;
        }

        generalCategory.addPreference(createCursorFollowingPreference(getPrefContext()));

        final MagnificationCursorFollowingModePreferenceController controller =
                new MagnificationCursorFollowingModePreferenceController(
                        getContext(),
                        MagnificationCursorFollowingModePreferenceController.PREF_KEY);
        controller.setDialogHelper(/* dialogHelper= */this);
        mMagnificationCursorFollowingModeDialogDelegate = controller;
        controller.displayPreference(getPreferenceScreen());
        addPreferenceController(controller);
    }

    private static Preference createFollowTypingPreference(Context context) {
        final Preference pref = new SwitchPreferenceCompat(context);
        pref.setTitle(R.string.accessibility_screen_magnification_follow_typing_title);
        pref.setSummary(R.string.accessibility_screen_magnification_follow_typing_summary);
        pref.setKey(MagnificationFollowTypingPreferenceController.PREF_KEY);
        return pref;
    }

    private void addFollowTypingSetting(PreferenceCategory generalCategory) {
        generalCategory.addPreference(createFollowTypingPreference(getPrefContext()));

        var followTypingPreferenceController = new MagnificationFollowTypingPreferenceController(
                getContext(), MagnificationFollowTypingPreferenceController.PREF_KEY);
        followTypingPreferenceController.setInSetupWizard(mInSetupWizard);
        followTypingPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(followTypingPreferenceController);
    }

    private static boolean isAlwaysOnSupported(Context context) {
        final boolean defaultValue = context.getResources().getBoolean(
                com.android.internal.R.bool.config_magnification_always_on_enabled);

        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                "AlwaysOnMagnifier__enable_always_on_magnifier",
                defaultValue
        );
    }

    private static Preference createAlwaysOnPreference(Context context) {
        final Preference pref = new SwitchPreferenceCompat(context);
        pref.setTitle(R.string.accessibility_screen_magnification_always_on_title);
        pref.setSummary(R.string.accessibility_screen_magnification_always_on_summary);
        pref.setKey(MagnificationAlwaysOnPreferenceController.PREF_KEY);
        return pref;
    }

    private void addAlwaysOnSetting(PreferenceCategory generalCategory) {
        if (!isAlwaysOnSupported(getContext())) {
            return;
        }

        final Preference pref = createAlwaysOnPreference(getPrefContext());
        generalCategory.addPreference(pref);

        var alwaysOnPreferenceController = new MagnificationAlwaysOnPreferenceController(
                getContext(), MagnificationAlwaysOnPreferenceController.PREF_KEY);
        alwaysOnPreferenceController.setInSetupWizard(mInSetupWizard);
        getSettingsLifecycle().addObserver(alwaysOnPreferenceController);
        alwaysOnPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(alwaysOnPreferenceController);
    }

    private static Preference createOneFingerPanningPreference(Context context) {
        final Preference pref = new SwitchPreferenceCompat(context);
        pref.setTitle(R.string.accessibility_magnification_one_finger_panning_title);
        pref.setKey(MagnificationOneFingerPanningPreferenceController.PREF_KEY);
        return pref;
    }

    private static boolean isOneFingerPanningSupported() {
        return Flags.enableMagnificationOneFingerPanningGesture();
    }

    private void addOneFingerPanningSetting(PreferenceCategory generalCategory) {
        if (!isOneFingerPanningSupported()) {
            return;
        }

        final Preference pref = createOneFingerPanningPreference(getPrefContext());
        generalCategory.addPreference(pref);

        mOneFingerPanningPreferenceController =
                new MagnificationOneFingerPanningPreferenceController(getContext());
        mOneFingerPanningPreferenceController.setInSetupWizard(mInSetupWizard);
        getSettingsLifecycle().addObserver(mOneFingerPanningPreferenceController);
        mOneFingerPanningPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(mOneFingerPanningPreferenceController);
    }

    private static Preference createJoystickPreference(Context context) {
        final Preference pref = new SwitchPreferenceCompat(context);
        pref.setTitle(R.string.accessibility_screen_magnification_joystick_title);
        pref.setSummary(R.string.accessibility_screen_magnification_joystick_summary);
        pref.setKey(MagnificationJoystickPreferenceController.PREF_KEY);
        return pref;
    }

    private static Preference createFeedbackPreference(Context context) {
        final Preference pref = new Preference(context);
        pref.setTitle(R.string.accessibility_feedback_title);
        pref.setSummary(R.string.accessibility_feedback_summary);
        pref.setKey(MagnificationFeedbackPreferenceController.PREF_KEY);
        return pref;
    }

    private static boolean isJoystickSupported() {
        return DeviceConfig.getBoolean(
                DeviceConfig.NAMESPACE_WINDOW_MANAGER,
                "MagnificationJoystick__enable_magnification_joystick",
                false);
    }

    private void addJoystickSetting(PreferenceCategory generalCategory) {
        if (!isJoystickSupported()) {
            return;
        }

        final Preference pref = createJoystickPreference(getPrefContext());
        generalCategory.addPreference(pref);

        MagnificationJoystickPreferenceController joystickPreferenceController =
                new MagnificationJoystickPreferenceController(
                        getContext(),
                        MagnificationJoystickPreferenceController.PREF_KEY
                );
        joystickPreferenceController.setInSetupWizard(mInSetupWizard);
        joystickPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(joystickPreferenceController);
    }

    private void addFeedbackSetting(PreferenceCategory generalCategory) {
        if (!Flags.enableLowVisionHats()) {
            return;
        }

        final Preference feedbackPreference = createFeedbackPreference(getPrefContext());
        generalCategory.addPreference(feedbackPreference);

        final MagnificationFeedbackPreferenceController magnificationFeedbackPreferenceController =
                new MagnificationFeedbackPreferenceController(getContext(), this,
                        MagnificationFeedbackPreferenceController.PREF_KEY);
        magnificationFeedbackPreferenceController.setInSetupWizard(mInSetupWizard);
        magnificationFeedbackPreferenceController.displayPreference(getPreferenceScreen());
        addPreferenceController(magnificationFeedbackPreferenceController);
    }

    @Override
    public void showDialog(int dialogId) {
        super.showDialog(dialogId);
    }

    @Override
    protected void registerKeysToObserverCallback(
            AccessibilitySettingsContentObserver contentObserver) {
        super.registerKeysToObserverCallback(contentObserver);

        var keysToObserve = List.of(
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_FOLLOW_TYPING_ENABLED,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_ALWAYS_ON_ENABLED,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_JOYSTICK_ENABLED
        );
        contentObserver.registerKeysToObserverCallback(keysToObserve,
                key -> updatePreferencesState());

        if (Flags.enableMagnificationOneFingerPanningGesture()) {
            contentObserver.registerKeysToObserverCallback(
                    List.of(Settings.Secure.ACCESSIBILITY_SINGLE_FINGER_PANNING_ENABLED),
                    key -> updateHtmlTextPreference());
        }
    }

    private void updatePreferencesState() {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        getPreferenceControllers().forEach(controllers::addAll);
        controllers.forEach(controller -> controller.updateState(
                findPreference(controller.getPreferenceKey())));
    }

    @Override
    CharSequence getCurrentHtmlDescription() {
        CharSequence origin = super.getCurrentHtmlDescription();
        if (!TextUtils.isEmpty(origin)) {
            // If in ToggleFeaturePreferenceFragment we already have a fixed html description, we
            // should use the fixed one, otherwise we'll dynamically decide the description.
            return origin;
        }

        Context context = getContext();
        if (mOneFingerPanningPreferenceController != null && context != null) {
            @StringRes int resId = mOneFingerPanningPreferenceController.isChecked()
                    ? R.string.accessibility_screen_magnification_summary_one_finger_panning_on
                    : R.string.accessibility_screen_magnification_summary_one_finger_panning_off;
            return MessageFormat.format(context.getString(resId), new Object[]{1, 2, 3, 4, 5});
        }
        return "";
    }

    @Override
    protected List<String> getShortcutFeatureSettingsKeys() {
        final List<String> shortcutKeys = super.getShortcutFeatureSettingsKeys();
        shortcutKeys.add(Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED);
        return shortcutKeys;
    }

    @Override
    protected CharSequence getShortcutTypeSummary(Context context) {
        if (!mShortcutPreference.isChecked()) {
            return context.getText(R.string.switch_off_text);
        }

        return getShortcutSummaryList(context,
                PreferredShortcuts.retrieveUserShortcutType(context,
                        MAGNIFICATION_CONTROLLER_NAME));
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_magnification;
    }

    @Override
    public int getMetricsCategory() {
        // TODO: Distinguish between magnification modes
        return SettingsEnums.ACCESSIBILITY_TOGGLE_SCREEN_MAGNIFICATION;
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DialogEnums.DIALOG_MAGNIFICATION_MODE:
            case DialogEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING:
                return Preconditions.checkNotNull(mMagnificationModeDialogDelegate)
                        .getDialogMetricsCategory(dialogId);
            case DialogEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING_MODE:
                return Preconditions.checkNotNull(mMagnificationCursorFollowingModeDialogDelegate)
                        .getDialogMetricsCategory(dialogId);
            case DialogEnums.GESTURE_NAVIGATION_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_GESTURE_NAVIGATION;
            case DialogEnums.ACCESSIBILITY_BUTTON_TUTORIAL:
                return SettingsEnums.DIALOG_TOGGLE_SCREEN_MAGNIFICATION_ACCESSIBILITY_BUTTON;
            default:
                return super.getDialogMetricsCategory(dialogId);
        }
    }

    @Override
    int getUserShortcutTypes() {
        return ShortcutUtils.getEnabledShortcutTypes(
                getPrefContext(), MAGNIFICATION_CONTROLLER_NAME);
    }

    @Override
    ComponentName getTileComponentName() {
        return null;
    }

    @Override
    protected void onPreferenceToggled(String preferenceKey, boolean enabled) {
        if (enabled && TextUtils.equals(
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_NAVBAR_ENABLED,
                preferenceKey)) {
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        Settings.Secure.putInt(getContentResolver(), preferenceKey, enabled ? ON : OFF);
    }

    @Override
    protected void onInstallSwitchPreferenceToggleSwitch() {
        mToggleServiceSwitchPreference.setVisible(false);
    }

    @Override
    public void onToggleClicked(@NonNull ShortcutPreference preference) {
        final int shortcutTypes = getUserPreferredShortcutTypes();
        getPrefContext().getSystemService(AccessibilityManager.class).enableShortcutsForTargets(
                preference.isChecked(), shortcutTypes,
                Set.of(MAGNIFICATION_CONTROLLER_NAME), getPrefContext().getUserId()
        );
        if (preference.isChecked()) {
            showDialog(DialogEnums.LAUNCH_ACCESSIBILITY_TUTORIAL);
        }
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    @Override
    public void onSettingsClicked(@NonNull ShortcutPreference preference) {
        EditShortcutsPreferenceFragment.showEditShortcutScreen(
                requireContext(),
                getMetricsCategory(),
                getShortcutTitle(),
                MAGNIFICATION_COMPONENT_NAME,
                getIntent());
    }

    @Override
    protected void updateShortcutPreferenceData() {
        final int shortcutTypes = ShortcutUtils.getEnabledShortcutTypes(
                getPrefContext(), MAGNIFICATION_CONTROLLER_NAME);
        if (shortcutTypes != DEFAULT) {
            final PreferredShortcut shortcut = new PreferredShortcut(
                    MAGNIFICATION_CONTROLLER_NAME, shortcutTypes);
            PreferredShortcuts.saveUserShortcutType(getPrefContext(), shortcut);
        }
    }

    @Override
    protected void initShortcutPreference() {
        mShortcutPreference = new ShortcutPreference(getPrefContext(), null);
        mShortcutPreference.setPersistent(false);
        mShortcutPreference.setKey(getShortcutPreferenceKey());
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
        mShortcutPreference.setOnClickCallback(this);
        mShortcutPreference.setTitle(getShortcutTitle());

        final PreferenceCategory generalCategory = findPreference(KEY_GENERAL_CATEGORY);
        generalCategory.addPreference(mShortcutPreference);
    }

    @Override
    protected String getShortcutPreferenceKey() {
        return KEY_MAGNIFICATION_SHORTCUT_PREFERENCE;
    }

    @Override
    protected CharSequence getShortcutTitle() {
        return getText(R.string.accessibility_screen_magnification_shortcut_title);
    }

    @Override
    protected void updateShortcutPreference() {
        mShortcutPreference.setChecked(getUserShortcutTypes() != DEFAULT);
        mShortcutPreference.setSummary(getShortcutTypeSummary(getPrefContext()));
    }

    /**
     * Gets the service summary of magnification.
     *
     * @param context The current context.
     */
    @NonNull
    public static CharSequence getServiceSummary(@NonNull Context context) {
        // Get the user shortcut type from settings provider.
        final int userShortcutType = ShortcutUtils.getEnabledShortcutTypes(
                context, MAGNIFICATION_CONTROLLER_NAME);
        final CharSequence featureState =
                (userShortcutType != DEFAULT)
                        ? context.getText(R.string.accessibility_summary_shortcut_enabled)
                        : context.getText(R.string.generic_accessibility_feature_shortcut_off);
        final CharSequence featureSummary = context.getText(R.string.magnification_feature_summary);
        return context.getString(
                com.android.settingslib.R.string.preference_summary_default_combination,
                featureState, featureSummary);
    }

    @Override
    protected int getUserPreferredShortcutTypes() {
        return PreferredShortcuts.retrieveUserShortcutType(
                getPrefContext(), MAGNIFICATION_CONTROLLER_NAME);
    }

    /**
     * Returns if a mouse is attached.
     */
    private static boolean hasMouse() {
        final int[] devices = InputDevice.getDeviceIds();
        for (int i = 0; i < devices.length; i++) {
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device != null && (device.getSources() & InputDevice.SOURCE_MOUSE)
                    == InputDevice.SOURCE_MOUSE) {
                return true;
            }
        }
        return false;
    }

    private boolean hasHardKeyboard() {
        final int[] devices = InputDevice.getDeviceIds();
        for (int i = 0; i < devices.length; i++) {
            InputDevice device = InputDevice.getDevice(devices[i]);
            if (device == null || device.isVirtual() || !device.isFullKeyboard()) {
                continue;
            }

            return true;
        }
        return false;
    }

    private boolean hasTouchscreen() {
        return getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN)
                || getPackageManager().hasSystemFeature(PackageManager.FEATURE_FAKETOUCH);
    }

    public static final Indexable.SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                // LINT.IfChange(search_data)
                @NonNull
                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(@NonNull Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> rawData =
                            super.getRawDataToIndex(context, enabled);

                    if (!com.android.settings.accessibility.Flags.fixA11ySettingsSearch()) {
                        return rawData;
                    }

                    // Add all preferences to search raw data so that they are included in
                    // indexing, which happens infrequently. Irrelevant preferences should be
                    // hidden from the live returned search results by `getNonIndexableKeys`,
                    // which is called every time a search occurs. This allows for dynamic search
                    // entries that hide or show depending on current device state.
                    rawData.add(createShortcutPreferenceSearchData(context));
                    Stream.of(
                                    createMagnificationModePreference(context),
                                    createFollowTypingPreference(context),
                                    createOneFingerPanningPreference(context),
                                    createAlwaysOnPreference(context),
                                    createJoystickPreference(context),
                                    createCursorFollowingPreference(context),
                                    createFeedbackPreference(context)
                            )
                            .forEach(pref ->
                                    rawData.add(createPreferenceSearchData(context, pref)));
                    return rawData;
                }

                @NonNull
                @Override
                public List<String> getNonIndexableKeys(@NonNull Context context) {
                    final List<String> niks = super.getNonIndexableKeys(context);

                    if (!com.android.settings.accessibility.Flags.fixA11ySettingsSearch()) {
                        return niks;
                    }

                    if (!isWindowMagnificationSupported(context)) {
                        niks.add(MagnificationModePreferenceController.PREF_KEY);
                        niks.add(MagnificationFollowTypingPreferenceController.PREF_KEY);
                        niks.add(MagnificationOneFingerPanningPreferenceController.PREF_KEY);
                        niks.add(MagnificationAlwaysOnPreferenceController.PREF_KEY);
                        niks.add(MagnificationJoystickPreferenceController.PREF_KEY);
                    } else {
                        if (!isAlwaysOnSupported(context)
                                // This preference's title "Keep on while switching apps" does not
                                // mention magnification so it may confuse users who search a term
                                // like "Keep on".
                                // So we hide it if the user has no magnification shortcut enabled.
                                || ShortcutUtils.getEnabledShortcutTypes(
                                        context, MAGNIFICATION_CONTROLLER_NAME) == DEFAULT) {
                            niks.add(MagnificationAlwaysOnPreferenceController.PREF_KEY);
                        }
                        if (!isOneFingerPanningSupported()) {
                            niks.add(MagnificationOneFingerPanningPreferenceController.PREF_KEY);
                        }
                        if (!isJoystickSupported()) {
                            niks.add(MagnificationJoystickPreferenceController.PREF_KEY);
                        }
                    }

                    if (!isMagnificationCursorFollowingModeDialogSupported()) {
                        niks.add(MagnificationCursorFollowingModePreferenceController.PREF_KEY);
                    }

                    if (!Flags.enableLowVisionHats()) {
                        niks.add(MagnificationFeedbackPreferenceController.PREF_KEY);
                    }

                    return niks;
                }
                // LINT.ThenChange(:preference_list)

                private SearchIndexableRaw createPreferenceSearchData(
                        Context context, Preference pref) {
                    final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.key = pref.getKey();
                    raw.title = pref.getTitle().toString();
                    return raw;
                }

                private SearchIndexableRaw createShortcutPreferenceSearchData(Context context) {
                    final SearchIndexableRaw raw = new SearchIndexableRaw(context);
                    raw.key = KEY_MAGNIFICATION_SHORTCUT_PREFERENCE;
                    raw.title = context.getString(
                            R.string.accessibility_screen_magnification_shortcut_title);
                    return raw;
                }
            };
}
