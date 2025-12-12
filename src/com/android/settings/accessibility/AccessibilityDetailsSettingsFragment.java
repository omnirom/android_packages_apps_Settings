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

import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_BUTTON_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME;
import static com.android.internal.accessibility.AccessibilityShortcutController.MAGNIFICATION_COMPONENT_NAME;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.AccessibilityShortcutInfo;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.admin.DevicePolicyManager;
import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.annotations.VisibleForTesting;
import com.android.settings.accessibility.data.AccessibilityRepositoryProvider;
import com.android.settings.accessibility.detail.a11yservice.A11yServicePreferenceFragment;
import com.android.settings.accessibility.screenmagnification.ui.MagnificationPreferenceFragment;
import com.android.settings.core.InstrumentedFragment;
import com.android.settings.core.SubSettingLauncher;
import com.android.settingslib.RestrictedLockUtilsInternal;

import java.util.List;
import java.util.Objects;

public class AccessibilityDetailsSettingsFragment extends InstrumentedFragment {

    private final static String TAG = "A11yDetailsSettings";
    private AppOpsManager mAppOps;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.ACCESSIBILITY_DETAILS_SETTINGS;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAppOps = getActivity().getSystemService(AppOpsManager.class);

        // In case the Intent doesn't have component name, go to a11y services list.
        final String extraComponentName = getActivity().getIntent().getStringExtra(
                Intent.EXTRA_COMPONENT_NAME);
        if (extraComponentName == null) {
            Log.w(TAG, "Open accessibility services list due to no component name.");
            openAccessibilitySettingsAndFinish();
            return;
        }

        final ComponentName componentName = ComponentName.unflattenFromString(extraComponentName);
        if (openSystemAccessibilitySettingsAndFinish(componentName)) {
            return;
        }

        if (openA11yActivityDetailsAndFinish(componentName)) {
            return;
        }

        if (openA11yServiceDetailsAndFinish(componentName)) {
            return;
        }
        // Fall back to open accessibility services list.
        openAccessibilitySettingsAndFinish();
    }

    private boolean openSystemAccessibilitySettingsAndFinish(
            @Nullable ComponentName componentName) {
        final LaunchFragmentArguments launchArguments =
                getSystemAccessibilitySettingsLaunchArguments(componentName);
        if (launchArguments == null) {
            return false;
        }
        openSubSettings(launchArguments.mDestination, launchArguments.mArguments);
        finish();
        return true;
    }

    @Nullable
    private LaunchFragmentArguments getSystemAccessibilitySettingsLaunchArguments(
            @Nullable ComponentName componentName) {
        if (MAGNIFICATION_COMPONENT_NAME.equals(componentName)) {
            final String destination = MagnificationPreferenceFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        if (ACCESSIBILITY_BUTTON_COMPONENT_NAME.equals(componentName)) {
            final String destination = AccessibilityButtonFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        if (ACCESSIBILITY_HEARING_AIDS_COMPONENT_NAME.equals(componentName)) {
            final String destination = AccessibilityHearingAidsFragment.class.getName();
            return new LaunchFragmentArguments(destination, /* arguments= */ null);
        }

        return null;
    }


    private void openAccessibilitySettingsAndFinish() {
        openSubSettings(AccessibilitySettings.class.getName(), /* arguments= */ null);
        finish();
    }

    private boolean openA11yActivityDetailsAndFinish(@Nullable ComponentName componentName) {
        // In case the AccessibilityShortcutInfo doesn't exist, go to ally settings screen.
        final AccessibilityShortcutInfo shortcutInfo =
                componentName != null ? AccessibilityRepositoryProvider.get(
                        requireContext()).getAccessibilityShortcutInfo(componentName) : null;
        if (shortcutInfo == null) {
            Log.w(TAG, "openA11yActivityDetailsAndFinish : invalid component name.");
            return false;
        }
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);
        openSubSettings(LaunchAccessibilityActivityPreferenceFragment.class.getName(), bundle);
        finish();
        return true;
    }

    private boolean openA11yServiceDetailsAndFinish(
            @Nullable ComponentName componentName) {
        // In case the A11yServiceInfo doesn't exist, go to ally services list.
        final AccessibilityServiceInfo info =
                componentName != null ? AccessibilityRepositoryProvider.get(
                        requireContext()).getAccessibilityServiceInfo(componentName) : null;
        if (info == null) {
            Log.w(TAG, "openA11yServiceDetailsAndFinish : invalid component name.");
            return false;
        }

        // In case this accessibility service isn't permitted, go to a11y services list.
        if (!isServiceAllowed(info.getResolveInfo().serviceInfo.applicationInfo.uid,
                componentName.getPackageName())) {
            Log.w(TAG,
                    "openA11yServiceDetailsAndFinish: target accessibility service is"
                            + "prohibited by Device Admin or App Op.");
            return false;
        }
        openSubSettings(A11yServicePreferenceFragment.class.getName(),
                buildArguments(info));
        finish();
        return true;
    }

    private void openSubSettings(@NonNull String destination, @Nullable Bundle arguments) {
        new SubSettingLauncher(getActivity())
                .setDestination(destination)
                .setSourceMetricsCategory(getMetricsCategory())
                .setArguments(arguments)
                .launch();
    }

    @VisibleForTesting
    boolean isServiceAllowed(int uid, String packageName) {
        final DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
        final List<String> permittedServices = dpm.getPermittedAccessibilityServices(
                UserHandle.myUserId());
        if (permittedServices != null && !permittedServices.contains(packageName)) {
            return false;
        }

        return !RestrictedLockUtilsInternal.isEnhancedConfirmationRestricted(getContext(),
                packageName, AppOpsManager.OPSTR_BIND_ACCESSIBILITY_SERVICE);
    }

    private Bundle buildArguments(AccessibilityServiceInfo info) {
        final ResolveInfo resolveInfo = info.getResolveInfo();
        final ServiceInfo serviceInfo = resolveInfo.serviceInfo;
        final String packageName = serviceInfo.packageName;
        final ComponentName componentName = new ComponentName(packageName, serviceInfo.name);

        final Bundle extras = new Bundle();
        extras.putParcelable(AccessibilitySettings.EXTRA_COMPONENT_NAME, componentName);
        // We will log nonA11yTool status from PolicyWarningUIController; others none.
        extras.putLong(AccessibilitySettings.EXTRA_TIME_FOR_LOGGING,
                getActivity().getIntent().getLongExtra(
                        AccessibilitySettings.EXTRA_TIME_FOR_LOGGING, 0));
        return extras;
    }

    private void finish() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        activity.finish();
    }

    private static class LaunchFragmentArguments {
        final String mDestination;
        final Bundle mArguments;

        LaunchFragmentArguments(@NonNull String destination, @Nullable Bundle arguments) {
            mDestination = Objects.requireNonNull(destination);
            mArguments = arguments;
        }
    }
}
