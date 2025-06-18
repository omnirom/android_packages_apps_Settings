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

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.provider.Settings;
import android.provider.Settings.Secure.AccessibilityMagnificationCursorFollowingMode;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.Preconditions;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.android.settings.DialogCreatable;
import com.android.settings.R;
import com.android.settings.accessibility.AccessibilityDialogUtils.DialogEnums;
import com.android.settings.core.BasePreferenceController;

import java.util.ArrayList;
import java.util.List;

/**
 * Controller that shows the magnification cursor following mode and the preference click behavior.
 */
public class MagnificationCursorFollowingModePreferenceController extends
        BasePreferenceController implements DialogCreatable {
    static final String PREF_KEY =
            Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE;

    private static final String TAG =
            MagnificationCursorFollowingModePreferenceController.class.getSimpleName();

    private final List<ModeInfo> mModeList = new ArrayList<>();
    @Nullable
    private DialogHelper mDialogHelper;
    @VisibleForTesting
    @Nullable
    ListView mModeListView;
    @Nullable
    private Preference mModePreference;

    public MagnificationCursorFollowingModePreferenceController(@NonNull Context context,
            @NonNull String preferenceKey) {
        super(context, preferenceKey);
        initModeList();
    }

    public void setDialogHelper(@NonNull DialogHelper dialogHelper) {
        mDialogHelper = dialogHelper;
    }

    private void initModeList() {
        mModeList.add(new ModeInfo(mContext.getString(
                R.string.accessibility_magnification_cursor_following_continuous),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS));
        mModeList.add(new ModeInfo(
                mContext.getString(R.string.accessibility_magnification_cursor_following_center),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER));
        mModeList.add(new ModeInfo(
                mContext.getString(R.string.accessibility_magnification_cursor_following_edge),
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE));
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @NonNull
    @Override
    public CharSequence getSummary() {
        return getCursorFollowingModeSummary(getCurrentMagnificationCursorFollowingMode());
    }

    @Override
    public void displayPreference(@NonNull PreferenceScreen screen) {
        super.displayPreference(screen);
        mModePreference = screen.findPreference(getPreferenceKey());
    }

    @Override
    public boolean handlePreferenceTreeClick(@NonNull Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey()) || mModePreference == null) {
            return super.handlePreferenceTreeClick(preference);
        }

        Preconditions.checkNotNull(mDialogHelper).showDialog(
                    DialogEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING_MODE);
        return true;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(int dialogId) {
        Preconditions.checkArgument(
                dialogId == DialogEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING_MODE,
                "This only handles cursor following mode dialog");
        return createMagnificationCursorFollowingModeDialog();
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        Preconditions.checkArgument(
                dialogId == DialogEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING_MODE,
                "This only handles cursor following mode dialog");
        return SettingsEnums.DIALOG_MAGNIFICATION_CURSOR_FOLLOWING;
    }

    @NonNull
    private Dialog createMagnificationCursorFollowingModeDialog() {
        mModeListView = AccessibilityDialogUtils.createSingleChoiceListView(mContext, mModeList,
                /* itemListener= */null);
        final View headerView = LayoutInflater.from(mContext).inflate(
                R.layout.accessibility_dialog_header, mModeListView,
                /* attachToRoot= */false);
        final TextView textView = Preconditions.checkNotNull(headerView.findViewById(
                R.id.accessibility_dialog_header_text_view));
        textView.setText(
                mContext.getString(R.string.accessibility_magnification_cursor_following_header));
        textView.setVisibility(View.VISIBLE);
        mModeListView.addHeaderView(headerView, /* data= */null, /* isSelectable= */false);
        final int selectionIndex = computeSelectionIndex();
        if (selectionIndex != AdapterView.INVALID_POSITION) {
            mModeListView.setItemChecked(selectionIndex, /* value= */true);
        }
        final CharSequence title = mContext.getString(
                R.string.accessibility_magnification_cursor_following_title);
        final CharSequence positiveBtnText = mContext.getString(R.string.save);
        final CharSequence negativeBtnText = mContext.getString(R.string.cancel);
        return AccessibilityDialogUtils.createCustomDialog(mContext, title, mModeListView,
                positiveBtnText,
                this::onMagnificationCursorFollowingModeDialogPositiveButtonClicked,
                negativeBtnText, /* negativeListener= */null);
    }

    void onMagnificationCursorFollowingModeDialogPositiveButtonClicked(
            DialogInterface dialogInterface, int which) {
        ListView listView = Preconditions.checkNotNull(mModeListView);
        final int selectionIndex = listView.getCheckedItemPosition();
        if (selectionIndex == AdapterView.INVALID_POSITION) {
            Log.w(TAG, "Selected positive button with INVALID_POSITION index");
            return;
        }
        ModeInfo cursorFollowingMode = (ModeInfo) listView.getItemAtPosition(selectionIndex);
        if (cursorFollowingMode != null) {
            Preconditions.checkNotNull(mModePreference).setSummary(
                    getCursorFollowingModeSummary(cursorFollowingMode.mMode));
            Settings.Secure.putInt(mContext.getContentResolver(), PREF_KEY,
                    cursorFollowingMode.mMode);
        }
    }

    private int computeSelectionIndex() {
        ListView listView = Preconditions.checkNotNull(mModeListView);
        @AccessibilityMagnificationCursorFollowingMode
        final int currentMode = getCurrentMagnificationCursorFollowingMode();
        for (int i = 0; i < listView.getCount(); i++) {
            final ModeInfo mode = (ModeInfo) listView.getItemAtPosition(i);
            if (mode != null && mode.mMode == currentMode) {
                return i;
            }
        }
        return AdapterView.INVALID_POSITION;
    }

    @NonNull
    private CharSequence getCursorFollowingModeSummary(
            @AccessibilityMagnificationCursorFollowingMode int cursorFollowingMode) {
        int stringId = switch (cursorFollowingMode) {
            case Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CENTER ->
                    R.string.accessibility_magnification_cursor_following_center;
            case Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_EDGE ->
                    R.string.accessibility_magnification_cursor_following_edge;
            default ->
                    R.string.accessibility_magnification_cursor_following_continuous;
        };
        return mContext.getString(stringId);
    }

    private @AccessibilityMagnificationCursorFollowingMode int
            getCurrentMagnificationCursorFollowingMode() {
        return Settings.Secure.getInt(mContext.getContentResolver(), PREF_KEY,
                Settings.Secure.ACCESSIBILITY_MAGNIFICATION_CURSOR_FOLLOWING_MODE_CONTINUOUS);
    }

    static class ModeInfo extends ItemInfoArrayAdapter.ItemInfo {
        @AccessibilityMagnificationCursorFollowingMode
        public final int mMode;

        ModeInfo(@NonNull CharSequence title,
                @AccessibilityMagnificationCursorFollowingMode int mode) {
            super(title, /* summary= */null, /* drawableId= */null);
            mMode = mode;
        }
    }
}
