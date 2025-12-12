/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.settings.testutils;

import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_GESTURE;
import static android.provider.Settings.Secure.ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_3BUTTON;
import static android.view.WindowManagerPolicyConstants.NAV_BAR_MODE_GESTURAL;

import static com.google.android.setupcompat.util.WizardManagerHelper.EXTRA_IS_SETUP_FLOW;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.testing.FragmentScenario;
import androidx.lifecycle.Lifecycle;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.accessibility.AccessibilityShortcutsTutorial;
import com.android.settings.accessibility.ShortcutPreference;
import com.android.settings.accessibility.shortcuts.EditShortcutsPreferenceFragment;
import com.android.settings.testutils.shadow.SettingsShadowResources;

import com.google.android.setupdesign.util.ThemeHelper;

import org.robolectric.shadows.ShadowLooper;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

/**
 * Utility class for common methods used in the accessibility feature related tests
 */
public class AccessibilityTestUtils {

    /**
     * Sets whether window magnification is supported.
     * Note: Calling needs to use [SettingsShadowResources] for this to work
     */
    public static void setWindowMagnificationSupported(Context context, boolean supported) {
        SettingsShadowResources.overrideResource(
                com.android.internal.R.bool.config_magnification_area, supported);
        shadowOf(context.getPackageManager())
                .setSystemFeature(PackageManager.FEATURE_WINDOW_MAGNIFICATION, supported);
    }

    public static void setSoftwareShortcutMode(
            Context context, boolean gestureNavEnabled, boolean floatingButtonEnabled) {
        int buttonMode = ACCESSIBILITY_BUTTON_MODE_NAVIGATION_BAR;
        if (floatingButtonEnabled) {
            buttonMode = ACCESSIBILITY_BUTTON_MODE_FLOATING_MENU;
        } else if (gestureNavEnabled) {
            buttonMode = ACCESSIBILITY_BUTTON_MODE_GESTURE;
        }
        int navMode = gestureNavEnabled ? NAV_BAR_MODE_GESTURAL : NAV_BAR_MODE_3BUTTON;

        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.ACCESSIBILITY_BUTTON_MODE, buttonMode, context.getUserId());
        Settings.Secure.putIntForUser(context.getContentResolver(),
                Settings.Secure.NAVIGATION_MODE, navMode, context.getUserId());
    }

    /**
     * Returns a mock {@link AccessibilityManager}
     */
    public static AccessibilityManager setupMockAccessibilityManager(Context mockContext) {
        AccessibilityManager am = mock(AccessibilityManager.class);
        when(mockContext.getSystemService(AccessibilityManager.class)).thenReturn(am);
        return am;
    }

    public static AccessibilityServiceInfo createAccessibilityServiceInfo(
            Context context, ComponentName componentName, boolean isAlwaysOnService) {
        final ApplicationInfo applicationInfo = new ApplicationInfo();
        applicationInfo.targetSdkVersion = Build.VERSION_CODES.R;
        final ServiceInfo serviceInfo = new ServiceInfo();
        applicationInfo.packageName = componentName.getPackageName();
        serviceInfo.packageName = componentName.getPackageName();
        serviceInfo.name = componentName.getClassName();
        serviceInfo.applicationInfo = applicationInfo;

        final ResolveInfo resolveInfo = new ResolveInfo();
        resolveInfo.serviceInfo = serviceInfo;
        try {
            final AccessibilityServiceInfo info = new AccessibilityServiceInfo(resolveInfo,
                    context);
            info.setComponentName(componentName);
            if (isAlwaysOnService) {
                info.flags |= AccessibilityServiceInfo.FLAG_REQUEST_ACCESSIBILITY_BUTTON;
            }
            return info;
        } catch (XmlPullParserException | IOException e) {
            // Do nothing
        }
        return null;
    }

    /**
     * Inflate the shortcut preference's UI for test.
     */
    public static PreferenceViewHolder inflateShortcutPreferenceView(
            @NonNull Context context, @NonNull ShortcutPreference pref) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(pref.getLayoutResource(), null);
        final PreferenceViewHolder viewHolder = PreferenceViewHolder.createInstanceForTests(view);
        inflater.inflate(
                pref.getWidgetLayoutResource(),
                viewHolder.itemView.findViewById(android.R.id.widget_frame));
        pref.onBindViewHolder(viewHolder);
        return viewHolder;
    }

    /**
     * Verify the shortcuts tutorial screen is shown
     *
     * @param fragment the Fragment where the dialog is launched from
     */
    public static void assertShortcutsTutorialDialogShown(Fragment fragment) {
        ShadowLooper.idleMainLooper();
        List<Fragment> fragments = fragment.getChildFragmentManager().getFragments();
        assertThat(fragments).isNotEmpty();
        assertThat(fragments).hasSize(1);
        assertThat(fragments.getFirst()).isInstanceOf(
                AccessibilityShortcutsTutorial.DialogFragment.class);
    }

    /**
     * Verify the dialog fragment is shown
     *
     * @param parentFragment the Fragment where the dialog is launched from
     * @param dialogFragmentClass the class of the dialog fragment to verify
     */
    public static <T extends DialogFragment> void assertDialogShown(
            Fragment parentFragment, Class<T> dialogFragmentClass) {
        ShadowLooper.idleMainLooper();
        List<Fragment> fragments = parentFragment.getChildFragmentManager().getFragments();
        assertThat(fragments).isNotEmpty();
        assertThat(fragments).hasSize(1);
        assertThat(fragments.getFirst()).isInstanceOf(dialogFragmentClass);
    }

    /**
     * Verify the EditShortcutsScreen is shown
     *
     * @param fragment the Fragment where the screen is launched from
     */
    public static void assertEditShortcutsScreenShown(Fragment fragment) {
        ShadowLooper.idleMainLooper();
        Intent intent = shadowOf((ContextWrapper) fragment.getContext()).peekNextStartedActivity();
        assertThat(intent).isNotNull();
        assertThat(intent.getExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT)).isEqualTo(
                EditShortcutsPreferenceFragment.class.getName());
    }

    /**
     * Launch a fragment with Setup wizard theme applied
     */
    public static <F extends Fragment> FragmentScenario<F> launchFragmentInSetupWizardFlow(
            Class<F> fragmentClass, @Nullable Bundle fragmentArgs) {
        FragmentScenario<F> fragmentScenario = FragmentScenario.launch(
                fragmentClass, fragmentArgs,
                androidx.appcompat.R.style.Theme_AppCompat, Lifecycle.State.INITIALIZED);
        fragmentScenario.onFragment(fragment -> {
            Activity activity = fragment.getActivity();
            activity.getIntent().putExtra(EXTRA_IS_SETUP_FLOW, true);
            activity.setTheme(R.style.SettingsPreferenceTheme_SetupWizard_Expressive);
            ThemeHelper.trySetSuwTheme(activity);
        });

        return fragmentScenario;
    }
}
