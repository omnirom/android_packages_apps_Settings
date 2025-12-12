/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.dream;

import static android.service.dreams.Flags.dreamsV2;

import static com.android.settings.dream.DreamMainSwitchPreferenceController.MAIN_SWITCH_PREF_KEY;

import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.service.dreams.DreamService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.dream.DreamBackend;
import com.android.settingslib.dream.DreamBackend.WhenToDream;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.MainSwitchPreference;

import java.util.ArrayList;
import java.util.List;

// LINT.IfChange
@SearchIndexable
public class DreamSettings extends DashboardFragment implements OnCheckedChangeListener {

    private static final String TAG = "DreamSettings";
    static final String WHILE_CHARGING_ONLY = "while_charging_only";
    static final String WHILE_DOCKED_ONLY = "while_docked_only";
    static final String EITHER_CHARGING_OR_DOCKED = "either_charging_or_docked";
    static final String WHILE_POSTURED_ONLY = "while_postured_only";
    static final String NEVER_DREAM = "never";
    private static final String SPACE_PREF_KEY = "dream_space_preference";
    private static final long DREAMS_LIST_REFRESH_DELAY_MS = 1000;

    private MainSwitchPreference mMainSwitchPreference;
    private Button mPreviewButton;
    private Preference mComplicationsTogglePreference;
    private Preference mHomeControllerTogglePreference;
    private RecyclerView mRecyclerView;
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private DreamPickerController mDreamPickerController;
    private DreamHomeControlsPreferenceController mDreamHomeControlsPreferenceController;
    private LowLightModePreferenceController mLowLightModePreferenceController;

    /** A callback that is invoked whenever the selected dream changes. */
    private final DreamPickerController.Callback mCallback = () ->
            updateSelectedDreamSettingsState(
                    mMainSwitchPreference != null ? mMainSwitchPreference.isChecked() : false);

    private final Runnable mRefreshDreamsListRunnable = this::refreshDreamsList;
    private BroadcastReceiver mPackageObserver;

    @WhenToDream
    static int getSettingFromPrefKey(String key) {
        switch (key) {
            case WHILE_CHARGING_ONLY:
                return DreamBackend.WHILE_CHARGING;
            case WHILE_DOCKED_ONLY:
                return DreamBackend.WHILE_DOCKED;
            case EITHER_CHARGING_OR_DOCKED:
                return DreamBackend.WHILE_CHARGING_OR_DOCKED;
            case WHILE_POSTURED_ONLY:
                return DreamBackend.WHILE_POSTURED;
            case NEVER_DREAM:
            default:
                return DreamBackend.NEVER;
        }
    }

    static String getKeyFromSetting(@WhenToDream int dreamSetting) {
        switch (dreamSetting) {
            case DreamBackend.WHILE_CHARGING:
                return WHILE_CHARGING_ONLY;
            case DreamBackend.WHILE_DOCKED:
                return WHILE_DOCKED_ONLY;
            case DreamBackend.WHILE_CHARGING_OR_DOCKED:
                return EITHER_CHARGING_OR_DOCKED;
            case DreamBackend.WHILE_POSTURED:
                return WHILE_POSTURED_ONLY;
            case DreamBackend.NEVER:
            default:
                return NEVER_DREAM;
        }
    }

    static int getDreamSettingDescriptionResId(@WhenToDream int dreamSetting,
            boolean enabledOnBattery) {
        switch (dreamSetting) {
            case DreamBackend.WHILE_CHARGING:
                return R.string.screensaver_settings_summary_sleep;
            case DreamBackend.WHILE_DOCKED:
                return enabledOnBattery ? R.string.screensaver_settings_summary_dock
                        : R.string.screensaver_settings_summary_dock_and_charging;
            case DreamBackend.WHILE_CHARGING_OR_DOCKED:
                return R.string.screensaver_settings_summary_either_long;
            case DreamBackend.WHILE_POSTURED:
                return enabledOnBattery ? R.string.screensaver_settings_summary_postured
                        : R.string.screensaver_settings_summary_postured_and_charging;
            case DreamBackend.NEVER:
            default:
                return R.string.screensaver_settings_summary_never;
        }
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.DREAM;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.dream_fragment_overview;
    }

    @Override
    public @Nullable String getPreferenceScreenBindingKey(@NonNull Context context) {
        return ScreensaverScreen.KEY;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_screen_saver;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        if (mDreamPickerController == null) {
            mDreamPickerController = new DreamPickerController(context);
        }

        DreamBackend backend = DreamBackend.getInstance(context);
        if (mDreamHomeControlsPreferenceController == null) {
            mDreamHomeControlsPreferenceController =
                    new DreamHomeControlsPreferenceController(context, backend);
        }
        if (mLowLightModePreferenceController == null) {
            mLowLightModePreferenceController =
                    new LowLightModePreferenceController(context, backend);
        }

        controllers.add(mDreamPickerController);
        controllers.add(mDreamHomeControlsPreferenceController);
        controllers.add(mLowLightModePreferenceController);
        controllers.add(new WhenToDreamPreferenceController(context));
        return controllers;
    }

    public static CharSequence getSummaryTextWithDreamName(Context context) {
        DreamBackend backend = DreamBackend.getInstance(context);
        return getSummaryTextFromBackend(backend, context);
    }

    @VisibleForTesting
    static CharSequence getSummaryTextFromBackend(DreamBackend backend, Context context) {
        if (backend.isEnabled()) {
            return context.getString(R.string.screensaver_settings_summary_on,
                    backend.getActiveDreamName());
        } else {
            return context.getString(R.string.screensaver_settings_summary_off);
        }
    }

    @VisibleForTesting
    void setDreamPickerController(DreamPickerController dreamPickerController) {
        mDreamPickerController = dreamPickerController;
    }

    @VisibleForTesting
    void setDreamHomeControlsPreferenceController(DreamHomeControlsPreferenceController
            dreamHomeControlsPreferenceController) {
        mDreamHomeControlsPreferenceController = dreamHomeControlsPreferenceController;
    }

    @VisibleForTesting
    void setLowLightModePreferenceController(LowLightModePreferenceController controller) {
        mLowLightModePreferenceController = controller;
    }

    @VisibleForTesting
    Handler getHandler() {
        return mHandler;
    }

    private void refreshDreamsList() {
        if (mDreamPickerController != null) {
            mDreamPickerController.refreshDreamsList();
        }
    }

    private void setAllPreferencesEnabled(boolean isEnabled) {
        getPreferenceControllers().forEach(controllers -> {
            controllers.forEach(controller -> {
                final String prefKey = controller.getPreferenceKey();
                if (prefKey.equals(MAIN_SWITCH_PREF_KEY)) {
                    return;
                }
                if (prefKey.equals(DreamHomeControlsPreferenceController.PREF_KEY)) {
                    return;
                }
                final Preference pref = findPreference(prefKey);
                if (pref != null) {
                    pref.setEnabled(isEnabled);
                    controller.updateState(pref);
                }
            });
        });

        updateSelectedDreamSettingsState(isEnabled);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        final DreamBackend dreamBackend = DreamBackend.getInstance(getContext());

        mComplicationsTogglePreference = findPreference(
                DreamComplicationPreferenceController.PREF_KEY);

        mHomeControllerTogglePreference = findPreference(
                DreamHomeControlsPreferenceController.PREF_KEY
        );

        mMainSwitchPreference = findPreference(MAIN_SWITCH_PREF_KEY);
        if (mMainSwitchPreference != null) {
            mMainSwitchPreference.addOnSwitchChangeListener(this);
        }

        setAllPreferencesEnabled(dreamBackend.isEnabled());

        if (mDreamPickerController != null) {
            mDreamPickerController.addCallback(mCallback);
        }

        mPackageObserver = new PackageObserver(this::onPackagesChanged);

        // Remove the space preference manually only if the flag is enabled, which removes the
        // floating preview button, making the extra space unnecessary.
        if (dreamsV2()) {
            removePreference(SPACE_PREF_KEY);
        }
    }

    @Override
    public void onDestroy() {
        if (mDreamPickerController != null) {
            mDreamPickerController.removeCallback(mCallback);
        }

        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register to receive broadcasts for package changes so that the dreams list can be
        // refreshed when packages are installed, uninstalled, or updated.
        final IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        packageFilter.addDataScheme("package");
        getActivity().registerReceiver(mPackageObserver, packageFilter);

        refreshDreamsList();
    }

    @Override
    public void onPause() {
        super.onPause();
        getHandler().removeCallbacks(mRefreshDreamsListRunnable);
        getActivity().unregisterReceiver(mPackageObserver);
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
            Bundle bundle) {
        if (!dreamsV2()) {
            final DreamBackend dreamBackend = DreamBackend.getInstance(getContext());

            final ViewGroup root = getActivity().findViewById(android.R.id.content);
            mPreviewButton = (Button) getActivity().getLayoutInflater().inflate(
                    R.layout.dream_preview_button, root, false);
            mPreviewButton.setVisibility(dreamBackend.isEnabled() ? View.VISIBLE : View.GONE);
            root.addView(mPreviewButton);
            mPreviewButton.setOnClickListener(v -> dreamBackend
                    .preview(dreamBackend.getActiveDream()));
            updatePaddingForPreviewButton();
        }

        mRecyclerView = super.onCreateRecyclerView(inflater, parent, bundle);
        mRecyclerView.setFocusable(false);

        return mRecyclerView;
    }

    /** Refresh the dreams list (in case a dream has just been installed or removed). */
    private void onPackagesChanged() {
        // Delay before refreshing the dreams list to account for dreams that are hosted in a
        // different package than the one that is being changed (for example, the home controls
        // dream depends on the presence of the Home app, but is actually hosted in sysui).
        getHandler().postDelayed(mRefreshDreamsListRunnable, DREAMS_LIST_REFRESH_DELAY_MS);
    }

    /**
     * Updates the visibility and enabled state of preferences that depend on the currently selected
     * dream.
     */
    private void updateSelectedDreamSettingsState(boolean dreamsEnabled) {
        if (mDreamPickerController == null) {
            return;
        }

        final DreamBackend.DreamInfo activeDream = mDreamPickerController.getActiveDreamInfo();

        if (mComplicationsTogglePreference != null) {
            mComplicationsTogglePreference.setVisible(
                    activeDream != null && activeDream.supportsComplications);
        }

        if (mHomeControllerTogglePreference != null) {
            boolean isEnabled = dreamsEnabled
                                && (activeDream == null
                                || (activeDream.dreamCategory
                                & DreamService.DREAM_CATEGORY_HOME_PANEL) == 0)
                                && mDreamHomeControlsPreferenceController
                                    .getAvailabilityStatus() == BasePreferenceController.AVAILABLE;
            mHomeControllerTogglePreference.setEnabled(isEnabled);
        }
    }

    private void updatePaddingForPreviewButton() {
        mPreviewButton.post(() -> {
            mRecyclerView.setPadding(0, 0, 0, mPreviewButton.getMeasuredHeight());
        });
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        setAllPreferencesEnabled(isChecked);
        if (!dreamsV2()) {
            mPreviewButton.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            updatePaddingForPreviewButton();
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new SearchIndexProvider(R.xml.dream_fragment_overview);

    static class SearchIndexProvider extends BaseSearchIndexProvider {
        SearchIndexProvider(int xmlRes) {
            super(xmlRes);
        }

        @Override
        protected boolean isPageSearchEnabled(Context context) {
            return Utils.areDreamsAvailableToCurrentUser(context);
        }
    }

    private static class PackageObserver extends BroadcastReceiver {
        private final Runnable mCallback;

        PackageObserver(Runnable callback) {
            mCallback = callback;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mCallback.run();
        }
    }
}
// LINT.ThenChange(ScreensaverScreen.kt)
