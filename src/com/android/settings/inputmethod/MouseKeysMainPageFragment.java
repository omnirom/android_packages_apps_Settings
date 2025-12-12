/*
 * Copyright 2024 The Android Open Source Project
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

import static com.android.internal.accessibility.AccessibilityShortcutController.MOUSE_KEYS_COMPONENT_NAME;
import static com.android.settings.inputmethod.PhysicalKeyboardFragment.getHardKeyboards;

import android.app.settings.SettingsEnums;
import android.content.ComponentName;
import android.content.Context;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.input.InputManager;
import android.hardware.input.InputSettings;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.InputDevice;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceScreen;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.internal.util.Preconditions;
import com.android.settings.R;
import com.android.settings.accessibility.BaseSupportFragment;
import com.android.settings.accessibility.ToggleShortcutPreferenceController;
import com.android.settings.activityembedding.ActivityEmbeddingUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.utils.ThreadUtils;
import com.android.settingslib.widget.LayoutPreference;

import java.util.List;

@SearchIndexable
public class MouseKeysMainPageFragment extends BaseSupportFragment
        implements InputManager.InputDeviceListener {

    private static final String TAG = "MouseKeysMainPageFragment";
    private static final String KEY_MOUSE_KEY_LIST = "mouse_keys_list";

    private InputManager mInputManager;
    private LayoutPreference mMouseKeyImagesPreference;
    @Nullable
    private InputDevice mCurrentInputDevice;

    static final Uri ACCESSIBILITY_MOUSE_KEYS_USE_PRIMARY_KEYS_URI =
            Settings.Secure.getUriFor(Settings.Secure.ACCESSIBILITY_MOUSE_KEYS_USE_PRIMARY_KEYS);

    final ContentObserver mSettingsObserver =
            new ContentObserver(new Handler(Looper.getMainLooper())) {
                @Override
                public void onChange(boolean selfChange, @Nullable Uri uri) {
                    if (mMouseKeyImagesPreference == null || uri == null) {
                        return;
                    }
                    onUsePrimaryKeysUpdated();
                }
            };

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        ToggleShortcutPreferenceController shortcutController =
                use(KeyboardAccessibilityMouseKeysShortcutController.class);
        if (shortcutController != null) {
            shortcutController.initialize(
                    getFeatureComponentName(),
                    getChildFragmentManager(),
                    getFeatureName(),
                    getMetricsCategory()
            );
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCurrentInputDevice = getInputDevice();
        final PreferenceScreen screen = getPreferenceScreen();
        mMouseKeyImagesPreference = screen.findPreference(KEY_MOUSE_KEY_LIST);
        mInputManager = Preconditions.checkNotNull(getActivity()
                .getSystemService(InputManager.class));
        String title = mCurrentInputDevice == null || getHardKeyboards(getContext()).size() == 1
                ? getActivity().getString(R.string.mouse_keys)
                : getActivity().getString(R.string.mouse_key_main_page_title,
                        mCurrentInputDevice.getName());
        getActivity().setTitle(title);
        configureImagesPreference();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (com.android.server.accessibility.Flags.enableMouseKeyEnhancement()) {
            getContentResolver().registerContentObserver(
                    ACCESSIBILITY_MOUSE_KEYS_USE_PRIMARY_KEYS_URI,
                    /* notifyForDescendants= */ false,
                    mSettingsObserver);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (com.android.server.accessibility.Flags.enableMouseKeyEnhancement()) {
            getContentResolver().unregisterContentObserver(mSettingsObserver);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        finishEarlyIfNeeded();
        mInputManager.registerInputDeviceListener(this, null);
    }

    @Override
    public void onPause() {
        super.onPause();
        mInputManager.unregisterInputDeviceListener(this);
    }

    @NonNull
    private CharSequence getFeatureName() {
        return getContext().getString(R.string.mouse_keys);
    }

    @NonNull
    private ComponentName getFeatureComponentName() {
        return MOUSE_KEYS_COMPONENT_NAME;
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_PHYSICAL_KEYBOARD_MOUSE_KEYS;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.mouse_keys_main_page;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public void onInputDeviceAdded(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceRemoved(int deviceId) {
        finishEarlyIfNeeded();
    }

    @Override
    public void onInputDeviceChanged(int deviceId) {
        finishEarlyIfNeeded();
    }

    private void finishEarlyIfNeeded() {
        final Context context = getContext();
        ThreadUtils.postOnBackgroundThread(() -> {
            final List<PhysicalKeyboardFragment.HardKeyboardDeviceInfo> newHardKeyboards =
                    getHardKeyboards(context);
            if (newHardKeyboards.isEmpty()) {
                getActivity().finish();
            }
        });
    }

    private void onUsePrimaryKeysUpdated() {
        updatePrimaryKeysImagesVisibility();
        use(MouseKeysPrimaryKeysController.class).setChecked(
                InputSettings.isPrimaryKeysForMouseKeysEnabled(getContext()));
    }

    private void updatePrimaryKeysImagesVisibility() {
        boolean usePrimaryKeys = InputSettings.isPrimaryKeysForMouseKeysEnabled(getContext());
        mMouseKeyImagesPreference.findViewById(R.id.mouse_keys_image_recycler_list)
                .setVisibility(usePrimaryKeys ? View.VISIBLE : View.GONE);
        mMouseKeyImagesPreference
                .findViewById(R.id.title_mouse_keys_image_recycler_list)
                .setVisibility(usePrimaryKeys ? View.VISIBLE : View.GONE);
    }

    private void configureImagesPreference() {
        final RecyclerView recyclerView = mMouseKeyImagesPreference.findViewById(
                R.id.mouse_keys_image_recycler_list);
        boolean isPortrait = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
        boolean isTwoPaneState = ActivityEmbeddingUtils.isAlreadyEmbedded(this.getActivity());
        int column = isPortrait && !isTwoPaneState ? 1 : 2;
        recyclerView.setLayoutManager(new GridLayoutManager(getActivity(), column));
        recyclerView.setAdapter(new MouseKeysImageListAdapter(getActivity(), mCurrentInputDevice));

        if (com.android.server.accessibility.Flags.enableMouseKeyEnhancement()) {
            // Update paddings to match the latest UI.
            recyclerView.setPadding(0, 0, 0, 0);
            final RecyclerView numKeyboardRecyclerView = mMouseKeyImagesPreference
                    .findViewById(R.id.mouse_keys_numpad_image_recycler_list);
            numKeyboardRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), column));
            numKeyboardRecyclerView.setAdapter(
                    new MouseKeysNumKeyboardImageListAdapter(getActivity(), mCurrentInputDevice));
            final View infoIcon = mMouseKeyImagesPreference.findViewById(
                    R.id.mouse_keys_info_icon);
            infoIcon.setVisibility(View.VISIBLE);

            mMouseKeyImagesPreference.findViewById(R.id.title_mouse_keys_image_recycler_list)
                    .setVisibility(View.VISIBLE);
            mMouseKeyImagesPreference
                    .findViewById(R.id.title_mouse_keys_numpad_image_recycler_list)
                    .setVisibility(View.VISIBLE);
            mMouseKeyImagesPreference
                    .findViewById(R.id.summary_mouse_keys_numpad_image_recycler_list)
                    .setVisibility(View.VISIBLE);
            updatePrimaryKeysImagesVisibility();
        }
    }

    /**
     * Priority of picking input device:
     * 1. internal keyboard(built-in keyboard)
     * 2. first keyboard in the list
     */
    @Nullable
    private InputDevice getInputDevice() {
        InputDevice inputDevice = null;
        for (int deviceId : InputDevice.getDeviceIds()) {
            final InputDevice device = InputDevice.getDevice(deviceId);
            if (device == null || device.isVirtual() || !device.isFullKeyboard()) {
                continue;
            }
            if (inputDevice == null) {
                inputDevice = device;
            } else if (!device.isExternal()) {
                inputDevice = device;
                break;
            }
        }
        return inputDevice;
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.mouse_keys_main_page) {
                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    return !getHardKeyboards(context).isEmpty();
                }
            };
}
