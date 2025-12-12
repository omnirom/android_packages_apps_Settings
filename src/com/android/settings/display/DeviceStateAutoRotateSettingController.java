/*
 * Copyright (C) 2022 The Android Open Source Project
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

package com.android.settings.display;

import static androidx.lifecycle.Lifecycle.Event.ON_START;
import static androidx.lifecycle.Lifecycle.Event.ON_STOP;

import android.app.settings.SettingsEnums;
import android.content.Context;

import androidx.annotation.VisibleForTesting;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;
import androidx.preference.TwoStatePreference;

import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.devicestate.DeviceStateAutoRotateSettingManager;
import com.android.settingslib.search.SearchIndexableRaw;

import java.util.List;

/** Controller for device state based auto rotation preferences. */
public class DeviceStateAutoRotateSettingController extends TogglePreferenceController implements
        LifecycleObserver {

    private TwoStatePreference mPreference;

    @NonNull
    private final DeviceStateAutoRotateSettingManager mAutoRotateSettingsManager;
    private final int mOrder;
    private final DeviceStateAutoRotateSettingManager.DeviceStateAutoRotateSettingListener
            mDeviceStateAutoRotateSettingListener = () -> updateState(mPreference);
    private final int mDeviceState;
    @NonNull
    private final String mDeviceStateDescription;
    @NonNull
    private final MetricsFeatureProvider mMetricsFeatureProvider;

    @VisibleForTesting
    DeviceStateAutoRotateSettingController(@NonNull Context context, int deviceState,
            @NonNull String deviceStateDescription, int order,
            @NonNull MetricsFeatureProvider metricsFeatureProvider,
            @Nullable DeviceStateAutoRotateSettingManager deviceStateAutoRotateSettingManager) {
        super(context, getPreferenceKeyForDeviceState(deviceState));

        if (deviceStateAutoRotateSettingManager == null) {
            throw new IllegalStateException("DeviceStateAutoRotateSettingController should not be "
                    + "created when DeviceStateAutoRotateSettingManager is not present");
        }

        mMetricsFeatureProvider = metricsFeatureProvider;
        mDeviceState = deviceState;
        mDeviceStateDescription = deviceStateDescription;
        mAutoRotateSettingsManager = deviceStateAutoRotateSettingManager;
        mOrder = order;
    }

    public DeviceStateAutoRotateSettingController(@NonNull Context context, int deviceState,
            @NonNull String deviceStateDescription, int order) {
        this(context, deviceState, deviceStateDescription, order,
                FeatureFactory.getFeatureFactory().getMetricsFeatureProvider(),
                DeviceStateAutoRotateSettingManagerProvider.getSingletonInstance(context));
    }

    void init(Lifecycle lifecycle) {
        lifecycle.addObserver(this);
    }

    @OnLifecycleEvent(ON_START)
    void onStart() {
        mAutoRotateSettingsManager.registerListener(mDeviceStateAutoRotateSettingListener);
    }

    @OnLifecycleEvent(ON_STOP)
    void onStop() {
        mAutoRotateSettingsManager.unregisterListener(mDeviceStateAutoRotateSettingListener);
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        mPreference = new SwitchPreferenceCompat(mContext);
        mPreference.setTitle(mDeviceStateDescription);
        mPreference.setKey(getPreferenceKey());
        mPreference.setOrder(mOrder);
        screen.addPreference(mPreference);
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return DeviceStateAutoRotationHelper.isDeviceStateRotationEnabled(mContext)
                ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public String getPreferenceKey() {
        return getPreferenceKeyForDeviceState(mDeviceState);
    }

    private static String getPreferenceKeyForDeviceState(int deviceState) {
        return "auto_rotate_device_state_" + deviceState;
    }

    @Override
    public boolean isChecked() {
        // It is highly unlikely to receive null value here. In the improbable event of null, a
        // non-null update will follow shortly, and the users will potentially see incorrect
        // state for a short time.
        final Boolean isRotationLocked = mAutoRotateSettingsManager.isRotationLocked(mDeviceState);
        return isRotationLocked != null && !isRotationLocked;
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        boolean isRotationLocked = !isChecked;
        logSettingChanged(isChecked);
        mAutoRotateSettingsManager.updateSetting(mDeviceState, isRotationLocked);
        return true;
    }

    private void logSettingChanged(boolean isChecked) {
        boolean isRotationLocked = !isChecked;
        mMetricsFeatureProvider.action(mContext, SettingsEnums.ACTION_ROTATION_LOCK,
                isRotationLocked);

        int actionCategory = isChecked
                ? SettingsEnums.ACTION_ENABLE_AUTO_ROTATION_DEVICE_STATE
                : SettingsEnums.ACTION_DISABLE_AUTO_ROTATION_DEVICE_STATE;
        mMetricsFeatureProvider.action(mContext, actionCategory, /* value= */ mDeviceState);
    }

    @Override
    public void updateRawDataToIndex(List<SearchIndexableRaw> rawData) {
        SearchIndexableRaw indexable = new SearchIndexableRaw(mContext);
        indexable.key = getPreferenceKey();
        indexable.title = mDeviceStateDescription;
        // Maybe pass screen title as param?
        indexable.screenTitle = mContext.getString(R.string.accelerometer_title);
        rawData.add(indexable);
    }

    @Override
    public int getSliceHighlightMenuRes() {
        return R.string.menu_key_display;
    }

    @Override
    public boolean isSliceable() {
        return true; // Maybe set to false if in accessibility settings screen
    }

    @Override
    public boolean isPublicSlice() {
        return true;
    }
}
