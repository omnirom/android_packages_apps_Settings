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

import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DISPLAY_ID_ARG;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_HELP_URL;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.EXTERNAL_DISPLAY_NOT_FOUND_RESOURCE;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isDisplaySizeSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isResolutionSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isRotationSettingEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isTopologyPaneEnabled;
import static com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.isUseDisplaySettingEnabled;

import android.app.Activity;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.window.DesktopExperienceFlags;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragmentBase;
import com.android.settings.accessibility.TextReadingPreferenceFragment;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.DisplayListener;
import com.android.settings.connecteddevice.display.ExternalDisplaySettingsConfiguration.Injector;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.widget.FooterPreference;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.HashMap;
import java.util.List;

/**
 * The Settings screen for External Displays configuration and connection management.
 */
public class ExternalDisplayPreferenceFragment extends SettingsPreferenceFragmentBase {
    @VisibleForTesting enum PrefBasics {
        DISPLAY_TOPOLOGY(10, "display_topology_preference", null),
        MIRROR(20, "mirror_preference", R.string.external_display_mirroring_title),

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

        // Built-in display link is before per-display settings.
        BUILTIN_DISPLAY_LIST(70, "builtin_display_list_preference",
                R.string.builtin_display_settings_category),

        EXTERNAL_DISPLAY_LIST(-1, "external_display_list", null),

        // If shown, footer should appear below everything.
        FOOTER(90, "footer_preference", null);


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

    private boolean mStarted;
    @Nullable
    private Preference mDisplayTopologyPreference;
    @Nullable
    private PreferenceCategory mBuiltinDisplayPreference;
    @Nullable
    private Preference mBuiltinDisplaySizeAndTextPreference;
    @Nullable
    private Injector mInjector;
    @Nullable
    private String[] mRotationEntries;
    @Nullable
    private String[] mRotationEntriesValues;
    @NonNull
    private final Runnable mUpdateRunnable = this::update;
    private final DisplayListener mListener = new DisplayListener() {
        @Override
        public void update(int displayId) {
            scheduleUpdate();
        }
    };

    public ExternalDisplayPreferenceFragment() {}

    @VisibleForTesting
    ExternalDisplayPreferenceFragment(@NonNull Injector injector) {
        mInjector = injector;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    public int getHelpResource() {
        return EXTERNAL_DISPLAY_HELP_URL;
    }

    @Override
    public void onCreateCallback(@Nullable Bundle icicle) {
        if (mInjector == null) {
            mInjector = new Injector(getPrefContext());
        }
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

    /**
     * @return id of the preference.
     */
    @Override
    protected int getPreferenceScreenResId() {
        return EXTERNAL_DISPLAY_SETTINGS_RESOURCE;
    }

    @VisibleForTesting
    protected void launchResolutionSelector(@NonNull final Context context, final int displayId) {
        final Bundle args = new Bundle();
        args.putInt(DISPLAY_ID_ARG, displayId);
        new SubSettingLauncher(context)
                .setDestination(ResolutionPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    @VisibleForTesting
    protected void launchBuiltinDisplaySettings() {
        final Bundle args = new Bundle();
        var context = getPrefContext();
        new SubSettingLauncher(context)
                .setDestination(TextReadingPreferenceFragment.class.getName())
                .setArguments(args)
                .setSourceMetricsCategory(getMetricsCategory()).launch();
    }

    // The real FooterPreference requires a resource which is not available in unit tests.
    @VisibleForTesting
    Preference newFooterPreference(Context context) {
        return new FooterPreference(context);
    }

    /**
     * Returns the preference for the footer.
     */
    private void addFooterPreference(Context context, PrefRefresh refresh, int title) {
        var pref = refresh.findUnusedPreference(PrefBasics.FOOTER.key);
        if (pref == null) {
            pref = newFooterPreference(context);
            PrefBasics.FOOTER.apply(pref, /* nth= */ null);
        }
        pref.setTitle(title);
        refresh.addPreference(pref);
    }

    @NonNull
    private ListPreference reuseRotationPreference(@NonNull Context context, PrefRefresh refresh,
            int position) {
        ListPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_ROTATION.keyForNth(position));
        if (pref == null) {
            pref = new ListPreference(context);
            PrefBasics.EXTERNAL_DISPLAY_ROTATION.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private Preference reuseResolutionPreference(@NonNull Context context, PrefRefresh refresh,
            int position) {
        var pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.keyForNth(position));
        if (pref == null) {
            pref = new Preference(context);
            PrefBasics.EXTERNAL_DISPLAY_RESOLUTION.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private MainSwitchPreference reuseUseDisplayPreference(
            Context context, PrefRefresh refresh, int position) {
        MainSwitchPreference pref = refresh.findUnusedPreference(
                PrefBasics.EXTERNAL_DISPLAY_USE.keyForNth(position));
        if (pref == null) {
            pref = new MainSwitchPreference(context);
            PrefBasics.EXTERNAL_DISPLAY_USE.apply(pref, position);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private IllustrationPreference reuseIllustrationPreference(
            Context context, PrefRefresh refresh) {
        IllustrationPreference pref = refresh.findUnusedPreference(PrefBasics.ILLUSTRATION.key);
        if (pref == null) {
            pref = new IllustrationPreference(context);
            PrefBasics.ILLUSTRATION.apply(pref, /* nth= */ null);
        }
        refresh.addPreference(pref);
        return pref;
    }

    @NonNull
    private PreferenceCategory getBuiltinDisplayListPreference(@NonNull Context context) {
        if (mBuiltinDisplayPreference == null) {
            mBuiltinDisplayPreference = new PreferenceCategory(context);
            PrefBasics.BUILTIN_DISPLAY_LIST.apply(mBuiltinDisplayPreference, /* nth= */ null);
        }
        return mBuiltinDisplayPreference;
    }

    @NonNull
    private Preference getBuiltinDisplaySizeAndTextPreference(@NonNull Context context) {
        if (mBuiltinDisplaySizeAndTextPreference == null) {
            mBuiltinDisplaySizeAndTextPreference = new BuiltinDisplaySizeAndTextPreference(context);
        }
        return mBuiltinDisplaySizeAndTextPreference;
    }

    @NonNull Preference getDisplayTopologyPreference(@NonNull Context context) {
        if (mDisplayTopologyPreference == null) {
            mDisplayTopologyPreference = new DisplayTopologyPreference(context);
            PrefBasics.DISPLAY_TOPOLOGY.apply(mDisplayTopologyPreference, /* nth= */ null);
        }
        return mDisplayTopologyPreference;
    }

    private void addMirrorPreference(Context context, PrefRefresh refresh) {
        Preference pref = refresh.findUnusedPreference(PrefBasics.MIRROR.key);
        if (pref == null) {
            pref = new MirrorPreference(context,
                DesktopExperienceFlags.ENABLE_DISPLAY_CONTENT_MODE_MANAGEMENT.isTrue());
            PrefBasics.MIRROR.apply(pref, /* nth= */ null);
        }
        refresh.addPreference(pref);
    }

    @NonNull
    private ExternalDisplaySizePreference reuseSizePreference(Context context,
            PrefRefresh refresh, DisplayDevice display, int position) {
        ExternalDisplaySizePreference pref =
                refresh.findUnusedPreference(PrefBasics.EXTERNAL_DISPLAY_SIZE.keyForNth(position));
        if (pref == null) {
            pref = new ExternalDisplaySizePreference(context, /* attrs= */ null);
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
        if (screen == null || mInjector == null || mInjector.getContext() == null) {
            return;
        }
        try (var cleanableScreen = new PrefRefresh(screen)) {
            updateScreen(cleanableScreen, mInjector.getContext());
        }
    }

    private void updateScreen(final PrefRefresh screen, Context context) {
        final var displaysToShow = mInjector == null
                ? List.<DisplayDevice>of() : mInjector.getConnectedDisplays();

        if (displaysToShow.isEmpty()) {
            showTextWhenNoDisplaysToShow(screen, context, /* position= */ 0);
        } else {
            showDisplaysList(displaysToShow, screen, context);
        }

        final Activity activity = getCurrentActivity();
        if (activity != null) {
            activity.setTitle(EXTERNAL_DISPLAY_TITLE_RESOURCE);
        }
    }

    private void showTextWhenNoDisplaysToShow(@NonNull final PrefRefresh screen,
            @NonNull Context context, int position) {
        if (isUseDisplaySettingEnabled(mInjector)) {
            addUseDisplayPreferenceNoDisplaysFound(context, screen, position);
        }
        addFooterPreference(context, screen, EXTERNAL_DISPLAY_NOT_FOUND_FOOTER_RESOURCE);
    }

    private static PreferenceCategory reuseDisplayCategory(
            PrefRefresh screen, Context context, int position) {
        // The rest of the settings are in a category with the display name as the title.
        String categoryKey = PrefBasics.EXTERNAL_DISPLAY_LIST.keyForNth(position);
        var category = (PreferenceCategory) screen.findUnusedPreference(categoryKey);

        if (category != null) {
            screen.addPreference(category);
        } else {
            category = new PreferenceCategory(context);
            screen.addPreference(category);
            PrefBasics.EXTERNAL_DISPLAY_LIST.apply(category, position);
            category.setOrder(PrefBasics.BUILTIN_DISPLAY_LIST.order + 1 + position);
        }

        return category;
    }

    private void showDisplaySettings(DisplayDevice display, PrefRefresh refresh,
            Context context, boolean includeV1Helpers, int position) {
        if (isUseDisplaySettingEnabled(mInjector)) {
            addUseDisplayPreferenceForDisplay(context, refresh, display, position);
        }
        final var displayRotation = getDisplayRotation(display.getId());
        if (includeV1Helpers && display.isEnabled() == DisplayIsEnabled.YES) {
            addIllustrationImage(context, refresh, displayRotation);
        }

        addResolutionPreference(context, refresh, display, position);
        addRotationPreference(context, refresh, display, displayRotation, position);
        if (isResolutionSettingEnabled(mInjector)) {
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
                        context, refresh, EXTERNAL_DISPLAY_CHANGE_RESOLUTION_FOOTER_RESOURCE);
            }
        }
        if (isDisplaySizeSettingEnabled(mInjector)) {
            addSizePreference(context, refresh, display, position);
        }
    }

    private void maybeAddV2Components(Context context, PrefRefresh screen) {
        if (isTopologyPaneEnabled(mInjector)) {
            screen.addPreference(getDisplayTopologyPreference(context));
            addMirrorPreference(context, screen);

            // If topology is shown, we also show a preference for the built-in display for
            // consistency with the topology.
            var builtinCategory = getBuiltinDisplayListPreference(context);
            screen.addPreference(builtinCategory);
            builtinCategory.addPreference(getBuiltinDisplaySizeAndTextPreference(context));
        }
    }

    private void showDisplaysList(@NonNull List<DisplayDevice> displaysToShow,
            @NonNull PrefRefresh screen, @NonNull Context context) {
        maybeAddV2Components(context, screen);
        int position = 0;
        boolean includeV1Helpers = !isTopologyPaneEnabled(mInjector) && displaysToShow.size() <= 1;
        for (var display : displaysToShow) {
            var category = reuseDisplayCategory(screen, context, position);
            category.setTitle(display.getName());

            try (var refresh = new PrefRefresh(category)) {
                // The category may have already been populated if it was retrieved from `screen`,
                // but we still need to update resolution and rotation items.
                showDisplaySettings(display, refresh, context, includeV1Helpers, position);
            }

            position++;
        }
    }

    private void addUseDisplayPreferenceNoDisplaysFound(Context context, PrefRefresh refresh,
            int position) {
        final var pref = reuseUseDisplayPreference(context, refresh, position);
        pref.setChecked(false);
        pref.setEnabled(false);
        pref.setOnPreferenceChangeListener(null);
    }

    private void addUseDisplayPreferenceForDisplay(final Context context,
            PrefRefresh refresh, final DisplayDevice display, int position) {
        final var pref = reuseUseDisplayPreference(context, refresh, position);
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

    private void addIllustrationImage(final Context context, PrefRefresh refresh,
            final int displayRotation) {
        var pref = reuseIllustrationPreference(context, refresh);
        if (displayRotation % 2 == 0) {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_PORTRAIT_DRAWABLE);
        } else {
            pref.setLottieAnimationResId(EXTERNAL_DISPLAY_LANDSCAPE_DRAWABLE);
        }
    }

    private void addRotationPreference(final Context context, PrefRefresh refresh,
            final DisplayDevice display, final int displayRotation, int position) {
        var pref = reuseRotationPreference(context, refresh, position);
        if (mRotationEntries == null || mRotationEntriesValues == null) {
            mRotationEntries = new String[] {
                    context.getString(R.string.external_display_standard_rotation),
                    context.getString(R.string.external_display_rotation_90),
                    context.getString(R.string.external_display_rotation_180),
                    context.getString(R.string.external_display_rotation_270)};
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
            return true;
        });
        pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES
                && isRotationSettingEnabled(mInjector));
    }

    private void addResolutionPreference(final Context context, PrefRefresh refresh,
            final DisplayDevice display, int position) {
        var pref = reuseResolutionPreference(context, refresh, position);
        pref.setSummary(display.getMode().getPhysicalWidth() + " x "
                + display.getMode().getPhysicalHeight());
        pref.setOnPreferenceClickListener((Preference p) -> {
            writePreferenceClickMetric(p);
            launchResolutionSelector(context, display.getId());
            return true;
        });
        pref.setEnabled(display.isEnabled() == DisplayIsEnabled.YES
                && isResolutionSettingEnabled(mInjector));
    }

    private void addSizePreference(final Context context, PrefRefresh refresh,
            DisplayDevice display, int position) {
        var pref = reuseSizePreference(context, refresh, display, position);
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
}
