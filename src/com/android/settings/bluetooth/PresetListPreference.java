/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;

import com.android.settings.CustomListPreference;
import com.android.settings.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A preference for hearing device presets that refreshes the selected item in the dialog
 * when its value is changed.
 *
 * <p>This class extends {@link CustomListPreference} to provide a custom dialog with
 * a selectable list of presets. It manages a custom adapter, ensuring the UI stays consistent
 * with the underlying data.
 */
public class PresetListPreference extends CustomListPreference {

    @Nullable
    private PresetArrayAdapter mAdapter;
    private DialogInterface.OnClickListener mWrapperListener;
    private boolean mIsItemClickedInDialog = false;

    public PresetListPreference(@NonNull Context context) {
        super(context, null);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, getEntries(), getEntryValues(), getValue());
    }

    @Override
    protected void onRestoreInstanceState(@Nullable Parcelable state) {
        if (state instanceof SavedState myState) {
            super.onRestoreInstanceState(myState.getSuperState());
            setEntries(myState.mEntryNames);
            setEntryValues(myState.mEntryValues);
            setValue(myState.mValue);
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    public void setEntries(CharSequence[] entries) {
        super.setEntries(entries);
        if (mAdapter != null) {
            mAdapter.updateList(Arrays.stream(entries).toList());
        }
    }

    @Override
    public void setValue(@Nullable String value) {
        super.setValue(value);
        if (mAdapter != null) {
            mAdapter.setSelectedIndex(getSelectedValueIndex());
        }
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder,
            DialogInterface.OnClickListener listener) {
        mAdapter = new PresetArrayAdapter(builder.getContext(), getEntries(),
                getSelectedValueIndex());
        mWrapperListener = (dialog, which) -> {
            mIsItemClickedInDialog = true;
            listener.onClick(dialog, which);
        };
        mIsItemClickedInDialog = false;
        builder.setAdapter(mAdapter, mWrapperListener);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        if (!mIsItemClickedInDialog) {
            // The dialog was dismissed without user interaction and the entries maybe updated when
            // the dialog is shown. Make sure to update the selected index manually to ensure the
            // CustomListPreferenceDialogFragment.mClickedDialogEntryIndex is synced with the remote
            // active preset value and avoid potential index out of bound exception.
            mWrapperListener.onClick(null, getSelectedValueIndex());
        }
    }

    @VisibleForTesting
    void setAdapter(PresetArrayAdapter adapter) {
        mAdapter = adapter;
    }

    private int getSelectedValueIndex() {
        final String selectedValue = getValue();
        return (selectedValue == null) ? -1 : findIndexOfValue(selectedValue);
    }

    public static class PresetArrayAdapter extends ArrayAdapter<CharSequence> {
        private int mSelectedIndex;

        public PresetArrayAdapter(@NonNull Context context, @NonNull CharSequence[] objects,
                int selectedIndex) {
            // The constructor with an array argument creates a fixed-size list which is immutable.
            // To allow for future modifications like adding or clearing, the array is first
            // converted to a mutable ArrayList.
            super(context, R.layout.preset_dialog_singlechoice, R.id.text1,
                    new ArrayList<>(Arrays.asList(objects)));
            mSelectedIndex = selectedIndex;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            View root = super.getView(position, convertView, parent);
            CheckedTextView text = root.findViewById(R.id.text1);
            if (text != null && mSelectedIndex != -1) {
                text.setChecked(position == mSelectedIndex);
            }
            return root;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        /**
         * Updates the selected index.
         *
         * @param index The new selected index.
         */
        public void setSelectedIndex(int index) {
            mSelectedIndex = index;
            notifyDataSetChanged();
        }

        /**
         * Updates the list of entries and refreshes the adapter.
         *
         * @param list The new list of entries.
         */
        public void updateList(List<CharSequence> list) {
            clear();
            addAll(list);
            notifyDataSetChanged();
        }
    }

    private static class SavedState extends BaseSavedState {
        CharSequence[] mEntryNames;
        CharSequence[] mEntryValues;
        String mValue;

        SavedState(Parcelable superState, CharSequence[] entryNames, CharSequence[] entryValues,
                String value) {
            super(superState);
            this.mEntryNames = entryNames;
            this.mEntryValues = entryValues;
            this.mValue = value;
        }

        private SavedState(Parcel in) {
            super(in);
            this.mEntryNames = in.readCharSequenceArray();
            this.mEntryValues = in.readCharSequenceArray();
            this.mValue = in.readString();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeCharSequenceArray(this.mEntryNames);
            out.writeCharSequenceArray(this.mEntryValues);
            out.writeString(this.mValue);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
