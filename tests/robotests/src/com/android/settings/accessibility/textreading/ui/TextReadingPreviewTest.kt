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

package com.android.settings.accessibility.textreading.ui

import android.content.Context
import android.view.Display
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreviewPreference
import com.android.settings.accessibility.textreading.data.DisplaySize
import com.android.settings.accessibility.textreading.data.FontSize
import com.android.settings.display.PreviewPagerAdapter
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.preference.createAndBindWidget
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/** Test for [TextReadingPreview]. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TextReadingPreviewTest {
    private lateinit var context: Context
    private val displaySizeDataFlow =
        MutableStateFlow(
            DisplaySize(currentIndex = 1, values = intArrayOf(356, 420, 490), defaultValue = 420)
        )

    private val fontSizeDataFlow =
        MutableStateFlow(
            FontSize(
                currentIndex = 1,
                values = floatArrayOf(0.85f, 1.0f, 1.15f, 1.30f, 1.50f, 1.80f, 2.0f),
                defaultValue = 1f,
            )
        )

    private val previewMetadata =
        TextReadingPreview(
            displaySizeProvider = { displaySizeDataFlow },
            fontSizeProvider = { fontSizeDataFlow },
        )
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Before
    fun setUp() {
        context = spy(ApplicationProvider.getApplicationContext<Context>())
        doReturn(Display.DEFAULT_DISPLAY).whenever(context).getDisplayId()
    }

    @After
    fun cleanUp() {
        testScope.cancel()
    }

    @Test
    fun getKey() {
        assertThat(previewMetadata.key).isEqualTo(TextReadingPreview.KEY)
    }

    @Test
    fun createWidget_returnTextReadingPreviewPreference() {
        val widget = previewMetadata.createWidget(context)
        assertThat(widget).isInstanceOf(TextReadingPreviewPreference::class.java)
        assertThat(widget.isSelectable).isFalse()
    }

    @Test
    fun onCreate_setupPreviewPreference() =
        testScope.runTest {
            val previewPreference =
                spy(previewMetadata.createAndBindWidget<TextReadingPreviewPreference>(context))
            val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                on { lifecycleScope }.thenReturn(testScope.backgroundScope)
                on { findPreference<TextReadingPreviewPreference>(TextReadingPreview.KEY) }
                    .thenReturn(previewPreference)
            }
            val previews =
                context.resources.obtainTypedArray(R.array.config_text_reading_preview_samples)
            val previewCount = previews.length()
            val isLayoutRtl =
                context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val expectedConfigIndex =
                fontSizeDataFlow.value.currentIndex * displaySizeDataFlow.value.values.size +
                    displaySizeDataFlow.value.currentIndex

            previewMetadata.onCreate(preferenceLifecycleContext)
            runCurrent()

            val previewPagerAdapter =
                argumentCaptor<PreviewPagerAdapter> {
                        verify(previewPreference).setPreviewAdapter(capture())
                    }
                    .firstValue
            assertThat(previewPagerAdapter.count).isEqualTo(previewCount)
            verify(previewPreference).setCurrentItem(if (isLayoutRtl) previewCount - 1 else 0)
            verify(previewPreference).setLastLayerIndex(expectedConfigIndex)
        }

    @Test
    fun onCreate_onNonDefaultDisplay_preferenceNotAvailable() =
        testScope.runTest {
            doReturn(2).whenever(context).getDisplayId()
            val previewPreference =
                spy(previewMetadata.createAndBindWidget<TextReadingPreviewPreference>(context))
            val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                on { lifecycleScope }.thenReturn(testScope.backgroundScope)
                on { findPreference<TextReadingPreviewPreference>(TextReadingPreview.KEY) }
                    .thenReturn(previewPreference)
            }
            previewMetadata.onCreate(preferenceLifecycleContext)
            runCurrent()

            assertThat(previewMetadata.isAvailable(context)).isEqualTo(false)
        }

    @Test
    fun fontSizeDisplaySizeChanged_updatePreviewPagerIndex() =
        testScope.runTest {
            val previewPreference =
                spy(previewMetadata.createAndBindWidget<TextReadingPreviewPreference>(context))
            val preferenceLifecycleContext: PreferenceLifecycleContext = mock {
                on { lifecycleScope }.thenReturn(testScope.backgroundScope)
                on { findPreference<TextReadingPreviewPreference>(TextReadingPreview.KEY) }
                    .thenReturn(previewPreference)
            }
            val displaySizeCounts = displaySizeDataFlow.value.values.size
            val newFontSizeIndex = 3
            val newDisplaySizeIndex = 2
            val expectedConfigIndex = newFontSizeIndex * displaySizeCounts + newDisplaySizeIndex

            previewMetadata.onCreate(preferenceLifecycleContext)
            runCurrent()
            fontSizeDataFlow.value = fontSizeDataFlow.value.copy(currentIndex = newFontSizeIndex)
            displaySizeDataFlow.value =
                displaySizeDataFlow.value.copy(currentIndex = newDisplaySizeIndex)
            runCurrent()

            verify(previewPreference).notifyPreviewPagerChanged(expectedConfigIndex)
        }
}
