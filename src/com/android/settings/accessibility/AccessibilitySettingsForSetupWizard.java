/*
 * Copyright (C) 2015 The Android Open Source Project
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

import static com.android.settings.Utils.getAdaptiveIcon;
import static com.android.settings.accessibility.AccessibilityUtil.AccessibilityServiceFragmentType.VOLUME_SHORTCUT_TOGGLE;
import static com.android.settingslib.widget.TwoTargetPreference.ICON_SIZE_MEDIUM;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.LinearLayout;

import androidx.preference.Preference;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.RestrictedPreference;

import com.google.android.setupdesign.GlifPreferenceLayout;
import com.google.android.setupdesign.util.ThemeHelper;

import java.util.List;

/**
 * Activity with the accessibility settings specific to Setup Wizard.
 */
public class AccessibilitySettingsForSetupWizard extends SettingsPreferenceFragment
        implements Preference.OnPreferenceChangeListener {

    // Preferences.
    private static final String DISPLAY_MAGNIFICATION_PREFERENCE =
            "screen_magnification_preference";
    private static final String SCREEN_READER_PREFERENCE = "screen_reader_preference";
    private static final String SELECT_TO_SPEAK_PREFERENCE = "select_to_speak_preference";

    // Package names and service names used to identify screen reader and SelectToSpeak services.
    private static final String SCREEN_READER_PACKAGE_NAME = "com.google.android.marvin.talkback";
    private static final String SCREEN_READER_SERVICE_NAME =
            "com.google.android.marvin.talkback.TalkBackService";
    private static final String SELECT_TO_SPEAK_PACKAGE_NAME = "com.google.android.marvin.talkback";
    private static final String SELECT_TO_SPEAK_SERVICE_NAME =
            "com.google.android.accessibility.selecttospeak.SelectToSpeakService";

    // Preference controls.
    private Preference mDisplayMagnificationPreference;
    private RestrictedPreference mScreenReaderPreference;
    private RestrictedPreference mSelectToSpeakPreference;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SUW_ACCESSIBILITY;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final GlifPreferenceLayout layout = (GlifPreferenceLayout) view;
        layout.setDividerInsets(Integer.MAX_VALUE, 0);
        layout.setDescriptionText(R.string.vision_settings_description);
        layout.setHeaderText(R.string.vision_settings_title);
        layout.setIcon(getPrefContext().getDrawable(R.drawable.ic_accessibility_visibility));

        if (ThemeHelper.shouldApplyExtendedPartnerConfig(getActivity())) {
            final LinearLayout headerLayout = layout.findManagedViewById(R.id.sud_layout_header);
            if (headerLayout != null) {
                headerLayout.setPadding(0, headerLayout.getPaddingTop(), 0,
                        headerLayout.getPaddingBottom());
            }
        }
    }

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent,
        Bundle savedInstanceState) {
        final GlifPreferenceLayout layout = (GlifPreferenceLayout) parent;
        return layout.onCreateRecyclerView(inflater, parent, savedInstanceState);
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.accessibility_settings_for_setup_wizard);

        mDisplayMagnificationPreference = findPreference(DISPLAY_MAGNIFICATION_PREFERENCE);
        mScreenReaderPreference = findPreference(SCREEN_READER_PREFERENCE);
        mSelectToSpeakPreference = findPreference(SELECT_TO_SPEAK_PREFERENCE);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateAccessibilityServicePreference(mScreenReaderPreference,
                SCREEN_READER_PACKAGE_NAME, SCREEN_READER_SERVICE_NAME,
                VolumeShortcutToggleScreenReaderPreferenceFragmentForSetupWizard.class.getName());
        updateAccessibilityServicePreference(mSelectToSpeakPreference,
                SELECT_TO_SPEAK_PACKAGE_NAME, SELECT_TO_SPEAK_SERVICE_NAME,
                VolumeShortcutToggleSelectToSpeakPreferenceFragmentForSetupWizard.class.getName());
        configureMagnificationPreferenceIfNeeded(mDisplayMagnificationPreference);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(false);
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return false;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (mDisplayMagnificationPreference == preference) {
            Bundle extras = mDisplayMagnificationPreference.getExtras();
            extras.putBoolean(AccessibilitySettings.EXTRA_LAUNCHED_FROM_SUW, true);
        }

        return super.onPreferenceTreeClick(preference);
    }

    private AccessibilityServiceInfo findService(String packageName, String serviceName) {
        final AccessibilityManager manager =
                getActivity().getSystemService(AccessibilityManager.class);
        final List<AccessibilityServiceInfo> accessibilityServices =
                manager.getInstalledAccessibilityServiceList();
        for (AccessibilityServiceInfo info : accessibilityServices) {
            ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
            if (packageName.equals(serviceInfo.packageName)
                    && serviceName.equals(serviceInfo.name)) {
                return info;
            }
        }

        return null;
    }

    private void updateAccessibilityServicePreference(RestrictedPreference preference,
            String packageName, String serviceName, String targetFragment) {
        final AccessibilityServiceInfo info = findService(packageName, serviceName);
        if (info == null) {
            getPreferenceScreen().removePreference(preference);
            return;
        }

        final ServiceInfo serviceInfo = info.getResolveInfo().serviceInfo;
        final Drawable icon = info.getResolveInfo().loadIcon(getPackageManager());
        preference.setIcon(getAdaptiveIcon(getContext(), icon, Color.WHITE));
        preference.setIconSize(ICON_SIZE_MEDIUM);
        final String title = info.getResolveInfo().loadLabel(getPackageManager()).toString();
        preference.setTitle(title);
        final ComponentName componentName =
                new ComponentName(serviceInfo.packageName, serviceInfo.name);
        preference.setKey(componentName.flattenToString());
        if (AccessibilityUtil.getAccessibilityServiceFragmentType(info) == VOLUME_SHORTCUT_TOGGLE) {
            preference.setFragment(targetFragment);
        }

        // Update the extras.
        final Bundle extras = preference.getExtras();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);

        extras.putString(AccessibilitySettings.EXTRA_PREFERENCE_KEY,
            preference.getKey());
        extras.putString(AccessibilitySettings.EXTRA_TITLE, title);

        final String description = info.loadDescription(getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_SUMMARY, description);

        extras.putInt(AccessibilitySettings.EXTRA_ANIMATED_IMAGE_RES, info.getAnimatedImageRes());

        final String htmlDescription = info.loadHtmlDescription(getPackageManager());
        extras.putString(AccessibilitySettings.EXTRA_HTML_DESCRIPTION, htmlDescription);
    }

    private static void configureMagnificationPreferenceIfNeeded(Preference preference) {
        final Context context = preference.getContext();
        preference.setFragment(
                ToggleScreenMagnificationPreferenceFragmentForSetupWizard.class.getName());
        final Bundle extras = preference.getExtras();
        MagnificationGesturesPreferenceController
                .populateMagnificationGesturesPreferenceExtras(extras, context);
    }
}
