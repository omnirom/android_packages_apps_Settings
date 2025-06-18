/*
 * Copyright (C) 2019 The Android Open Source Project
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

import static com.android.settings.accessibility.AccessibilityUtil.State.OFF;
import static com.android.settings.accessibility.AccessibilityUtil.State.ON;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.accessibility.MagnificationCapabilities.MagnificationMode;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.utils.AnnotationSpan;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnCreate;
import com.android.settingslib.core.lifecycle.events.OnSaveInstanceState;

import java.util.ArrayList;
import java.util.List;

/** Controller that shows the magnification area mode summary and the preference click behavior. */
public class MagnificationModePreferenceController extends BasePreferenceController implements
        DialogCreatable, LifecycleObserver, OnCreate, OnSaveInstanceState {

    static final String PREF_KEY = "screen_magnification_mode";
    static final String EXTRA_MODE = "mode";

    private static final String TAG = MagnificationModePreferenceController.class.getSimpleName();

    @Nullable
    private DialogHelper mDialogHelper;
    // The magnification mode in the dialog.
    @MagnificationMode
    private int mModeCache = MagnificationMode.NONE;
    @Nullable
    private Preference mModePreference;
    @Nullable
    private ShortcutPreference mLinkPreference;

    @VisibleForTesting
    @Nullable
    ListView mMagnificationModesListView;

    private final List<MagnificationModeInfo> mModeInfos = new ArrayList<>();

    public MagnificationModePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        initModeInfos();
    }


    public void setDialogHelper(@NonNull DialogHelper dialogHelper) {
        mDialogHelper = dialogHelper;
    }

    private void initModeInfos() {
        mModeInfos.add(new MagnificationModeInfo(mContext.getText(
                R.string.accessibility_magnification_mode_dialog_option_full_screen), null,
                R.drawable.a11y_magnification_mode_fullscreen, MagnificationMode.FULLSCREEN));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_window),
                null, R.drawable.a11y_magnification_mode_window, MagnificationMode.WINDOW));
        mModeInfos.add(new MagnificationModeInfo(
                mContext.getText(R.string.accessibility_magnification_mode_dialog_option_switch),
                mContext.getText(
                        R.string.accessibility_magnification_area_settings_mode_switch_summary),
                R.drawable.a11y_magnification_mode_switch, MagnificationMode.ALL));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @NonNull
    @Override
    public CharSequence getSummary() {
        final int capabilities = MagnificationCapabilities.getCapabilities(mContext);
        return MagnificationCapabilities.getSummary(mContext, capabilities);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mModeCache = savedInstanceState.getInt(EXTRA_MODE, MagnificationMode.NONE);
        }
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mModePreference = screen.findPreference(getPreferenceKey());
        mLinkPreference = screen.findPreference(
                ToggleScreenMagnificationPreferenceFragment.KEY_MAGNIFICATION_SHORTCUT_PREFERENCE);
        Preconditions.checkNotNull(mModePreference).setOnPreferenceClickListener(preference -> {
            mModeCache = MagnificationCapabilities.getCapabilities(mContext);
            Preconditions.checkNotNull(mDialogHelper).showDialog(
                    DialogEnums.DIALOG_MAGNIFICATION_MODE);
            return true;
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(EXTRA_MODE, mModeCache);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(int dialogId) {
        return switch (dialogId) {
            case DialogEnums.DIALOG_MAGNIFICATION_MODE -> createMagnificationModeDialog();
            case DialogEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING ->
                    createMagnificationTripleTapWarningDialog();
            default -> throw new IllegalArgumentException(
                    "This only handles magnification mode and triple tap warning dialog");
        };
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        return switch (dialogId) {
            case DialogEnums.DIALOG_MAGNIFICATION_MODE ->
                    SettingsEnums.DIALOG_MAGNIFICATION_CAPABILITY;
            case DialogEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING ->
                    SettingsEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING;
            default -> 0;
        };
    }

    @NonNull
    private ListView getMagnificationModesListView() {
        return Preconditions.checkNotNull(mMagnificationModesListView);
    }

    @NonNull
    private Dialog createMagnificationModeDialog() {
        mMagnificationModesListView = AccessibilityDialogUtils.createSingleChoiceListView(
                mContext, mModeInfos, this::onMagnificationModeSelected);

        final View headerView = LayoutInflater.from(mContext).inflate(
                R.layout.accessibility_dialog_header, getMagnificationModesListView(),
                /* attachToRoot= */false);
        final TextView textView = Preconditions.checkNotNull(headerView.findViewById(
                R.id.accessibility_dialog_header_text_view));
        textView.setText(
                mContext.getString(R.string.accessibility_magnification_area_settings_message));
        getMagnificationModesListView().addHeaderView(headerView, /* data= */null,
                /* isSelectable= */false);

        getMagnificationModesListView().setItemChecked(computeSelectionIndex(), /* value= */true);
        final CharSequence title = mContext.getString(
                R.string.accessibility_magnification_mode_dialog_title);
        final CharSequence positiveBtnText = mContext.getString(R.string.save);
        final CharSequence negativeBtnText = mContext.getString(R.string.cancel);

        return AccessibilityDialogUtils.createCustomDialog(mContext, title,
                getMagnificationModesListView(), positiveBtnText,
                this::onMagnificationModeDialogPositiveButtonClicked,
                negativeBtnText, /* negativeListener= */null);
    }

    @VisibleForTesting
    void onMagnificationModeDialogPositiveButtonClicked(@NonNull DialogInterface dialogInterface,
            int which) {
        final int selectedIndex = getMagnificationModesListView().getCheckedItemPosition();
        if (selectedIndex == AdapterView.INVALID_POSITION) {
            Log.w(TAG, "Selected positive button with INVALID_POSITION index");
            return;
        }

        mModeCache = ((MagnificationModeInfo) getMagnificationModesListView().getItemAtPosition(
                selectedIndex)).mMagnificationMode;

        // Do not save mode until user clicks positive button in triple tap warning dialog.
        if (isTripleTapEnabled(mContext) && mModeCache != MagnificationMode.FULLSCREEN) {
            Preconditions.checkNotNull(mDialogHelper).showDialog(
                    DialogEnums.DIALOG_MAGNIFICATION_TRIPLE_TAP_WARNING);
        } else { // Save mode (capabilities) value, don't need to show dialog to confirm.
            updateCapabilitiesAndSummary(mModeCache);
        }
    }

    private void updateCapabilitiesAndSummary(@MagnificationMode int mode) {
        mModeCache = mode;
        MagnificationCapabilities.setCapabilities(mContext, mModeCache);
        Preconditions.checkNotNull(mModePreference).setSummary(
                MagnificationCapabilities.getSummary(mContext, mModeCache));
    }

    private void onMagnificationModeSelected(@NonNull AdapterView<?> parent, @NonNull View view,
            int position, long id) {
        final MagnificationModeInfo modeInfo =
                (MagnificationModeInfo) getMagnificationModesListView().getItemAtPosition(position);
        if (modeInfo.mMagnificationMode == mModeCache) {
            return;
        }
        mModeCache = modeInfo.mMagnificationMode;
    }

    private int computeSelectionIndex() {
        final int modesSize = mModeInfos.size();
        for (int i = 0; i < modesSize; i++) {
            if (mModeInfos.get(i).mMagnificationMode == mModeCache) {
                return i + getMagnificationModesListView().getHeaderViewsCount();
            }
        }
        Log.w(TAG, "Can not find matching mode in mModeInfos");
        return 0;
    }

    @VisibleForTesting
    static boolean isTripleTapEnabled(@NonNull Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_DISPLAY_MAGNIFICATION_ENABLED, OFF) == ON;
    }

    @NonNull
    private Dialog createMagnificationTripleTapWarningDialog() {
        @SuppressWarnings({"InflateParams"})
        final View contentView = LayoutInflater.from(mContext).inflate(
                R.layout.magnification_triple_tap_warning_dialog, /* root= */ null);
        final CharSequence title = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_title);
        final CharSequence positiveBtnText = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_positive_button);
        final CharSequence negativeBtnText = mContext.getString(
                R.string.accessibility_magnification_triple_tap_warning_negative_button);

        final Dialog dialog = AccessibilityDialogUtils.createCustomDialog(mContext, title,
                contentView,
                positiveBtnText, this::onMagnificationTripleTapWarningDialogPositiveButtonClicked,
                negativeBtnText, this::onMagnificationTripleTapWarningDialogNegativeButtonClicked);

        updateLinkInTripleTapWarningDialog(dialog, contentView);

        return dialog;
    }

    private void updateLinkInTripleTapWarningDialog(@NonNull Dialog dialog,
            @NonNull View contentView) {
        final TextView messageView = contentView.findViewById(R.id.message);
        // TODO(b/225682559): Need to remove performClick() after refactoring accessibility dialog.
        final View.OnClickListener linkListener = view -> {
            updateCapabilitiesAndSummary(mModeCache);
            Preconditions.checkNotNull(mLinkPreference).performClick();
            dialog.dismiss();
        };
        final AnnotationSpan.LinkInfo linkInfo = new AnnotationSpan.LinkInfo(
                AnnotationSpan.LinkInfo.DEFAULT_ANNOTATION, linkListener);
        final CharSequence textWithLink = AnnotationSpan.linkify(mContext.getText(
                R.string.accessibility_magnification_triple_tap_warning_message), linkInfo);

        if (messageView != null) {
            messageView.setText(textWithLink);
            messageView.setMovementMethod(LinkMovementMethod.getInstance());
        }
        dialog.setContentView(contentView);
    }

    @VisibleForTesting
    void onMagnificationTripleTapWarningDialogNegativeButtonClicked(
            @NonNull DialogInterface dialogInterface, int which) {
        mModeCache = MagnificationCapabilities.getCapabilities(mContext);
        Preconditions.checkNotNull(mDialogHelper).showDialog(
                DialogEnums.DIALOG_MAGNIFICATION_MODE);
    }

    @VisibleForTesting
    void onMagnificationTripleTapWarningDialogPositiveButtonClicked(
            @NonNull DialogInterface dialogInterface, int which) {
        updateCapabilitiesAndSummary(mModeCache);
    }

    @VisibleForTesting
    static class MagnificationModeInfo extends ItemInfoArrayAdapter.ItemInfo {
        @MagnificationMode
        public final int mMagnificationMode;

        MagnificationModeInfo(@NonNull CharSequence title, @Nullable CharSequence summary,
                @DrawableRes int drawableId, @MagnificationMode int magnificationMode) {
            super(title, summary, drawableId);
            mMagnificationMode = magnificationMode;
        }
    }
}
