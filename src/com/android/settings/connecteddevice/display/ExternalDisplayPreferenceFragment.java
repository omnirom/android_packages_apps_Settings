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
import static android.hardware.display.DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK;
import static android.hardware.display.DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_DESKTOP;
import static android.hardware.display.DisplayManager.EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR;
import static android.provider.Settings.Secure.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY;

import static com.android.settings.Utils.createAccessibleSequence;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isExternalDisplaySettingsPageEnabled;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityTaskManager;
import android.app.TaskStackListener;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.view.Display;
import android.view.View;
import android.widget.TextView;
import android.window.DesktopExperienceFlags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.RestrictedListPreference;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.accessibility.TextReadingPreferenceFragment;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.core.instrumentation.SettingsStatsLog;
import com.android.settings.flags.FeatureFlagsImpl;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Settings screen for External Displays configuration and connection management.
 */
@SearchIndexable
public class ExternalDisplayPreferenceFragment extends SettingsPreferenceFragmentBase {
    @VisibleForTesting enum PrefBasics {
        DISPLAY_TOPOLOGY(10, "display_topology_preference", null),
        MIRROR(20, "mirror_preference", R.string.external_display_mirroring_title),
        INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY(
                25,
                "include_default_display_in_topology_preference",
                R.string.builtin_display_settings_universal_cursor_title),

        // If shown, use toggle should be before other per-display settings.
        EXTERNAL_DISPLAY_USE(30, "external_display_use_preference",
                R.string.external_display_use_title),

        ILLUSTRATION(35, "external_display_illustration", null),

        // If shown, external display size is before other per-display settings.
        EXTERNAL_DISPLAY_SIZE(40, "external_display_size", R.string.screen_zoom_title),
        EXTERNAL_DISPLAY_ROTATION(50, "external_display_rotation",
                R.string.external_display_rotation),
        EXTERNAL_DISPLAY_RESOLUTION(60, "external_display_resolution",
                R.string.external_display_resolution_settings_title),
        EXTERNAL_DISPLAY_CONNECTION(65, "external_display_connection_preference",
                R.string.external_display_connection_preference),

        // Built-in display link is before per-display settings.
        BUILTIN_DISPLAY_LIST(70, "builtin_display_list_preference",
                R.string.builtin_display_settings_category),

        EXTERNAL_DISPLAY_LIST(-1, "external_display_list", null),

        // If shown, footer should appear below everything.
        FOOTER(90, "footer_preference", null),

        // A static preference just to show "no external display is connected" info
        NO_EXTERNAL_DISPLAY_CONNECTED(100, "no_external_display_connected_preference", null);


        PrefBasics(int order, String key, @Nullable Integer titleResource) {
            this.order = order;
            this.key = key;
            this.titleResource = titleResource;
        }

        // Fields must be public to make the linter happy.
        public final int order;
        public final String key;
        @Nullable public final Integer titleResource;

        /**
         * Applies this basic data to the given preference.
         *
         * @param preference object whose properties to set
         * @param nth if non-null, disambiguates the key so that other preferences can have the same
         *            basic properties. Does not affect the order.
         */
        void apply(Preference preference, @Nullable Integer nth) {
            if (order != -1) {
                preference.setOrder(order);
            }
            if (titleResource != null) {
                preference.setTitle(titleResource);
            }
            preference.setKey(nth == null ? key : keyForNth(nth));
            preference.setPersistent(false);
        }

        String keyForNth(int nth) {
            return key + "_" + nth;
        }
    }

    static final int EXTERNAL_DISPLAY_SETTINGS_RESOURCE = R.xml.external_display_settings;
    static final int EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE =
            R.string.external_display_change_resolution_footer_title;
    static final int EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE =
            R.drawable.external_display_mirror_landscape;
    static final int EXTERNAL_DISPLAY_TITLE_RESOURCE =
            R.string.external_display_settings_title;
    static final int EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE =
            R.string.external_display_not_found_footer_title;
    static final int EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE =
            R.drawable.external_display_mirror_portrait;
    static final int EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE = R.string.screen_zoom_short_summary;
    static final int INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY_SUMMARY_RESOURCE =
            R.string.builtin_display_settings_universal_cursor_description;

    static final String CONNECTION_PREF_NONE = "none";
    static final String CONNECTION_PREF_DESKTOP = "desktop";
    static final String CONNECTION_PREF_MIRROR = "mirror";

    @VisibleForTesting
    final LockTaskModeChangedListener mLockTaskModeChangedListener =
            new LockTaskModeChangedListener();

    private boolean mStarted;
    private int mLockTaskMode;
    @Nullable
    private Preference mDisplayTopologyPreference;
    @Nullable
    private PreferenceCategory mBuiltinDisplayPreference;
    @Nullable
    private Preference mBuiltinDisplaySizeAndTextPreference;
    @NonNull
    private ConnectedDisplayInjector mInjector;
    @Nullable
    private String[] mRotationEntries;
    @Nullable
    private String[] mRotationEntriesValues;
    @Nullable
    private String[] mConnectionEntries;
    @Nullable
    private String[] mConnectionEntriesValues;
    @NonNull
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener = new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };
    private ActivityTaskManager mActivityTaskManager;
    private DevicePolicyManager mDpm;

    public ExternalDisplayPreferenceFragment() {
        mInjector = new ConnectedDisplayInjector(/* context= */ null);
    }

    @VisibleForTesting
    ExternalDisplayPreferenceFragment(@NonNull ConnectedDisplayInjector injector) {
        mInjector = injector;
    }

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
        if (mInjector.getContext() == null) {
            mInjector = new ConnectedDisplayInjector(requireContext());
        }
        mActivityTaskManager = requireContext().getSystemService(ActivityTaskManager.class);
        mDpm = requireContext().getSystemService(DevicePolicyManager.class);
        addPreferencesFromResource(EXTERNAL_DISPLAY_SETTINGS_RESOURCE);
    }

    @Override
    public void onActivityCreatedCallback(@Nullable Bundle savedInstanceState) {
        View view = getView();
        TextView emptyView = null;
        if (view != null) {
            emptyView = view.findViewById(android.R.id.empty);
        }
        if (emptyView != null) {
            emptyView.setText(EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE);
            setEmptyView(emptyView);
        }
    }

    @Override
    public void onStartCallback() {
        mStarted = true;
        mLockTaskMode =
                requireContext().getSystemService(ActivityManager.class).getLockTaskModeState();
        mActivityTaskManager.registerTaskStackListener(mLockTaskModeChangedListener);
        mInjector.registerDisplayListener(mListener);
        scheduleUpdate();
    }

    @Override
    public void onStopCallback() {
        mStarted = false;
        mInjector.unregisterDisplayListener(mListener);
        ExternalDisplaySettingsLoggerStore.removeAllLoggers();
        mActivityTaskManager.unregisterTaskStackListener(mLockTaskModeChangedListener);
        unscheduleUpdate();
    }

    /**
     * @return id of the preference.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
    }

    @VisibleForTesting
    protected void launchResolutionSelector(final int displayId) {
        final Bundle args = new Bundle();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(requireContext())
                .setDestination(ResolutionPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @VisibleForTesting
    protected void launchBuiltinDisplaySettings() {
        final Bundle args = new Bundle();
        new SubSettingLauncher(requireContext())
                .setDestination(TextReadingPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    // The real FooterPreference requires a resource which is not available in unit tests.
    @VisibleForTesting
    Preference newFooterPreference() {
        return new FooterPreference(requireContext());
    }

    /**
     * Returns the preference for the footer.
     */
    private void addFooterPreference(PrefRefresh refresh, int title) {
        var pref = refresh.findUnusedPreference(PrefBasics.FOOTER.key);
        if (pref == null) {
            pref = newFooterPreference();
            PrefBasics.FOOTER.apply(pref, /* nth= */ null);
        }
        pref.setTitle(title);
        refresh.addPreference(pref);
    }

    private void addNoExternalDisplayConnectedPreference(PrefRefresh refresh) {
        var pref = refresh.findUnusedPreference(PrefBasics.NO_EXTERNAL_DISPLAY_CONNECTED.key);
        if (pref == null) {
            pref = new Preference(requireContext());
            pref.setLayoutResource(R.layout.no_external_display_connected_screen);
            pref.setSelectable(false);
            pref.setPersistent(false);
            PrefBasics.NO_EXTERNAL_DISPLAY_CONNECTED.apply(pref, /* nth= */ null);
        }
        refresh.addPreference(pref);
    }

    @NonNull
    private RestrictedListPreference reuseConnectionPreference(PrefRefresh refresh, int position) {
        RestrictedListPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_CONNECTION.keyForNth(position));
        if (pref == null) {
            pref = new RestrictedListPreference(requireContext());
            PrefBasics.EXTERNAL_DISPLAY_CONNECTION.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private ListPreference reuseRotationPreference(PrefRefresh refresh, int position) {
        ListPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(position));
        if (pref == null) {
            pref = new ListPreference(requireContext());
            PrefBasics.EXTERNAL_DISPLAY_ROTATION.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private Preference reuseResolutionPreference(PrefRefresh refresh, int position) {
        var pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(position));
        if (pref == null) {
            pref = new Preference(requireContext());
            PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private MainSwitchPreference reuseUseDisplayPreference(PrefRefresh refresh, int position) {
        MainSwitchPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(position));
        if (pref == null) {
            pref = new MainSwitchPreference(requireContext());
            PrefBasics.EXTERNAL_DISPLAY_USE.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private IllustrationPreference reuseIllustrationPreference(PrefRefresh refresh) {
        IllustrationPreference pref = refresh.findUnusedPreference(PrefBasics.ILLUSTRATION.key);
        if (pref == null) {
            pref = new IllustrationPreference(requireContext());
            PrefBasics.ILLUSTRATION.apply(pref, /* nth= */ null);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private PreferenceCategory getBuiltinDisplayListPreference() {
        if (mBuiltinDisplayPreference == null) {
            mBuiltinDisplayPreference = new PreferenceCategory(requireContext());
            PrefBasics.BUILTIN_DISPLAY_LIST.apply(mBuiltinDisplayPreference, /* nth= */ null);
        }
        return mBuiltinDisplayPreference;
    }

    @NonNull
    private Preference getBuiltinDisplaySizeAndTextPreference() {
        if (mBuiltinDisplaySizeAndTextPreference == null) {
            mBuiltinDisplaySizeAndTextPreference = new BuiltinDisplaySizeAndTextPreference(
                    requireContext());
        }
        return mBuiltinDisplaySizeAndTextPreference;
    }

    @NonNull Preference getDisplayTopologyPreference() {
        if (mDisplayTopologyPreference == null) {
            mDisplayTopologyPreference = new DisplayTopologyPreference(mInjector);
            PrefBasics.DISPLAY_TOPOLOGY.apply(mDisplayTopologyPreference, /* nth= */ null);
        }
        return mDisplayTopologyPreference;
    }

    private void addMirrorPreference(PrefRefresh refresh) {
        MirrorPreference pref = refresh.findUnusedPreference(PrefBasics.MIRROR.key);
        boolean isDisplayContentModeManagementFlagEnabled =
                DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue();
        if (pref == null) {
            pref = new MirrorPreference(requireContext(),
                    isDisplayContentModeManagementFlagEnabled);
            PrefBasics.MIRROR.apply(pref, /* nth= */ null);
        }
        if (mLockTaskMode == LOCK_TASK_MODE_LOCKED) {
            final EnforcingAdmin enforcingAdmin = mDpm.getEnforcingAdminsForPolicy(
                    DevicePolicyIdentifiers.LOCK_TASK_POLICY,
                    UserHandle.myUserId()).getMostImportantEnforcingAdmin();
            pref.setDisabledByAdmin(enforcingAdmin);
            pref.setChecked(true);
        } else {
            pref.setDisabledByAdmin((EnforcingAdmin) null);
            pref.setEnabled(isDisplayContentModeManagementFlagEnabled);
            pref.setChecked(isDisplayInMirroringMode());
            pref.setSummary("");
        }
        refresh.addPreference(pref);
    }

    private void addIncludeDefaultDisplayInTopologyPreference(PrefRefresh refresh) {
        Preference pref =
                refresh.findUnusedPreference(PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.key);
        if (pref == null) {
            pref = new SwitchPreferenceCompat(requireContext());
            PrefBasics.INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY.apply(pref, /* nth= */ null);
            pref.setSummary(INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY_SUMMARY_RESOURCE);
            final SwitchPreferenceCompat switchPref = (SwitchPreferenceCompat) pref;
            boolean isActive =
                    Settings.Secure.getInt(
                            requireContext().getContentResolver(),
                            INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
                            0)
                            != 0;
            switchPref.setChecked(isActive);
            switchPref.setOnPreferenceClickListener(
                    (p) -> {
                        writePreferenceClickMetric(p);
                        Settings.Secure.putInt(
                                requireContext().getContentResolver(),
                                INCLUDE_DEFAULT_DISPLAY_IN_TOPOLOGY,
                                switchPref.isChecked() ? 1 : 0);
                        return true;
                    });
        }
        refresh.addPreference(pref);
    }

    @NonNull
    private ExternalDisplaySizePreference reuseSizePreference(
            PrefRefresh refresh, DisplayDevice display, int position) {
        ExternalDisplaySizePreference pref =
                refresh.findUnusedPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(position));
        if (pref == null) {
            pref = new ExternalDisplaySizePreference(requireContext(), /* attrs= */ null);
            PrefBasics.EXTERNAL_DISPLAY_SIZE.apply(pref, position);
        }
        if (display.getMode() != null) {
            pref.setStateForPreference(display.getMode().getPhysicalWidth(),
                    display.getMode().getPhysicalHeight(), display.getId());
        }
        refresh.addPreference(pref);
        return pref;
    }

    private void update() {
        final var screen = getPreferenceScreen();
        if (screen == null || getContext() == null) {
            return;
        }
        try (var cleanableScreen = new PrefRefresh(screen)) {
            updateScreen(cleanableScreen);
        }
    }

    private void updateScreen(final PrefRefresh screen) {
        var displaysToShow = mInjector.getDisplays().stream().filter(
                DisplayDevice::isConnectedDisplay).toList();
        if (mInjector.getFlags().displayTopologyPaneInDisplayList()) {
            displaysToShow = displaysToShow.stream().filter(
                    display -> display.isEnabled() == DisplayIsEnabled.YES).toList();
        }

        if (displaysToShow.isEmpty()) {
            showTextWhenNoDisplaysToShow(screen, /* position= */ 0);
        } else {
            showDisplaysList(displaysToShow, screen);
        }

        final Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.setTitle(EXTERNAL_DISPLAY_TITLE_RESOURCE);
        }
    }

    private boolean isUseDisplaySettingEnabled() {
        return mInjector.getFlags().resolutionAndEnableConnectedDisplaySetting()
                && !mInjector.getFlags().displayTopologyPaneInDisplayList();
    }

    private void showTextWhenNoDisplaysToShow(@NonNull final PrefRefresh screen, int position) {
        if (isUseDisplaySettingEnabled()) {
            addUseDisplayPreferenceNoDisplaysFound(screen, position);
            addFooterPreference(screen, EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE);
        } else {
            addNoExternalDisplayConnectedPreference(screen);
        }
    }

    private PreferenceCategory reuseDisplayCategory(PrefRefresh screen, int position) {
        // The rest of the settings are in a category with the display name as the title.
        String categoryKey = PrefBasics.EXTERNAL_DISPLAY_LIST.keyForNth(position);
        var category = (PreferenceCategory) screen.findUnusedPreference(categoryKey);

        if (category != null) {
            screen.addPreference(category);
        } else {
            category = new PreferenceCategory(requireContext());
            screen.addPreference(category);
            PrefBasics.EXTERNAL_DISPLAY_LIST.apply(category, position);
            category.setOrder(PrefBasics.BUILTIN_DISPLAY_LIST.order + 1 + position);
        }

        return category;
    }

    private void showDisplaySettings(DisplayDevice display, PrefRefresh refresh,
            boolean includeV1Helpers, int position) {
        if (isUseDisplaySettingEnabled()) {
            addUseDisplayPreferenceForDisplay(refresh, display, position);
        }
        final var displayRotation = getDisplayRotation(display.getId());
        if (includeV1Helpers && display.isEnabled() == DisplayIsEnabled.YES) {
            addIllustrationImage(refresh, displayRotation);
        }

        addResolutionPreference(refresh, display, position);
        addRotationPreference(refresh, display, displayRotation, position);

        ExternalDisplaySettingsLoggerStore.ExternalDisplayMetricsLogger logger =
                ExternalDisplaySettingsLoggerStore.getLogger(display.getId());
        logger.updateRotation(displayRotation * 90);
        Display.Mode mode = display.getMode();
        if (mode != null) {
            logger.updateResolution(mode.getPhysicalWidth(), mode.getPhysicalHeight());
        }
        if (DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue()
                && DesktopExperienceFlags.ENABLE_UPDATED_DISPLAY_CONNECTION_DIALOG.isTrue()
                && mInjector.isProjectedModeEnabled()) {
            addConnectionPreference(refresh, display, position);
        }
        if (mInjector.getFlags().resolutionAndEnableConnectedDisplaySetting()) {
            // Do not show the footer about changing resolution affecting apps. This is not in the
            // UX design for v2, and there is no good place to put it, since (a) if it is on the
            // bottom of the screen, the external resolution setting must be below the built-in
            // display options for the per-display fragment, which is too hidden for the per-display
            // fragment, or (b) the footer is above the Built-in display settings, rather than the
            // bottom of the screen, which contradicts the visual style and purpose of the
            // FooterPreference class, or (c) we must hide the built-in display settings, which is
            // inconsistent with the topology pane, which shows that display.
            // TODO(b/352648432): probably remove footer once the pane and rest of v2 UI is in
            // place.
            if (includeV1Helpers && display.isEnabled() == DisplayIsEnabled.YES) {
                addFooterPreference(
                        refresh, EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE);
            }
        }
        if (mInjector.getFlags().displaySizeConnectedDisplaySetting()
                && !isDisplayInMirroringMode()) {
            addSizePreference(refresh, display, position);
        }
    }

    private void maybeAddV2Components(PrefRefresh screen) {
        if (mInjector.getFlags().displayTopologyPaneInDisplayList()) {
            screen.addPreference(getDisplayTopologyPreference());
            addMirrorPreference(screen);
            if (mInjector.isDefaultDisplayInTopologyFlagEnabled()
                    && mInjector.isProjectedModeEnabled()
                    && !isDisplayInMirroringMode()) {
                addIncludeDefaultDisplayInTopologyPreference(screen);
            }

            // If topology is shown, we also show a preference for the built-in display for
            // consistency with the topology.
            var builtinCategory = getBuiltinDisplayListPreference();
            screen.addPreference(builtinCategory);
            builtinCategory.addPreference(getBuiltinDisplaySizeAndTextPreference());
        }
    }

    private void showDisplaysList(@NonNull List<DisplayDevice> displaysToShow,
            @NonNull PrefRefresh screen) {
        maybeAddV2Components(screen);
        int position = 0;
        boolean includeV1Helpers = !mInjector.getFlags().displayTopologyPaneInDisplayList()
                && displaysToShow.size() <= 1;
        for (var display : displaysToShow) {
            var category = reuseDisplayCategory(screen, position);
            category.setTitle(display.getName());

            try (var refresh = new PrefRefresh(category)) {
                // The category may have already been populated if it was retrieved from `screen`,
                // but we still need to update resolution and rotation items.
                showDisplaySettings(display, refresh, includeV1Helpers, position);
            }

            position++;
        }
    }

    private void addUseDisplayPreferenceNoDisplaysFound(PrefRefresh refresh, int position) {
        final var pref = reuseUseDisplayPreference(refresh, position);
        pref.setChecked(false);
        pref.setEnabled(false);
        pref.setOnPreferenceChangeListener(null);
    }

    private void addUseDisplayPreferenceForDisplay(
            PrefRefresh refresh, final DisplayDevice display, int position) {
        final var pref = reuseUseDisplayPreference(refresh, position);
        pref.setChecked(display.isEnabled() == DisplayIsEnabled.YES);
        pref.setEnabled(true);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            final boolean result;
            if (mInjector == null) {
                return false;
            }
            if ((Boolean) newValue) {
                result = mInjector.enableConnectedDisplay(display.getId());
            } else {
                result = mInjector.disableConnectedDisplay(display.getId());
            }
            if (result) {
                pref.setChecked((Boolean) newValue);
            }
            return result;
        });
    }

    private void addIllustrationImage(PrefRefresh refresh, final int displayRotation) {
        var pref = reuseIllustrationPreference(refresh);
        if (displayRotation % 2 == 0) {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE);
        } else {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE);
        }
    }

    private void addRotationPreference(PrefRefresh refresh,
            final DisplayDevice display, final int displayRotation, int position) {
        var pref = reuseRotationPreference(refresh, position);
        if (mRotationEntries == null || mRotationEntriesValues == null) {
            mRotationEntries = new String[] {
                    requireContext().getString(R.string.external_display_standard_rotation),
                    requireContext().getString(R.string.external_display_rotation_90),
                    requireContext().getString(R.string.external_display_rotation_180),
                    requireContext().getString(R.string.external_display_rotation_270)};
            mRotationEntriesValues = new String[] {"0", "1", "2", "3"};
        }
        pref.setEntries(mRotationEntries);
        pref.setEntryValues(mRotationEntriesValues);
        pref.setValueIndex(displayRotation);
        pref.setSummary(mRotationEntries[displayRotation]);
        pref.setOnPreferenceChangeListener((p, newValue) -> {
            writePreferenceClickMetric(p);
            var rotation = Integer.parseInt((String) newValue);
            var displayId = display.getId();
            if (mInjector == null || !mInjector.freezeDisplayRotation(displayId, rotation)) {
                return false;
            }
            pref.setValueIndex(rotation);
            ExternalDisplaySettingsLoggerStore.ExternalDisplayMetricsLogger logger =
                    ExternalDisplaySettingsLoggerStore.getLogger(display.getId());
            logger.updateRotation(rotation * 90);
            logger.log(SettingsStatsLog.EXTERNAL_DISPLAY_SETTINGS_CHANGED__SETTING__ROTATION);
            return true;
        });
        pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES
                && mInjector.getFlags().rotationConnectedDisplaySetting());
    }

    private void addConnectionPreference(PrefRefresh refresh, DisplayDevice display, int position) {
        final int connectionType = mInjector.getDisplayConnectionPreference(display.getUniqueId());
        var pref = reuseConnectionPreference(refresh, position);
        if (mLockTaskMode == LOCK_TASK_MODE_LOCKED) {
            // When kiosk mode is enabled, we hide the desktop option and force mirroring only,
            // so we should also lock the preference to mirror only as well
            pref.setValue(CONNECTION_PREF_MIRROR);
            pref.setSummary(requireContext().getString(
                    R.string.external_display_connection_preference_mirroring));
            final EnforcingAdmin enforcingAdmin = mDpm.getEnforcingAdminsForPolicy(
                    DevicePolicyIdentifiers.LOCK_TASK_POLICY,
                    UserHandle.myUserId()).getMostImportantEnforcingAdmin();
            pref.setDisabledByAdmin(enforcingAdmin);
        } else {
            if (mConnectionEntries == null || mConnectionEntriesValues == null) {
                mConnectionEntries = new String[]{
                        requireContext().getString(
                                R.string.external_display_connection_preference_show_dialog),
                        requireContext().getString(
                                R.string.external_display_connection_preference_desktop),
                        requireContext().getString(
                                R.string.external_display_connection_preference_mirroring),
                };
                mConnectionEntriesValues = new String[]{
                        CONNECTION_PREF_NONE,
                        CONNECTION_PREF_DESKTOP,
                        CONNECTION_PREF_MIRROR,
                };
            }
            pref.setEntries(mConnectionEntries);
            pref.setEntryValues(mConnectionEntriesValues);
            pref.setValueIndex(connectionType);
            pref.setSummary(mConnectionEntries[connectionType]);
            pref.setOnPreferenceChangeListener((p, newValue) -> {
                writePreferenceClickMetric(p);
                final int selectedConnectionPreference = switch ((String) newValue) {
                    case CONNECTION_PREF_DESKTOP -> EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_DESKTOP;
                    case CONNECTION_PREF_MIRROR -> EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_MIRROR;
                    default -> EXTERNAL_DISPLAY_CONNECTION_PREFERENCE_ASK;
                };
                mInjector.updateDisplayConnectionPreference(
                        display.getUniqueId(), selectedConnectionPreference);
                pref.setValue((String) newValue);
                scheduleUpdate();
                return true;
            });
            pref.setDisabledByAdmin((EnforcingAdmin) null);
            pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES);
        }
    }

    private void addResolutionPreference(PrefRefresh refresh,
            final DisplayDevice display, int position) {
        Display.Mode mode = display.getMode();
        if (mode == null) {
            return;
        }
        var pref = reuseResolutionPreference(refresh, position);
        int width = mode.getPhysicalWidth();
        int height = mode.getPhysicalHeight();
        pref.setSummary(
                createAccessibleSequence(
                        width + " x " + height,
                        getResources()
                                .getString(
                                        R.string.screen_resolution_delimiter_a11y, width, height)));
        pref.setOnPreferenceClickListener((Preference p) -> {
            writePreferenceClickMetric(p);
            launchResolutionSelector(display.getId());
            return true;
        });
        pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES
                && mInjector.getFlags().resolutionAndEnableConnectedDisplaySetting());
    }

    private void addSizePreference(PrefRefresh refresh, DisplayDevice display, int position) {
        var pref = reuseSizePreference(refresh, display, position);
        pref.setSummary(EXTERNAL_DISPLAY_SIZE_SUMMARY_RESOURCE);
        pref.setOnPreferenceClickListener(
                (Preference p) -> {
                    writePreferenceClickMetric(p);
                    return true;
                });
        pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES);
    }

    private int getDisplayRotation(int displayId) {
        if (mInjector == null) {
            return 0;
        }
        return Math.min(3, Math.max(0, mInjector.getDisplayUserRotation(displayId)));
    }

    @VisibleForTesting
    void scheduleUpdate() {
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

    private class BuiltinDisplaySizeAndTextPreference extends Preference
            implements Preference.OnPreferenceClickListener {
        BuiltinDisplaySizeAndTextPreference(@NonNull final Context context) {
            super(context);

            setPersistent(false);
            setKey("builtin_display_size_and_text");
            setTitle(R.string.accessibility_text_reading_options_title);
            setOnPreferenceClickListener(this);
        }

        @Override
        public boolean onPreferenceClick(@NonNull Preference preference) {
            launchBuiltinDisplaySettings();
            return true;
        }
    }

    private static class PrefRefresh implements AutoCloseable {
        private final PreferenceGroup mScreen;
        private final HashMap<String, Preference> mUnusedPreferences = new HashMap<>();

        PrefRefresh(@NonNull final PreferenceGroup screen) {
            mScreen = screen;
            int preferencesCount = mScreen.getPreferenceCount();
            for (int i = 0; i < preferencesCount; i++) {
                var pref = mScreen.getPreference(i);
                if (pref.hasKey()) {
                    mUnusedPreferences.put(pref.getKey(), pref);
                }
            }
        }

        @Nullable
        <P extends Preference> P findUnusedPreference(@NonNull String key) {
            return (P) mUnusedPreferences.get(key);
        }

        boolean addPreference(@NonNull final Preference pref) {
            if (pref.hasKey()) {
                final var previousPref = mUnusedPreferences.get(pref.getKey());
                if (pref == previousPref) {
                    // Exact preference already added, no need to add it again.
                    // And no need to remove this preference either.
                    mUnusedPreferences.remove(pref.getKey());
                    return true;
                }
                // Exact preference is not yet added
            }
            return mScreen.addPreference(pref);
        }

        @Override
        public void close() {
            for (var v : mUnusedPreferences.values()) {
                mScreen.removePreference(v);
            }
        }
    }

    @VisibleForTesting
    class LockTaskModeChangedListener extends TaskStackListener {

        @Override
        public void onLockTaskModeChanged(int mode) {
            super.onLockTaskModeChanged(mode);
            mLockTaskMode = mode;
            scheduleUpdate();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(EXTERNAL_DISPLAY_SETTINGS_RESOURCE) {
                @Override
                public @NonNull List<SearchIndexableRaw> getRawDataToIndex(@NonNull Context context,
                        boolean enabled) {
                    List<SearchIndexableRaw> rawData = new ArrayList<>();
                    if (!isExternalDisplaySettingsPageEnabled(new FeatureFlagsImpl())) {
                        return rawData;
                    }
                    SearchIndexableRaw indexInfo = new SearchIndexableRaw(context);
                    indexInfo.key = "external_display_screen_title";
                    indexInfo.title = context.getString(EXTERNAL_DISPLAY_TITLE_RESOURCE);
                    indexInfo.keywords = context.getString(
                            R.string.keywords_external_display_settings);
                    indexInfo.screenTitle = context.getString(
                            R.string.connected_devices_dashboard_title);
                    rawData.add(indexInfo);
                    return rawData;
                }
            };

    private boolean isDisplayInMirroringMode() {
        return mLockTaskMode == LOCK_TASK_MODE_LOCKED
                || ExternalDisplayUtilsKt.isDisplayInMirroringMode(requireContext());
    }
}
