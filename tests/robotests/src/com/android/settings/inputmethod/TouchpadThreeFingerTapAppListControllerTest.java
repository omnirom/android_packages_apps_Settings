/*
 * Copyright 2025 The Android Open Source Project
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

package com.android.settings.inputmethod;

import static android.hardware.input.InputManager.CUSTOM_INPUT_GESTURE_RESULT_SUCCESS;

import static com.android.settings.inputmethod.TouchpadThreeFingerTapActionPreferenceController.SET_GESTURE;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.DEFAULT_GESTURE_TYPE;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.LAUNCHING_APP_KEY;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.TRIGGER;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getCurrentGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.isGestureTypeLaunchApp;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setLaunchAppAsGestureType;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.hardware.input.AppLaunchData.ComponentData;
import android.hardware.input.InputGestureData;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.os.Bundle;
import android.os.UserHandle;

import androidx.preference.PreferenceScreen;
import androidx.preference.TwoStatePreference;

import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

/** Tests for {@link TouchpadThreeFingerTapAppListController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowSystemSettings.class,
})
public class TouchpadThreeFingerTapAppListControllerTest {

    private static final String PREF_KEY = "testScreen";
    private static final String TEST_TITLE_PREFIX = "testTitle";
    private static final String TEST_PACKAGE_PREFIX = "testPackage";
    private static final String TEST_CLASS_PREFIX = "testClass";
    private static final int GO_HOME_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_HOME;
    private static final InputGestureData.Filter TOUCHPAD_FILTER = InputGestureData.Filter.TOUCHPAD;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mMockPreferenceScreen;

    @Mock
    private ContentObserver mMockContentObserver;

    @Mock
    private LauncherApps mMockLauncherApps;
    @Mock
    private LauncherActivityInfo mMockActivityInfo1;
    @Mock
    private LauncherActivityInfo mMockActivityInfo2;
    @Mock
    private Drawable mMockDrawable;

    @Mock
    private InputManager mMockInputManager;

    @Mock
    private SharedPreferences mMockSharedPreferences;
    @Mock
    private SharedPreferences.Editor mMockEditor;

    private final Context mContext = RuntimeEnvironment.application;
    private ContentResolver mContentResolver;
    private TouchpadThreeFingerTapAppListController mController;
    private InputGestureData mCustomInputGesture;
    private LauncherApps.Callback mLauncherAppsCallback;
    private FakeFeatureFactory mFeatureFactory;

    @Before
    public void setup() {
        mContentResolver = mContext.getContentResolver();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        mController = new TouchpadThreeFingerTapAppListController(
                mContext, PREF_KEY,
                mMockLauncherApps, mMockInputManager, mMockSharedPreferences, mMockContentObserver);
        setupMockLauncherApps();
        setupMockInputManager();
        when(mMockSharedPreferences.edit()).thenReturn(mMockEditor);
    }

    private void setupMockInputManager() {
        doAnswer(
                invocation -> {
                    mCustomInputGesture = null;
                    return null;
                }
        ).when(mMockInputManager).removeAllCustomInputGestures(eq(TOUCHPAD_FILTER));

        doAnswer(
                invocation -> {
                    mCustomInputGesture = invocation.getArgument(0);
                    return CUSTOM_INPUT_GESTURE_RESULT_SUCCESS;
                }
        ).when(mMockInputManager).addCustomInputGesture(any(InputGestureData.class));

        doAnswer(
                invocation -> {
                    return mCustomInputGesture;
                }
        ).when(mMockInputManager).getInputGesture(eq(TRIGGER));
    }

    private void setupMockLauncherApps() {
        List<LauncherActivityInfo> activityInfos = new ArrayList<>();
        activityInfos.add(mMockActivityInfo1);
        activityInfos.add(mMockActivityInfo2);
        when(mMockLauncherApps.getActivityList(isNull(), any(UserHandle.class)))
                .thenReturn(activityInfos);

        for (int i = 0; i < activityInfos.size(); i++) {
            setupMockActivityInfo(activityInfos.get(i), i);
        }
    }

    private void setupMockActivityInfo(LauncherActivityInfo activityInfo, int suffix) {
        when(activityInfo.getLabel()).thenReturn(TEST_TITLE_PREFIX + suffix);
        when(activityInfo.getComponentName()).thenReturn(new ComponentName(
                TEST_PACKAGE_PREFIX + suffix, TEST_CLASS_PREFIX + suffix));
        when(activityInfo.getIcon(anyInt())).thenReturn(mMockDrawable);
    }

    @Test
    public void displayPreference_populateAllApps() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);

        mController.displayPreference(mMockPreferenceScreen);

        verify(mMockPreferenceScreen).removeAll();
        verify(mMockPreferenceScreen, times(2)).addPreference(captor.capture());

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs).hasSize(2);

        for (int i = 0; i < prefs.size(); i++) {
            SelectorWithWidgetPreference pref = prefs.get(i);
            assertTrue((TEST_TITLE_PREFIX + i).contentEquals(pref.getTitle()));
            assertThat(pref.getIcon()).isEqualTo(mMockDrawable);

            ComponentName component = new ComponentName(
                    TEST_PACKAGE_PREFIX + i, TEST_CLASS_PREFIX + i);
            String key = component.flattenToString();
            assertThat(pref.getKey()).isEqualTo(key);
        }
    }

    @Test
    public void updateState_whenActionIsLaunchApp_correspondingAppChecked() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();

        ComponentName componentName = getComponentName(/* matchingIndex= */ 1);
        setLaunchingApp(componentName);
        mController.updateState(mMockPreferenceScreen);

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs.get(0).isChecked()).isFalse();
        assertThat(prefs.get(1).isChecked()).isTrue();
    }

    private void setLaunchingApp(ComponentName launchingApp) {
        String flattened = launchingApp == null ? null : launchingApp.flattenToString();
        when(mMockSharedPreferences.getString(eq(LAUNCHING_APP_KEY), any())).thenReturn(flattened);
    }

    @Test
    public void updateState_whenActionIsGoHome_nothingChecked() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();

        setGestureType(mContentResolver, GO_HOME_GESTURE);
        mController.updateState(mMockPreferenceScreen);

        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertTrue(prefs.stream().noneMatch(TwoStatePreference::isChecked));
    }

    @Test
    public void onRadioButtonClick_gestureAndTargetAppUpdated() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor = capturePrefs();
        setDisplayPreferenceExtra(/* shouldSetGesture= */ true);

        int clickingIndex = 0;
        ComponentName expectedApp = new ComponentName(
                TEST_PACKAGE_PREFIX + clickingIndex, TEST_CLASS_PREFIX + clickingIndex);
        setLaunchingApp(expectedApp);

        mController.onRadioButtonClicked(captor.getAllValues().get(clickingIndex));

        // SharedPref is updated
        verifySharedPrefEdited(/* launchingApp= */ expectedApp);

        // Settings key is updated
        assertTrue(isGestureTypeLaunchApp(mContentResolver));

        // InputManager gesture is updated
        assertThat(mCustomInputGesture).isNotNull();
        ComponentData componentData =
                (ComponentData) mCustomInputGesture.getAction().appLaunchData();
        assertThat(componentData).isNotNull();
        assertThat(componentData.getPackageName()).isEqualTo(expectedApp.getPackageName());
        assertThat(componentData.getClassName()).isEqualTo(expectedApp.getClassName());

        // The pref list is updated accordingly
        List<SelectorWithWidgetPreference> prefs = captor.getAllValues();
        assertThat(prefs.get(0).isChecked()).isTrue();
        assertThat(prefs.get(1).isChecked()).isFalse();

        // The app selection metrics is tracked
        verify(mFeatureFactory.metricsFeatureProvider).action(any(),
                        eq(SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP),
                        eq(expectedApp.getPackageName()));

    }

    private void verifySharedPrefEdited(ComponentName launchingApp) {
        String flattened = launchingApp == null ? null : launchingApp.flattenToString();
        verify(mMockEditor).putString(eq(LAUNCHING_APP_KEY), eq(flattened));
        verify(mMockEditor).apply();
    }

    private void setDisplayPreferenceExtra(boolean shouldSetGesture) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(SET_GESTURE, shouldSetGesture);
        when(mMockPreferenceScreen.getExtras()).thenReturn(bundle);
    }

    @Test
    public void onPackageRemoved_isNotSelectedApp_gestureIsAppLaunch_doNothing() {
        ArgumentCaptor<LauncherApps.Callback> captor =
                ArgumentCaptor.forClass(LauncherApps.Callback.class);
        verify(mMockLauncherApps).registerCallback(captor.capture());
        mLauncherAppsCallback = captor.getValue();

        int selectedAppIndex = 0;
        ComponentName componentName = getComponentName(/* matchingIndex= */ selectedAppIndex);
        setLaunchingApp(componentName);
        setLaunchAppAsGestureType(mContentResolver, mMockInputManager, componentName);
        mController.updateState(mMockPreferenceScreen);

        mLauncherAppsCallback.onPackageRemoved(
                TEST_PACKAGE_PREFIX + 1, UserHandle.CURRENT);

        assertTrue(isGestureTypeLaunchApp(mContentResolver));
    }

    @Test
    public void onPackageRemoved_isSelectedApp_gestureIsNotAppLaunch_doNothing() {
        ArgumentCaptor<LauncherApps.Callback> captor =
                ArgumentCaptor.forClass(LauncherApps.Callback.class);
        verify(mMockLauncherApps).registerCallback(captor.capture());
        mLauncherAppsCallback = captor.getValue();

        int selectedAppIndex = 0;
        ComponentName componentName = getComponentName(/* matchingIndex= */ selectedAppIndex);
        setLaunchingApp(componentName);
        setGestureType(mContentResolver, GO_HOME_GESTURE);
        mController.updateState(mMockPreferenceScreen);

        mLauncherAppsCallback.onPackageRemoved(
                TEST_PACKAGE_PREFIX + selectedAppIndex, UserHandle.CURRENT);

        verifySharedPrefEdited(/* launchingApp= */ null);
        assertThat(getCurrentGestureType(mContentResolver)).isEqualTo(GO_HOME_GESTURE);
    }

    @Test
    public void onPackageRemoved_isSelectedApp_setToDefaultGesture() {
        ArgumentCaptor<LauncherApps.Callback> captor =
                ArgumentCaptor.forClass(LauncherApps.Callback.class);
        verify(mMockLauncherApps).registerCallback(captor.capture());
        mLauncherAppsCallback = captor.getValue();

        int selectedAppIndex = 0;
        ComponentName componentName = getComponentName(/* matchingIndex= */ selectedAppIndex);
        setLaunchingApp(componentName);
        setLaunchAppAsGestureType(
                mContentResolver, mMockInputManager, componentName);
        mController.updateState(mMockPreferenceScreen);

        mLauncherAppsCallback.onPackageRemoved(
                TEST_PACKAGE_PREFIX + selectedAppIndex, UserHandle.CURRENT);
        verifySharedPrefEdited(/* launchingApp= */ null);

        // Settings key is updated
        int gesture = getCurrentGestureType(mContentResolver);
        assertThat(gesture).isEqualTo(DEFAULT_GESTURE_TYPE);
    }

    private ArgumentCaptor<SelectorWithWidgetPreference> capturePrefs() {
        ArgumentCaptor<SelectorWithWidgetPreference> captor =
                ArgumentCaptor.forClass(SelectorWithWidgetPreference.class);
        mController.displayPreference(mMockPreferenceScreen);
        verify(mMockPreferenceScreen, times(2)).addPreference(captor.capture());
        when(mMockPreferenceScreen.getPreferenceCount()).thenReturn(captor.getAllValues().size());
        when(mMockPreferenceScreen.getPreference(eq(0)))
                .thenReturn(captor.getAllValues().get(0));
        when(mMockPreferenceScreen.getPreference(eq(1)))
                .thenReturn(captor.getAllValues().get(1));
        return captor;
    }

    private ComponentName getComponentName(int matchingIndex) {
        return new ComponentName(
                TEST_PACKAGE_PREFIX + matchingIndex, TEST_CLASS_PREFIX + matchingIndex);
    }
}
