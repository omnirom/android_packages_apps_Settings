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

package com.android.settings.testutils;

import android.content.SharedPreferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fake {@link SharedPreferences} to use within tests.
 *
 * <p>This will act in the same way as a real one for a particular file, but will store all the
 * mData in memory in the instance.
 *
 * <p>{@link SharedPreferences.Editor#apply()} and {@link SharedPreferences.Editor#commit} both act
 * in the same way, synchronously modifying the stored mData. Listeners are dispatched in the same
 * thread, also synchronously.
 */
public class FakeSharedPreferences implements SharedPreferences {
    private final Map<String, Object> mData = new HashMap<>();
    private final Set<OnSharedPreferenceChangeListener> mListeners = new HashSet<>();

    @Override
    public Map<String, ?> getAll() {
        return new HashMap<>(mData);
    }

    @Override
    public String getString(String key, String defValue) {
        Object value = mData.get(key);
        return (value instanceof String) ? (String) value : defValue;
    }

    @Override
    public Set<String> getStringSet(String key, Set<String> defValues) {
        Object value = mData.get(key);
        if (value instanceof Set<?>) {
            Set<?> setValue = (Set<?>) value;
            if (setValue.stream().allMatch(String.class::isInstance)) {
                return new HashSet<>((Set<String>) setValue);
            }
        }
        return (defValues != null) ? new HashSet<>(defValues) : null;
    }

    @Override
    public int getInt(String key, int defValue) {
        Object value = mData.get(key);
        return (value instanceof Integer) ? (Integer) value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        Object value = mData.get(key);
        return (value instanceof Long) ? (Long) value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        Object value = mData.get(key);
        return (value instanceof Float) ? (Float) value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        Object value = mData.get(key);
        return (value instanceof Boolean) ? (Boolean) value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return mData.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new Editor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener
            listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener
            listener) {
        mListeners.remove(listener);
    }

    public class Editor implements SharedPreferences.Editor {
        private boolean mClear = false;
        private final Map<String, Object> mChanges = new HashMap<>();
        private final Set<String> mRemovals = new HashSet<>();

        @Override
        public SharedPreferences.Editor putString(String key, String value) {
            if (value != null) {
                mChanges.put(key, value);
            } else {
                mRemovals.add(key);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putStringSet(String key, Set<String> values) {
            if (values != null) {
                mChanges.put(key, new HashSet<>(values));
            } else {
                mRemovals.add(key);
            }
            return this;
        }

        @Override
        public SharedPreferences.Editor putInt(String key, int value) {
            mChanges.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putLong(String key, long value) {
            mChanges.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putFloat(String key, float value) {
            mChanges.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor putBoolean(String key, boolean value) {
            mChanges.put(key, value);
            return this;
        }

        @Override
        public SharedPreferences.Editor remove(String key) {
            mRemovals.add(key);
            return this;
        }

        @Override
        public SharedPreferences.Editor clear() {
            mClear = true;
            return this;
        }

        @Override
        public boolean commit() {
            if (mClear) {
                mData.clear();
            }
            for (String key : mRemovals) {
                mData.remove(key);
            }
            mData.putAll(mChanges);

            Set<String> changedKeys = new HashSet<>(mRemovals);
            changedKeys.addAll(mChanges.keySet());

            if (mClear || !mRemovals.isEmpty() || !mChanges.isEmpty()) {
                for (OnSharedPreferenceChangeListener listener : mListeners) {
                    if (mClear) {
                        listener.onSharedPreferenceChanged(FakeSharedPreferences.this, null);
                    }
                    for (String key : changedKeys) {
                        listener.onSharedPreferenceChanged(FakeSharedPreferences.this, key);
                    }
                }
            }

            return true;
        }

        @Override
        public void apply() {
            commit();
        }
    }
}
