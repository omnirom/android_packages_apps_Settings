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

import static com.android.settings.inputmethod.TouchpadThreeFingerTapActionPreferenceController.SET_GESTURE;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.LAUNCHING_APP_KEY;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.SHARED_PREF_NAME;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.getCurrentGestureType;
import static com.android.settings.inputmethod.TouchpadThreeFingerTapUtils.setGestureType;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.KeyGestureEvent;
import android.os.Bundle;
import android.platform.test.annotations.DisableFlags;
import android.platform.test.annotations.EnableFlags;
import android.platform.test.flag.junit.SetFlagsRule;
import android.view.InputDevice;

import androidx.preference.PreferenceScreen;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.SettingsActivity;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.flags.Flags;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.ShadowInputDevice;
import com.android.settings.testutils.shadow.ShadowSystemSettings;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests for {@link TouchpadThreeFingerTapActionPreferenceController} */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {
        ShadowInputDevice.class,
        ShadowSystemSettings.class,
})
public class TouchpadThreeFingerTapActionPreferenceControllerTest {

    @Rule
    public final SetFlagsRule mSetFlagsRule = new SetFlagsRule();

    private static final String GO_HOME_KEY = "go_home";
    private static final String LAUNCH_APP_KEY = "launch_app";

    private static final int GO_HOME_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_HOME;
    private static final int LAUNCH_APP_GESTURE =
            KeyGestureEvent.KEY_GESTURE_TYPE_LAUNCH_APPLICATION;
    private static final int OTHER_GESTURE = KeyGestureEvent.KEY_GESTURE_TYPE_BACK;

    @Rule
    public MockitoRule mMockitoRule = MockitoJUnit.rule();

    @Mock
    private PreferenceScreen mMockScreen;
    @Mock
    private SelectorWithWidgetPreference mMockPref;
    @Mock
    private ContentObserver mMockContentObserver;
    @Mock
    private PackageManager mMockPackageManager;
    @Mock
    private Context mMockContext;
    @Mock
    private SharedPreferences mMockSharedPreferences;

    @Captor
    private ArgumentCaptor<Intent> mIntentCaptor;

    private ContentResolver mContentResolver;
    private FakeFeatureFactory mFeatureFactory;
    private TouchpadThreeFingerTapActionPreferenceController mController;

    @Before
    public void setup() {
        final Context context = ApplicationProvider.getApplicationContext();
        mContentResolver = context.getContentResolver();
        mFeatureFactory = FakeFeatureFactory.setupForTest();
        when(mMockContext.getContentResolver()).thenReturn(context.getContentResolver());
        when(mMockContext.getPackageManager()).thenReturn(mMockPackageManager);
        when(mMockContext.getSharedPreferences(eq(SHARED_PREF_NAME), anyInt()))
                .thenReturn(mMockSharedPreferences);
        addTouchpad();
    }

    private void addTouchpad() {
        int deviceId = 1;
        InputDevice device = ShadowInputDevice.makeInputDevicebyIdWithSources(deviceId,
                InputDevice.SOURCE_TOUCHPAD);
        ShadowInputDevice.addDevice(deviceId, device);
    }

    private void setupController(String preferenceKey) {
        final InputManager inputManager =
                ApplicationProvider.getApplicationContext().getSystemService(InputManager.class);
        mController = new TouchpadThreeFingerTapActionPreferenceController(
                mMockContext, preferenceKey, mMockContentObserver, inputManager);
        when(mMockScreen.findPreference(mController.getPreferenceKey())).thenReturn(mMockPref);
        when(mMockPref.getKey()).thenReturn(preferenceKey);
        mController.displayPreference(mMockScreen);
    }

    @Test
    public void updateState_whenPreferenceIsNotCurrentGesture_preferenceNotChecked() {
        setupController(/* preferenceKey= */ GO_HOME_KEY);
        setGestureType(mContentResolver, OTHER_GESTURE);

        mController.updateState(mMockPref);

        verify(mMockPref).setChecked(false);
    }

    @Test
    public void updateState_whenPreferenceMatchesCurrentGesture_preferenceChecked() {
        setupController(/* preferenceKey= */ GO_HOME_KEY);
        setGestureType(mContentResolver, GO_HOME_GESTURE);

        mController.updateState(mMockPref);

        verify(mMockPref).setChecked(true);
    }

    @Test
    public void onRadioButtonClick_gestureTypeUpdated() {
        setupController(/* preferenceKey= */ GO_HOME_KEY);
        mController.onRadioButtonClicked(mMockPref);

        int value = getCurrentGestureType(mContentResolver);

        assertThat(value).isEqualTo(GO_HOME_GESTURE);
    }

    /**
     * The following tests are for the app launching preference.
     * Separating these from the one with a basic action (Go Home) since it has more complex logic.
     */
    @Test
    @EnableFlags(Flags.FLAG_THREE_FINGER_TAP_APP_LAUNCH)
    public void isAppLaunchPref_getAvailabilityStatus_flagsEnabled_shouldReturnAvailable() {
        setupController(/* preferenceKey= */ LAUNCH_APP_KEY);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.AVAILABLE);
    }

    @Test
    @DisableFlags(Flags.FLAG_THREE_FINGER_TAP_APP_LAUNCH)
    public void isAppLaunchPref_getAvailabilityStatus_flagsDisabled_shouldReturnUnavailable() {
        setupController(/* preferenceKey= */ LAUNCH_APP_KEY);
        assertThat(mController.getAvailabilityStatus())
                .isEqualTo(BasePreferenceController.CONDITIONALLY_UNAVAILABLE);
    }

    @Test
    public void isAppLaunchPref_onRadioButtonClick_hasNoSelectedApp_launchAppSelection() {
        setupController(/* preferenceKey= */ LAUNCH_APP_KEY);
        setLaunchingApp(/* launchingApp= */ null);
        mController.onRadioButtonClicked(mMockPref);

        assertAppSelectionIsLaunched();
    }

    private void setLaunchingApp(ComponentName launchingApp) {
        String flattened = launchingApp == null ? null : launchingApp.flattenToString();
        when(mMockSharedPreferences.getString(eq(LAUNCHING_APP_KEY), any())).thenReturn(flattened);
    }

    private void assertAppSelectionIsLaunched() {
        verify(mMockContext).startActivity(mIntentCaptor.capture());

        Intent intent = mIntentCaptor.getValue();
        assertThat(intent).isNotNull();

        String dest = intent.getStringExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT);
        assertThat(dest).isEqualTo(TouchpadThreeFingerTapAppSelectionFragment.class.getName());

        Bundle args = intent.getBundleExtra(SettingsActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
        assertThat(args).isNotNull();
        assertTrue(args.getBoolean(SET_GESTURE));
    }

    @Test
    public void isAppLaunchPref_onRadioButtonClick_hasSelectedApp_gestureTypeUpdated() {
        setupController(/* preferenceKey= */ LAUNCH_APP_KEY);
        ComponentName app = new ComponentName("testPackage", "testClass");
        setLaunchingApp(app);
        mController.onRadioButtonClicked(mMockPref);

        // Shouldn't launch the app selection page
        Intent nextIntent = shadowOf(RuntimeEnvironment.getApplication()).getNextStartedActivity();
        assertThat(nextIntent).isNull();

        int gesture = getCurrentGestureType(mContentResolver);
        assertThat(gesture).isEqualTo(LAUNCH_APP_GESTURE);
        verify(mFeatureFactory.metricsFeatureProvider).action(any(),
                eq(SettingsEnums.ACTION_TOUCHPAD_THREE_FINGER_TAP_LAUNCHING_APP),
                eq("testPackage"));
    }

    @Test
    public void isAppLaunchPref_getSummary_hasSelectedApp_showsSelectedApp() {
        setupController(/* preferenceKey= */ LAUNCH_APP_KEY);
        ComponentName app = new ComponentName("testPackage", "testClass");
        setLaunchingApp(app);

        String expected = "testLabel";
        ApplicationInfo appInfo = mock(ApplicationInfo.class);
        try {
            when(mMockPackageManager.getApplicationInfo(eq(app.getPackageName()), anyInt()))
                    .thenReturn(appInfo);
            when(appInfo.loadLabel(any())).thenReturn(expected);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
        assertEquals(expected, mController.getSummary().toString());
    }
}
