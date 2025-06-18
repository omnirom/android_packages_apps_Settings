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
package com.android.settings.bluetooth

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.testing.EmptyFragmentActivity
import androidx.preference.Preference
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.spy
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BluetoothDetailsFragmentTest {
    @get:Rule val mockitoRule: MockitoRule = MockitoJUnit.rule()

    private lateinit var activity: FragmentActivity
    private lateinit var fragment: TestConfigurableFragment
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Context>())
    }

    @Test
    fun setPreferenceDisplayOrder_null_unchanged() = buildFragment {
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key1" })
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key2" })

        fragment.setPreferenceDisplayOrder(null)

        assertThat(this.displayedKeys).containsExactly("key1", "key2")
    }

    @Test
    fun setPreferenceDisplayOrder_hideItem() = buildFragment {
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key1" })
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key2" })

        fragment.setPreferenceDisplayOrder(mutableListOf("key2"))

        assertThat(this.displayedKeys).containsExactly("key2")
    }

    @Test
    fun setPreferenceDisplayOrder_hideAndReShownItem() = buildFragment {
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key1" })
        fragment.preferenceScreen.addPreference(Preference(context).apply { key = "key2" })

        fragment.setPreferenceDisplayOrder(mutableListOf("key2"))
        fragment.setPreferenceDisplayOrder(mutableListOf("key2", "key1"))

        assertThat(this.displayedKeys).containsExactly("key2", "key1")
    }

    private fun buildFragment(r: (() -> Unit)) {
        ActivityScenario.launch(EmptyFragmentActivity::class.java).use { activityScenario ->
            activityScenario.onActivity { activity: EmptyFragmentActivity ->
                this@BluetoothDetailsFragmentTest.activity = activity
                fragment = TestConfigurableFragment()
                activity.supportFragmentManager.beginTransaction().add(fragment, null).commitNow()
                fragment.setPreferenceScreen(
                    fragment.preferenceManager.createPreferenceScreen(context)
                )
                r.invoke()
            }
        }
    }

    private val displayedKeys: List<String>
        get() {
            val keys: MutableList<String> = mutableListOf()
            for (i in 0..<fragment.preferenceScreen.preferenceCount) {
                if (fragment.preferenceScreen.getPreference(i).isVisible) {
                    keys.add(fragment.preferenceScreen.getPreference(i).key)
                }
            }
            return keys
        }

    class TestConfigurableFragment : BluetoothDetailsConfigurableFragment() {
        protected override fun getPreferenceScreenResId(): Int {
            return 0
        }

        override fun getLogTag(): String {
            return "TAG"
        }

        override fun getMetricsCategory(): Int {
            return 0
        }
    }
}
