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

package com.android.settings.widget;

import android.annotation.AnyRes;
import android.content.Context;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.PreferenceXmlParserUtils;
import com.android.settings.core.PreferenceXmlParserUtils.MetadataFlag;
import com.android.settingslib.RestrictedSelectorWithWidgetPreference;
import com.android.settingslib.widget.CandidateInfo;
import com.android.settingslib.widget.IllustrationPreference;
import com.android.settingslib.widget.SelectorWithWidgetPreference;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A fragment to handle general radio button picker
 */
public abstract class RadioButtonPickerFragment extends SettingsPreferenceFragment implements
        SelectorWithWidgetPreference.OnClickListener {

    @VisibleForTesting
    static final String EXTRA_FOR_WORK = "for_work";

    private static final String TAG = "RadioButtonPckrFrgmt";
    private static final String KEY_CATEGORY_PREFERENCE = "radio_button_picker_category";
    @VisibleForTesting
    boolean mAppendStaticPreferences = false;

    private final Map<String, CandidateInfo> mCandidates = new ArrayMap<>();

    protected UserManager mUserManager;
    protected int mUserId;
    private int mIllustrationId;
    private int mIllustrationPreviewId;
    private IllustrationType mIllustrationType;
    private int mCategoryTitleId;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mUserManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        final Bundle arguments = getArguments();

        boolean mForWork = false;
        if (arguments != null) {
            mForWork = arguments.getBoolean(EXTRA_FOR_WORK);
        }
        final UserHandle managedProfile = Utils.getManagedProfile(mUserManager);
        mUserId = mForWork && managedProfile != null
                ? managedProfile.getIdentifier()
                : UserHandle.myUserId();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (isCatalystEnabled()) {
            PreferenceScreen preferenceScreen = createPreferenceScreen();
            setPreferenceScreen(preferenceScreen);
        } else {
            super.onCreatePreferences(savedInstanceState, rootKey);
        }
        try {
            // Check if the xml specifies if static preferences should go on the top or bottom
            final List<Bundle> metadata = PreferenceXmlParserUtils.extractMetadata(getContext(),
                getPreferenceScreenResId(),
                MetadataFlag.FLAG_INCLUDE_PREF_SCREEN |
                MetadataFlag.FLAG_NEED_PREF_APPEND);
            mAppendStaticPreferences = metadata.getFirst()
                    .getBoolean(PreferenceXmlParserUtils.METADATA_APPEND);
        } catch (IOException e) {
            Log.e(TAG, "Error trying to open xml file", e);
        } catch (XmlPullParserException e) {
            Log.e(TAG, "Error parsing xml", e);
        }
        updateCandidates();
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    protected abstract int getPreferenceScreenResId();

    @Override
    public void onRadioButtonClicked(SelectorWithWidgetPreference selected) {
        final String selectedKey = selected.getKey();
        onRadioButtonConfirmed(selectedKey);
    }

    /**
     * Called after the user tries to select an item.
     */
    protected void onSelectionPerformed(boolean success) {
    }

    /**
     * Whether the UI should show a "None" item selection.
     */
    protected boolean shouldShowItemNone() {
        return false;
    }

    /**
     * Populate any static preferences, independent of the radio buttons.
     * These might be used to provide extra information about the choices.
     **/
    protected void addStaticPreferences(PreferenceScreen screen) {
    }

    protected CandidateInfo getCandidate(String key) {
        return mCandidates.get(key);
    }

    protected void onRadioButtonConfirmed(String selectedKey) {
        final boolean success = setDefaultKey(selectedKey);
        if (success) {
            updateCheckedState(selectedKey);
        }
        onSelectionPerformed(success);
    }

    /**
     * A chance for subclasses to bind additional things to the preference.
     */
    public void bindPreferenceExtra(SelectorWithWidgetPreference pref,
            String key, CandidateInfo info, String defaultKey, String systemDefaultKey) {
    }

    /**
     * A chance for subclasses to create a custom preference instance.
     */
    protected SelectorWithWidgetPreference createPreference() {
        return new RestrictedSelectorWithWidgetPreference(getPrefContext());
    }

    public void updateCandidates() {
        mCandidates.clear();
        final List<? extends CandidateInfo> candidateList = getCandidates();
        if (candidateList != null) {
            for (CandidateInfo info : candidateList) {
                mCandidates.put(info.getKey(), info);
            }
        }
        final String defaultKey = getDefaultKey();
        final String systemDefaultKey = getSystemDefaultKey();
        final PreferenceScreen screen = getPreferenceScreen();
        screen.removeAll();
        if (mIllustrationId != 0) {
            addIllustration(screen);
        }
        if (!mAppendStaticPreferences) {
            addStaticPreferences(screen);
        }
        PreferenceGroup radioButtonsGroup = screen;
        if (mCategoryTitleId != 0) {
            Context context = getPrefContext();
            PreferenceCategory category = new PreferenceCategory(getPrefContext());
            category.setKey(KEY_CATEGORY_PREFERENCE);
            category.setEnabled(true);
            category.setTitle(context.getText(mCategoryTitleId));
            screen.addPreference(category);
            radioButtonsGroup = category;
        }
        final int customLayoutResId = getRadioButtonPreferenceCustomLayoutResId();
        if (shouldShowItemNone()) {
            final SelectorWithWidgetPreference nonePref =
                    new SelectorWithWidgetPreference(getPrefContext());
            if (customLayoutResId > 0) {
                nonePref.setLayoutResource(customLayoutResId);
            }
            nonePref.setIcon(R.drawable.ic_remove_circle);
            nonePref.setTitle(R.string.app_list_preference_none);
            nonePref.setChecked(TextUtils.isEmpty(defaultKey));
            nonePref.setOnClickListener(this);
            radioButtonsGroup.addPreference(nonePref);
        }
        if (candidateList != null) {
            for (CandidateInfo info : candidateList) {
                SelectorWithWidgetPreference pref = createPreference();
                if (customLayoutResId > 0) {
                    pref.setLayoutResource(customLayoutResId);
                }
                bindPreference(pref, info.getKey(), info, defaultKey);
                bindPreferenceExtra(pref, info.getKey(), info, defaultKey, systemDefaultKey);
                radioButtonsGroup.addPreference(pref);
            }
        }
        mayCheckOnlyRadioButton();
        if (mAppendStaticPreferences) {
            addStaticPreferences(screen);
        }
    }

    /** Updates [pref] with the candidate information provided. */
    public void bindPreference(
            @NonNull SelectorWithWidgetPreference pref,
            @NonNull String key,
            @NonNull CandidateInfo info,
            @NonNull String defaultKey) {
        pref.setTitle(info.loadLabel());
        pref.setIcon(Utils.getSafeIcon(info.loadIcon()));
        pref.setKey(key);
        if (TextUtils.equals(defaultKey, key)) {
            pref.setChecked(true);
        }
        pref.setEnabled(info.enabled);
        pref.setOnClickListener(this);
    }

    /** Requests that all radio button preferences update their checked state. */
    public void updateCheckedState(String selectedKey) {
        forEachRadioButtonPreference(radioPref -> {
            final boolean newCheckedState = TextUtils.equals(radioPref.getKey(), selectedKey);
            if (radioPref.isChecked() != newCheckedState) {
                radioPref.setChecked(newCheckedState);
            }
        });
    }

    public void mayCheckOnlyRadioButton() {
        // If there is only 1 thing on screen, select it.
        PreferenceGroup preferenceGroup = getRadioPreferenceGroup();
        if (preferenceGroup != null && preferenceGroup.getPreferenceCount() == 1) {
            if (preferenceGroup.getPreference(0) instanceof SelectorWithWidgetPreference onlyPref) {
                onlyPref.setChecked(true);
            }
        }
    }

    /**
     * Iterates through all [SelectorWithWidgetPreference] preferences in the preference hierarchy
     * and executes the given action on each.
     * <p>
     * Note that this method does not operate recursively. It assumes that the radio button
     * preferences are either inside of the [PreferenceCategory] if one is defined, or at the top of
     * the hierarchy (the [PreferenceScreen]) otherwise.
     *
     * @param action The function to execute on each SelectorWithWidgetPreference.
     */
    private void forEachRadioButtonPreference(Consumer<SelectorWithWidgetPreference> action) {
        if (action == null) {
            return;
        }
        final PreferenceGroup group = getRadioPreferenceGroup();
        if (group == null) {
            return;
        }
        final int count = group.getPreferenceCount();
        for (int i = 0; i < count; i++) {
            if (group.getPreference(i) instanceof SelectorWithWidgetPreference radioPref) {
                action.accept(radioPref);
            }
        }
    }

    /**
     * Returns the `PreferenceGroup` in which the radio buttons in this `Fragment` are contained, or
     * `null` if the top-level `PreferenceScreen` isn't defined yet.
     */
    @Nullable
    private PreferenceGroup getRadioPreferenceGroup() {
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen == null) {
            return null;
        }
        if (mCategoryTitleId != 0) {
            Preference categoryPref = screen.findPreference(KEY_CATEGORY_PREFERENCE);
            if (categoryPref instanceof PreferenceGroup categoryPrefGroup) {
                // All radio preferences are inside the category.
                return categoryPrefGroup;
            }
        }
        // All radio preferences are at the top-level.
        return screen;
    }

    /**
     * Allows you to set an illustration at the top of this screen. Set the illustration id to 0
     * if you want to remove the illustration.
     *
     * @param illustrationId   The res id for the raw of the illustration.
     * @param previewId        The res id for the drawable of the illustration.
     * @param illustrationType The illustration type for the raw of the illustration.
     */
    protected void setIllustration(@AnyRes int illustrationId, @AnyRes int previewId,
            IllustrationType illustrationType) {
        mIllustrationId = illustrationId;
        mIllustrationPreviewId = previewId;
        mIllustrationType = illustrationType;
    }

    /**
     * Allows you to set an illustration at the top of this screen. Set the illustration id to 0
     * if you want to remove the illustration.
     *
     * @param illustrationId   The res id for the raw of the illustration.
     * @param illustrationType The illustration type for the raw of the illustration.
     */
    protected void setIllustration(@AnyRes int illustrationId, IllustrationType illustrationType) {
        setIllustration(illustrationId, 0, illustrationType);
    }

    private void addIllustration(PreferenceScreen screen) {
        switch (mIllustrationType) {
            case LOTTIE_ANIMATION:
                IllustrationPreference illustrationPreference = new IllustrationPreference(
                        getContext());
                illustrationPreference.setLottieAnimationResId(mIllustrationId);
                screen.addPreference(illustrationPreference);
                break;
            default:
                throw new IllegalArgumentException(
                        "Invalid illustration type: " + mIllustrationType);
        }
    }

    /**
     * Allows you to wrap the radio button preference in a category. Set the title id to 0
     * to remove the category.
     *
     * @param title The string resource ID for the title of the category.
     */
    protected void setCategory(@StringRes int title) {
        mCategoryTitleId = title;
    }

    protected abstract List<? extends CandidateInfo> getCandidates();

    protected abstract String getDefaultKey();

    protected abstract boolean setDefaultKey(String key);

    protected String getSystemDefaultKey() {
        return null;
    }

    /**
     * Provides a custom layout for each candidate row.
     */
    @LayoutRes
    protected int getRadioButtonPreferenceCustomLayoutResId() {
        return 0;
    }

    protected enum IllustrationType {
        LOTTIE_ANIMATION
    }

}
