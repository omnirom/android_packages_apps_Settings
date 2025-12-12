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

import static com.android.settings.Utils.createAccessibleSequence;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.util.SparseArray;
import android.view.Display.Mode;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.ToBooleanFunction;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ResolutionPreferenceFragment extends SettingsPreferenceFragmentBase {
    @VisibleForTesting static final int TOP_MODE_RES_MAX_COUNT = 3;
    @VisibleForTesting static final String MORE_OPTIONS_KEY = "more_options";
    @VisibleForTesting static final String TOP_OPTIONS_KEY = "top_options";

    @VisibleForTesting
    static final int EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE =
            R.xml.external_display_resolution_settings;

    private static final String TAG = "ResolutionPreference";
    private static final int MORE_OPTIONS_TITLE_RESOURCE =
            R.string.external_display_more_options_title;
    private static final int DEFAULT_LOW_REFRESH_RATE = 60;

    @Nullable private ConnectedDisplayInjector mInjector;
    @Nullable private PreferenceCategory mTopOptionsPreference;
    @Nullable private PreferenceCategory mMoreOptionsPreference;
    @Nullable private DisplayDevice mDisplay;
    private boolean mStarted;
    // Maps a resolution preference key (e.g., "1920x1080") to its corresponding Display.Mode.
    private final Map<String, Mode> mResolutionPreferences = new HashMap<>();
    private final SparseArray<Mode> mAvailableModes = new SparseArray<>();
    private int mExternalDisplayPeakWidth;
    private int mExternalDisplayPeakHeight;
    private int mExternalDisplayPeakRefreshRate;
    private final Set<Point> mExternDisplayResolutionShown = new HashSet<>();
    private boolean mRefreshRateSynchronizationEnabled;
    private boolean mMoreOptionsExpanded;
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener =
            new DisplayListener() {
                @Override
                public void update(int displayId) {
                    scheduleUpdate();
                }
            };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_EXTERNAL_DISPLAY_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return EXTERNAL_DISPLAY_HELP_URL;
    }

    @Override
    public void onCreateCallback(@Nullable Bundle icicle) {
        if (mInjector == null) {
            mInjector = new ConnectedDisplayInjector(getPrefContext());
        }
        addPreferencesFromResource(EXTERNAL_DISPLAY_RESOLUTION_SETTINGS_RESOURCE);
        updateDisplayModeLimits(mInjector.getContext());
        setupResolutionApplyConfirmationHandler();
    }

    @Override
    public void onActivityCreatedCallback(@Nullable Bundle savedInstanceState) {
        View view = getView();
        TextView emptyView = null;
        if (view != null) {
            emptyView = (TextView) view.findViewById(android.R.id.empty);
        }
        if (emptyView != null) {
            emptyView.setText(EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE);
            setEmptyView(emptyView);
        }
    }

    @Override
    public void onStartCallback() {
        mStarted = true;
        if (mInjector == null) {
            return;
        }
        mInjector.registerDisplayListener(mListener);
        scheduleUpdate();
    }

    @Override
    public void onStopCallback() {
        mStarted = false;
        if (mInjector == null) {
            return;
        }
        mInjector.unregisterDisplayListener(mListener);
        unscheduleUpdate();
    }

    public ResolutionPreferenceFragment() {}

    @VisibleForTesting
    ResolutionPreferenceFragment(@NonNull ConnectedDisplayInjector injector) {
        mInjector = injector;
    }

    @VisibleForTesting
    protected int getDisplayIdArg() {
        var args = getArguments();
        return args != null ? args.getInt(DISPLAY_ID_ARG, INVALID_DISPLAY) : INVALID_DISPLAY;
    }

    @VisibleForTesting
    @NonNull
    protected Resources getResources(@NonNull Context context) {
        return context.getResources();
    }

    private void update() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (!isAdded() || screen == null || mInjector == null) {
            return;
        }
        // Ensure not null if isAdded() returns true
        Context context = requireContext();
        mDisplay = mInjector.getDisplay(getDisplayIdArg());
        if (mDisplay == null) {
            finishFragment();
            return;
        }
        mResolutionPreferences.clear();
        List<Mode> supportedModes = new ArrayList<>(mDisplay.getSupportedModes());
        supportedModes.sort(
                Comparator.comparingInt(Mode::getPhysicalWidth)
                        .thenComparingInt(Mode::getPhysicalHeight)
                        .reversed());
        mAvailableModes.clear();
        for (Mode mode : supportedModes) {
            mAvailableModes.put(mode.getModeId(), mode);
        }

        var remainingModes =
                addModePreferences(
                        context,
                        getTopPreference(context, screen),
                        supportedModes,
                        this::isTopMode,
                        mDisplay);
        addRemainingPreferences(
                context,
                getMorePreference(context, screen),
                mDisplay,
                remainingModes.first,
                remainingModes.second);
        Log.i(TAG, "Currently selected display mode: " + modeToReadableString(getSelectedMode()));
    }

    private PreferenceCategory getTopPreference(
            @NonNull Context context, @NonNull PreferenceScreen screen) {
        if (mTopOptionsPreference == null) {
            mTopOptionsPreference = new PreferenceCategory(context);
            mTopOptionsPreference.setPersistent(false);
            mTopOptionsPreference.setKey(TOP_OPTIONS_KEY);
            screen.addPreference(mTopOptionsPreference);
        } else {
            mTopOptionsPreference.removeAll();
        }
        return mTopOptionsPreference;
    }

    private PreferenceCategory getMorePreference(
            @NonNull Context context, @NonNull PreferenceScreen screen) {
        if (mMoreOptionsPreference == null) {
            mMoreOptionsPreference = new PreferenceCategory(context);
            mMoreOptionsPreference.setPersistent(false);
            mMoreOptionsPreference.setTitle(MORE_OPTIONS_TITLE_RESOURCE);
            mMoreOptionsPreference.setOnExpandButtonClickListener(
                    () -> {
                        mMoreOptionsExpanded = true;
                    });
            mMoreOptionsPreference.setKey(MORE_OPTIONS_KEY);
            screen.addPreference(mMoreOptionsPreference);
        } else {
            mMoreOptionsPreference.removeAll();
        }
        return mMoreOptionsPreference;
    }

    private void addRemainingPreferences(
            @NonNull Context context,
            @NonNull PreferenceCategory group,
            @NonNull DisplayDevice display,
            boolean isSelectedModeFound,
            @NonNull List<Mode> moreModes) {
        if (moreModes.isEmpty()) {
            return;
        }
        mMoreOptionsExpanded |= !isSelectedModeFound;
        group.setInitialExpandedChildrenCount(mMoreOptionsExpanded ? Integer.MAX_VALUE : 0);
        addModePreferences(context, group, moreModes, /* checkMode= */ null, display);
    }

    private Pair<Boolean, List<Mode>> addModePreferences(
            @NonNull Context context,
            @NonNull PreferenceGroup group,
            @NonNull List<Mode> modes,
            @Nullable ToBooleanFunction<Mode> checkMode,
            @NonNull DisplayDevice display) {
        Mode curMode = display.getMode();
        var currentResolution = modeToPrefKey(curMode);
        var rotatedResolution = rotatedModeToPrefKey(curMode);
        var skippedModes = new ArrayList<Mode>();
        var isAnyOfModesSelected = false;
        for (var mode : modes) {
            var modeStr = modeToPrefKey(mode);
            if (mResolutionPreferences.containsKey(modeStr)) {
                continue;
            }
            if (checkMode != null && !checkMode.apply(mode)) {
                skippedModes.add(mode);
                continue;
            }
            var isCurrentMode =
                    currentResolution.equals(modeStr) || rotatedResolution.equals(modeStr);
            if (!isCurrentMode && !isAllowedMode(mode)) {
                continue;
            }
            mResolutionPreferences.put(modeStr, mode);
            SelectorWithWidgetPreference pref = new SelectorWithWidgetPreference(context);
            pref.setPersistent(false);
            pref.setKey(modeStr);
            int width = mode.getPhysicalWidth();
            int height = mode.getPhysicalHeight();
            pref.setTitle(
                    createAccessibleSequence(
                            modeToReadableString(mode),
                            getResources()
                                    .getString(
                                            R.string.screen_resolution_delimiter_a11y,
                                            width,
                                            height)));
            pref.setSingleLineTitle(true);
            pref.setOnClickListener(preference -> onDisplayModeClicked(preference, display));
            pref.setChecked(isCurrentMode);
            isAnyOfModesSelected |= isCurrentMode;
            group.addPreference(pref);
        }
        return new Pair<>(isAnyOfModesSelected, skippedModes);
    }

    private boolean isTopMode(@NonNull Mode mode) {
        return mTopOptionsPreference != null
                && mTopOptionsPreference.getPreferenceCount() < TOP_MODE_RES_MAX_COUNT;
    }

    private boolean isAllowedMode(@NonNull Mode mode) {
        if (mRefreshRateSynchronizationEnabled
                && (mode.getRefreshRate() < DEFAULT_LOW_REFRESH_RATE - 1
                        || mode.getRefreshRate() > DEFAULT_LOW_REFRESH_RATE + 1)) {
            Log.d(TAG, mode + " refresh rate is out of synchronization range");
            return false;
        }
        if (mExternalDisplayPeakHeight > 0
                && mode.getPhysicalHeight() > mExternalDisplayPeakHeight) {
            Log.d(TAG, mode + " height is above the allowed limit");
            return false;
        }
        if (mExternalDisplayPeakWidth > 0 && mode.getPhysicalWidth() > mExternalDisplayPeakWidth) {
            Log.d(TAG, mode + " width is above the allowed limit");
            return false;
        }
        if (mExternalDisplayPeakRefreshRate > 0
                && mode.getRefreshRate() > mExternalDisplayPeakRefreshRate) {
            Log.d(TAG, mode + " refresh rate is above the allowed limit");
            return false;
        }

        // If mode filtering is enabled.
        if (!mExternDisplayResolutionShown.isEmpty()) {
            Mode m;
            // If this is an anisotropic mode, check the base mode is supported.
            if ((mode.getFlags() & Mode.FLAG_ANISOTROPY_CORRECTION) != 0) {
                m = mAvailableModes.get(mode.getParentModeId());
            } else {
                m = mode;
            }
            if (m != null) {
                return mExternDisplayResolutionShown.contains(
                        new Point(m.getPhysicalWidth(), m.getPhysicalHeight()));
            }
        }
        return true;
    }

    private void scheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        unscheduleUpdate();
        mInjector.getHandler().post(mUpdateRunnable);
    }

    private void unscheduleUpdate() {
        if (mInjector == null || !mStarted) {
            return;
        }
        mInjector.getHandler().removeCallbacks(mUpdateRunnable);
    }

    private void onDisplayModeClicked(
            @NonNull SelectorWithWidgetPreference preference, @NonNull DisplayDevice display) {
        Mode curMode = display.getMode();
        var currentResolution = modeToPrefKey(curMode);
        if (currentResolution.equals(preference.getKey())) {
            return;
        }
        Mode mode = mResolutionPreferences.get(preference.getKey());
        if (mode == null || mInjector == null) {
            return;
        }
        // Don't show confirmation dialog for synthetic mode
        boolean isSyntheticMode = (mode.getFlags() & Mode.FLAG_SIZE_OVERRIDE) != 0;
        if (isSyntheticMode || !enableResolutionApplyConfirmation()) {
            mInjector.setUserPreferredDisplayMode(display.getId(), mode, /* storeMode= */ true);

            ExternalDisplaySettingsLoggerStore.ExternalDisplayMetricsLogger logger =
                    ExternalDisplaySettingsLoggerStore.getLogger(display.getId());
            logger.updateResolution(mode.getPhysicalWidth(), mode.getPhysicalHeight());
            logger.log(SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED__SETTING__RESOLUTION);
        } else {
            updateAllPreferenceStates(mode);
            showDialog();
            Log.i(TAG, "Selected display mode: " + modeToReadableString(mode));
            // TODO(b/421018668): Add logging for deferred resolution update.
        }
    }

    private void updateAllPreferenceStates(Mode mode) {
        String keyToCheck = modeToPrefKey(mode);
        updatePreferenceGroupState(mTopOptionsPreference, keyToCheck);
        updatePreferenceGroupState(mMoreOptionsPreference, keyToCheck);
    }

    private void updatePreferenceGroupState(@Nullable PreferenceGroup group, String key) {
        if (group == null) {
            return;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            Preference pref = group.getPreference(i);
            if (pref instanceof SelectorWithWidgetPreference) {
                ((SelectorWithWidgetPreference) pref).setChecked(pref.getKey().equals(key));
            }
        }
    }

    private void showDialog() {
        if (mInjector == null || mDisplay == null) {
            return;
        }
        Mode existingMode = mDisplay.getMode();
        Mode selectedMode = getSelectedMode();
        if (existingMode == null || selectedMode == null) {
            return;
        }
        // Temporarily set the display mode
        mInjector.setUserPreferredDisplayMode(
                mDisplay.getId(), selectedMode, /* storeMode= */ false);

        ResolutionChangeDialogFragment dialog =
                ResolutionChangeDialogFragment.Companion.newInstance(selectedMode, existingMode);
        Log.i(TAG, "Prompts for confirmation on resolution change");
        dialog.show(getParentFragmentManager(), ResolutionChangeDialogFragment.TAG);
    }

    private void updateDisplayModeLimits(@Nullable Context context) {
        if (context == null) {
            return;
        }
        mExternalDisplayPeakRefreshRate =
                getResources(context)
                        .getInteger(
                                com.android.internal.R.integer
                                        .config_externalDisplayPeakRefreshRate);
        mExternalDisplayPeakWidth =
                getResources(context)
                        .getInteger(com.android.internal.R.integer.config_externalDisplayPeakWidth);
        mExternalDisplayPeakHeight =
                getResources(context)
                        .getInteger(
                                com.android.internal.R.integer.config_externalDisplayPeakHeight);
        mRefreshRateSynchronizationEnabled =
                getResources(context)
                        .getBoolean(
                                com.android.internal.R.bool
                                        .config_refreshRateSynchronizationEnabled);
        int[] resolutionsArray =
                getResources(context)
                        .getIntArray(R.array.config_resolutionsShownOnExternalDisplay);
        mExternDisplayResolutionShown.clear();
        if (resolutionsArray != null) {
            for (int i = 0; i < resolutionsArray.length; i += 2) {
                if (i + 1 < resolutionsArray.length) {
                    mExternDisplayResolutionShown.add(
                            new Point(resolutionsArray[i], resolutionsArray[i + 1]));
                }
            }
        }
        Log.d(TAG, "mExternalDisplayPeakRefreshRate=" + mExternalDisplayPeakRefreshRate);
        Log.d(TAG, "mExternalDisplayPeakWidth=" + mExternalDisplayPeakWidth);
        Log.d(TAG, "mExternalDisplayPeakHeight=" + mExternalDisplayPeakHeight);
        Log.d(TAG, "mRefreshRateSynchronizationEnabled=" + mRefreshRateSynchronizationEnabled);
        Log.d(TAG, "mExternDisplayResolutionShown=" + mExternDisplayResolutionShown.stream().map(
                p -> p.x + "x" + p.y).collect(Collectors.joining(", ")));
    }

    private @Nullable Mode getSelectedMode() {
        Mode mode = getSelectedMode(mTopOptionsPreference);
        if (mode != null) {
            return mode;
        }
        return getSelectedMode(mMoreOptionsPreference);
    }

    private @Nullable Mode getSelectedMode(@Nullable PreferenceGroup group) {
        if (group == null) {
            return null;
        }
        for (int i = 0; i < group.getPreferenceCount(); i++) {
            SelectorWithWidgetPreference pref =
                    (SelectorWithWidgetPreference) group.getPreference(i);
            if (pref.isChecked()) {
                return mResolutionPreferences.get(pref.getKey());
            }
        }
        return null;
    }

    private String modeToPrefKey(@Nullable Mode m) {
        if (m == null) {
            return "";
        }
        return toPrefKey(m.getPhysicalWidth(), m.getPhysicalHeight());
    }

    private String rotatedModeToPrefKey(@Nullable Mode m) {
        if (m == null) {
            return "";
        }
        return toPrefKey(m.getPhysicalHeight(), m.getPhysicalWidth());
    }

    private String toPrefKey(int w, int h) {
        return w + "x" + h;
    }

    private String modeToReadableString(@Nullable Mode m) {
        if (m == null) {
            return "";
        }
        return m.getPhysicalWidth() + " x " + m.getPhysicalHeight();
    }

    private void setupResolutionApplyConfirmationHandler() {
        if (!enableResolutionApplyConfirmation()) {
            return;
        }
        getParentFragmentManager()
                .setFragmentResultListener(
                        ResolutionChangeDialogFragment.KEY_RESULT,
                        this,
                        (requestKey, bundle) -> {
                            if (mInjector == null) {
                                return;
                            }
                            boolean confirmed =
                                    bundle.getBoolean(
                                            ResolutionChangeDialogFragment.KEY_CONFIRMED, false);
                            int displayId = getDisplayIdArg();
                            if (confirmed) {
                                Mode selectedMode =
                                        bundle.getParcelable(
                                                ResolutionChangeDialogFragment.KEY_NEW_MODE,
                                                Mode.class);
                                if (selectedMode == null) {
                                    // This should never happen, just added to handle nullable
                                    // getParcelable()
                                    Log.w(
                                            TAG,
                                            "Selected mode was not set from dialog fragment"
                                                    + " result, reverting display#"
                                                    + displayId
                                                    + " resolution");
                                    mInjector.resetUserPreferredDisplayMode(displayId);
                                    return;
                                }
                                Log.i(
                                        TAG,
                                        "Updating display#"
                                                + displayId
                                                + " resolution to "
                                                + modeToReadableString(selectedMode));
                                mInjector.setUserPreferredDisplayMode(
                                        displayId, selectedMode, /* storeMode= */ true);
                            } else {
                                // Proactively update UI since it will take time for the reset
                                // to be propagated to DisplayManager and for DisplayManager to
                                // update the listener back
                                Mode existingMode =
                                        bundle.getParcelable(
                                                ResolutionChangeDialogFragment.KEY_EXISTING_MODE,
                                                Mode.class);
                                updateAllPreferenceStates(existingMode);
                                Log.i(
                                        TAG,
                                        "Reverting display#"
                                                + displayId
                                                + " resolution to "
                                                + modeToReadableString(existingMode));
                                mInjector.resetUserPreferredDisplayMode(displayId);
                            }
                        });
    }

    private boolean enableResolutionApplyConfirmation() {
        if (mInjector == null) {
            return false;
        }
        return mInjector.getFlags().enableResolutionApplyConfirmationBugfix();
    }
}
