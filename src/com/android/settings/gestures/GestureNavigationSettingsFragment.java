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

package com.android.settings.gestures;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import com.android.settings.R;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.widget.SeekBarPreference;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.widget.ButtonPreference;
import com.android.settingslib.widget.SliderPreference;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * A fragment to include all the settings related to Gesture Navigation mode.
 */
@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class GestureNavigationSettingsFragment extends DashboardFragment {

    public static final String TAG = "GestureNavigationSettingsFragment";

    public static final String GESTURE_NAVIGATION_SETTINGS =
            "com.android.settings.GESTURE_NAVIGATION_SETTINGS";
    static final String ACTION_GESTURE_SANDBOX = "com.android.quickstep.action.GESTURE_SANDBOX";

    private static final String LEFT_EDGE_SEEKBAR_KEY = "gesture_left_back_sensitivity";
    private static final String RIGHT_EDGE_SEEKBAR_KEY = "gesture_right_back_sensitivity";
    private static final String GESTURE_TUTORIAL_KEY = "assistant_gesture_navigation_tutorial";
    final Intent mLaunchTutorialIntent =  new Intent(ACTION_GESTURE_SANDBOX)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("use_tutorial_menu", true);

    private WindowManager mWindowManager;
    private BackGestureIndicatorView mIndicatorView;

    private float[] mBackGestureInsetScales;
    private float mDefaultBackGestureInset;

    public GestureNavigationSettingsFragment() {
        super();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mIndicatorView = new BackGestureIndicatorView(getActivity());
        mWindowManager = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        super.onCreatePreferences(savedInstanceState, rootKey);

        final Resources res = getActivity().getResources();
        mDefaultBackGestureInset = res.getDimensionPixelSize(
                com.android.internal.R.dimen.config_backGestureInset);
        mBackGestureInsetScales = getFloatArray(res.obtainTypedArray(
                com.android.internal.R.array.config_backGestureInsetScales));

        initSliderPreference(LEFT_EDGE_SEEKBAR_KEY);
        initSliderPreference(RIGHT_EDGE_SEEKBAR_KEY);
        initTutorialButton();
    }

    @Override
    public void onResume() {
        super.onResume();

        mWindowManager.addView(mIndicatorView, mIndicatorView.getLayoutParams(
                getActivity().getWindow().getAttributes()));
    }

    @Override
    public void onPause() {
        super.onPause();

        mWindowManager.removeView(mIndicatorView);
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.gesture_navigation_settings;
    }

    @Override
    public int getHelpResource() {
        // TODO(b/146001201): Replace with gesture navigation help page when ready.
        return R.string.help_uri_default;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_GESTURE_NAV_BACK_SENSITIVITY_DLG;
    }

    private void initTutorialButton() {
        final ButtonPreference pref = getPreferenceScreen().findPreference(GESTURE_TUTORIAL_KEY);
        if (pref == null) {
            return;
        }
        if (!isGestureTutorialAvailable()) {
            pref.setVisible(false);
            return;
        }
        pref.setOnClickListener(preference -> {
            startActivity(mLaunchTutorialIntent);
        });
    }

    private boolean isGestureTutorialAvailable() {
        Context context = getContext();
        return context != null
                && mLaunchTutorialIntent.resolveActivity(context.getPackageManager()) != null;
    }

    private void initSliderPreference(final String key) {
        final SliderPreference pref = getPreferenceScreen().findPreference(key);
        pref.setUpdatesContinuously(true);
        pref.setHapticFeedbackMode(SeekBarPreference.HAPTIC_FEEDBACK_MODE_ON_TICKS);
        pref.setSliderIncrement(1);

        final String settingsKey = key == LEFT_EDGE_SEEKBAR_KEY
                ? Settings.Secure.BACK_GESTURE_INSET_SCALE_LEFT
                : Settings.Secure.BACK_GESTURE_INSET_SCALE_RIGHT;
        final float initScale = Settings.Secure.getFloat(
                getContext().getContentResolver(), settingsKey, 1.0f);

        // Find the closest value to initScale
        float minDistance = Float.MAX_VALUE;
        int minDistanceIndex = -1;
        for (int i = 0; i < mBackGestureInsetScales.length; i++) {
            float d = Math.abs(mBackGestureInsetScales[i] - initScale);
            if (d < minDistance) {
                minDistance = d;
                minDistanceIndex = i;
            }
        }
        pref.setValue(minDistanceIndex);
        pref.setSliderStateDescription(formatStateDescription(pref, minDistanceIndex));
        pref.setOnPreferenceChangeListener((p, v) -> {
            final int width = (int) (mDefaultBackGestureInset * mBackGestureInsetScales[(int) v]);
            mIndicatorView.setIndicatorWidth(width, key == LEFT_EDGE_SEEKBAR_KEY);
            final float scale = mBackGestureInsetScales[(int) v];
            Settings.Secure.putFloat(getContext().getContentResolver(), settingsKey, scale);
            pref.setSliderStateDescription(formatStateDescription(pref, (int) v));
            return true;
        });
    }

    private CharSequence formatStateDescription(SliderPreference pref, int progress) {
        Locale curLocale = getContext().getResources().getConfiguration().getLocales().get(0);
        NumberFormat numberFormat = NumberFormat.getPercentInstance(curLocale);
        return numberFormat.format(getPercent(pref.getMin(), pref.getMax(), progress));
    }

    private double getPercent(int min, int max, int progress) {
        final float diffProgress = max - min;
        if (diffProgress <= 0.0f) {
            return 0.0f;
        }
        final float percent = (progress - min) / diffProgress;
        return Math.floor(Math.max(0.0f, Math.min(1.0f, percent)) * 100) / 100;
    }

    private static float[] getFloatArray(TypedArray array) {
        int length = array.length();
        float[] floatArray = new float[length];
        for (int i = 0; i < length; i++) {
            floatArray[i] = array.getFloat(i, 1.0f);
        }
        array.recycle();
        return floatArray;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.gesture_navigation_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return SystemNavigationPreferenceController.isGestureAvailable(context);
                }
            };

}
