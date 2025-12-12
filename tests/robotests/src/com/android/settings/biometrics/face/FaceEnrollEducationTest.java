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

package com.android.settings.biometrics.face;

import static android.util.DisplayMetrics.DENSITY_DEFAULT;
import static android.util.DisplayMetrics.DENSITY_XXXHIGH;

import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_action_label_pause;
import static com.android.settingslib.widget.preference.illustration.R.string.settingslib_illustration_content_description;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_KEY_NEXT_LAUNCHED;
import static com.android.settings.biometrics.BiometricEnrollBase.EXTRA_LAUNCHED_POSTURE_GUIDANCE;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_CLOSED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_OPENED;
import static com.android.settings.biometrics.BiometricUtils.DEVICE_POSTURE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.hardware.face.FaceManager;
import android.view.View;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;

import com.android.settings.R;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.shadow.SettingsShadowResources;
import com.android.settings.testutils.shadow.ShadowUtils;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.setupcompat.template.FooterBarMixin;
import com.google.android.setupcompat.template.FooterButton;
import com.google.android.setupdesign.GlifLayout;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowUtils.class, SettingsShadowResources.class})
public class FaceEnrollEducationTest {
    @Mock
    private FaceManager mFaceManager;

    private Context mContext;
    private ActivityScenario<TestFaceEnrollEducation> mScenario;
    private FakeFeatureFactory mFakeFeatureFactory;

    public static class TestFaceEnrollEducation extends FaceEnrollEducation {

        @Override
        protected boolean launchPostureGuidance() {
            return super.launchPostureGuidance();
        }
    }

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        ShadowUtils.setFaceManager(mFaceManager);
        mContext = ApplicationProvider.getApplicationContext();
        mFakeFeatureFactory = FakeFeatureFactory.setupForTest();
    }

    @After
    public void tearDown() {
        ShadowUtils.reset();
        if (mScenario != null) {
            mScenario.close();
        }
    }

    private void setupActivityForPosture() {
        Context appContext = ApplicationProvider.getApplicationContext();
        final Intent testIntent = new Intent(appContext, TestFaceEnrollEducation.class);
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);
        testIntent.putExtra(EXTRA_KEY_NEXT_LAUNCHED, false);
        testIntent.putExtra(EXTRA_LAUNCHED_POSTURE_GUIDANCE, false);

        final Intent postureGuidanceProviderIntent = new Intent(); // Intent for the mock provider
        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                postureGuidanceProviderIntent);

        mScenario = ActivityScenario.launch(testIntent);
    }

    private void setupActivity() {
        Context appContext = ApplicationProvider.getApplicationContext();
        final Intent testIntent = new Intent(appContext, TestFaceEnrollEducation.class);
        // Set the challenge token so the confirm screen will not be shown
        testIntent.putExtra(ChooseLockSettingsHelper.EXTRA_KEY_CHALLENGE_TOKEN, new byte[0]);

        when(mFakeFeatureFactory.mFaceFeatureProvider.getPostureGuidanceIntent(any())).thenReturn(
                null /* Simulate no posture intent */);

        mScenario = ActivityScenario.launch(testIntent);
    }

    @Test
    @Ignore("b/295325503")
    public void testFaceEnrollEducation_hasHeader() {
        setupActivity();

        mScenario.onActivity(activity -> {
            GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
            CharSequence headerText = glifLayout.getHeaderText();

            assertThat(headerText.toString()).isEqualTo(
                    activity.getString(R.string.security_settings_face_enroll_education_title));
        });
    }

    @Test
    public void testFaceEnrollEducation_hasDescription() {
        setupActivity();

        mScenario.onActivity(activity -> {
            GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
            CharSequence desc = glifLayout.getDescriptionText();

            assertThat(desc.toString()).isEqualTo(
                    activity.getString(R.string.security_settings_face_enroll_education_message));
        });
    }

    @Test
    public void testFaceEnrollEducation_showFooterPrimaryButton() {
        setupActivity();

        mScenario.onActivity(activity -> {
            GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
            FooterBarMixin footer = glifLayout.getMixin(FooterBarMixin.class);
            FooterButton footerButton = footer.getPrimaryButton();

            assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
            assertThat(footerButton.getText().toString()).isEqualTo(
                    activity.getString(R.string.security_settings_face_enroll_education_start));
        });
    }

    @Test
    public void testFaceEnrollEducation_showFooterSecondaryButton() {
        setupActivity();

        mScenario.onActivity(activity -> {
            GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
            FooterBarMixin footer = glifLayout.getMixin(FooterBarMixin.class);
            FooterButton footerButton = footer.getSecondaryButton();

            assertThat(footerButton.getVisibility()).isEqualTo(View.VISIBLE);
            assertThat(footerButton.getText().toString()).isEqualTo(activity.getString(
                    R.string.security_settings_face_enroll_introduction_cancel));
        });
    }

    @Test
    public void testFaceEnrollEducation_defaultNeverLaunchPostureGuidance() {
        setupActivity();

        mScenario.onActivity(activity -> {
            assertThat(activity.launchPostureGuidance()).isFalse();
            assertThat(activity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
        });
    }

    @Test
    public void testFaceEnrollEducation_onStartNeverRegisterPostureChangeCallback() {
        setupActivity();

        mScenario.onActivity(activity -> {
            assertThat(activity.getPostureGuidanceIntent()).isNull();
            assertThat(activity.getPostureCallback()).isNull();
            assertThat(activity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_UNKNOWN);
        });
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onStartRegisteredPostureChangeCallback() {
        setupActivityForPosture();

        mScenario.onActivity(activity -> {
            assertThat(activity.getPostureGuidanceIntent()).isNotNull();
            assertThat(activity.getPostureCallback()).isNotNull();
        });
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_unFolded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_XXXHIGH;
        setupActivityForPosture();

        mScenario.onActivity(activity -> {
            assertThat(activity.getPostureGuidanceIntent()).isNotNull();
            assertThat(activity.getPostureCallback()).isNotNull();

            activity.onConfigurationChanged(newConfig);

            assertThat(activity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_OPENED);
        });
    }

    @Test
    public void testFaceEnrollEducationWithPosture_onFoldedUpdated_folded() {
        final Configuration newConfig = new Configuration();
        newConfig.smallestScreenWidthDp = DENSITY_DEFAULT;
        setupActivityForPosture();

        mScenario.onActivity(activity -> {
            assertThat(activity.getPostureGuidanceIntent()).isNotNull();
            assertThat(activity.getPostureCallback()).isNotNull();

            activity.onConfigurationChanged(newConfig);

            assertThat(activity.getDevicePostureState()).isEqualTo(DEVICE_POSTURE_CLOSED);
        });
    }

    @Test
    public void testFaceEnrollEducation_LaunchActivityNormal() {
        setupActivity();

        mScenario.onActivity(activity -> {
            final View faceEnrollEducationView = activity.findViewById(
                    R.id.setup_wizard_layout);
            assertThat(faceEnrollEducationView).isNotNull();

            int a11yButtonId = activity.isUsingExpressiveStyle()
                    ? R.id.accessibility_button_expressive
                    : R.id.accessibility_button;
            final Button a11yButton = activity.findViewById(a11yButtonId);
            assertThat(a11yButton).isNotNull();
        });
    }

    @Test
    public void testFaceEnrollEducation_LottieA11y() {
        SettingsShadowResources.overrideResource(R.bool.config_face_education_use_lottie, true);

        setupActivity();

        mScenario.onActivity(activity -> {
            LottieAnimationView lottie = activity.findViewById(R.id.illustration_lottie);
            assertThat(lottie).isNotNull();
            assertThat(lottie.getVisibility()).isEqualTo(View.VISIBLE);

            // Correct contentDescription for a11y
            CharSequence contentDescription = lottie.getContentDescription();
            assertThat(contentDescription).isNotNull();
            assertThat(contentDescription.toString()).isEqualTo(
                    mContext.getString(settingslib_illustration_content_description));

            // Verify an OnClickListener is present. Direct testing of Lottie's
            // click-to-pause/resume behavior has limitations in Robolectric, so this checks for
            // interactive setup.
            assertThat(lottie.hasOnClickListeners()).isTrue();

            // Verify the AccessibilityDelegate provides correct information.
            // Lottie animation playback and full interaction are not deeply simulated in
            // Robolectric, so we inspect the AccessibilityNodeInfo as populated by the delegate.
            // Expected AccessibilityNodeInfo properties:
            // 1. className is null: Prevents TalkBack from announcing a generic type like "Image",
            //    allowing for more specific announcements via contentDescription or action labels.
            // 2. ACTION_CLICK label is "pause" because animation is playing before onResume().
            View.AccessibilityDelegate delegate = lottie.getAccessibilityDelegate();
            assertThat(delegate).isNotNull();
            AccessibilityNodeInfo nodeInfo = new AccessibilityNodeInfo(lottie);
            delegate.onInitializeAccessibilityNodeInfo(lottie, nodeInfo);
            assertThat(nodeInfo.getClassName()).isNull();
            CharSequence clickActionLabel = null;
            if (nodeInfo.getActionList() != null) {
                for (AccessibilityNodeInfo.AccessibilityAction action : nodeInfo.getActionList()) {
                    if (action.getId() == AccessibilityNodeInfo.ACTION_CLICK) {
                        clickActionLabel = action.getLabel();
                        break;
                    }
                }
            }
            assertThat(clickActionLabel).isNotNull();
            assertThat(clickActionLabel.toString()).isEqualTo(
                    mContext.getString(settingslib_action_label_pause));
        });
    }

    @Test
    public void testFaceEnrollEducation_IllustrationShouldNotTruncated() {
        setupActivity();

        mScenario.onActivity(activity -> {
            final GlifLayout glifLayout = activity.findViewById(R.id.setup_wizard_layout);
            final TextView descView =  glifLayout.getDescriptionTextView();
            final int descBottomPos = activity.getOnScreenPositionTop(descView)
                    + descView.getHeight();
            final LottieAnimationView illustrationLottie = activity.findViewById(
                    R.id.illustration_lottie);
            final int illustrationLottieTop = activity.getOnScreenPositionTop(illustrationLottie);
            if (illustrationLottieTop < descBottomPos) {
                assertThat(activity.adjustIllustrationLottiePosition()).isTrue();
            } else {
                assertThat(activity.adjustIllustrationLottiePosition()).isFalse();
            }
        });
    }
}
