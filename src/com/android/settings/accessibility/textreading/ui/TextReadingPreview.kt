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
import android.content.res.Configuration
import android.view.Display
import android.view.View
import com.android.settings.R
import com.android.settings.accessibility.TextReadingPreviewPreference
import com.android.settings.accessibility.textreading.data.DisplaySize
import com.android.settings.accessibility.textreading.data.FontSize
import com.android.settings.display.PreviewPagerAdapter
import com.android.settingslib.metadata.PreferenceAvailabilityProvider
import com.android.settingslib.metadata.PreferenceLifecycleContext
import com.android.settingslib.metadata.PreferenceLifecycleProvider
import com.android.settingslib.metadata.PreferenceMetadata
import com.android.settingslib.preference.PreferenceBinding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

internal class TextReadingPreview(
    displaySizeProvider: () -> Flow<DisplaySize>,
    fontSizeProvider: () -> Flow<FontSize>,
) :
    PreferenceMetadata,
    PreferenceBinding,
    PreferenceLifecycleProvider,
    PreferenceAvailabilityProvider {
    override val key: String
        get() = KEY

    private var previewPagerAdapter: PreviewPagerAdapter? = null
    private val displaySize: Flow<DisplaySize> by lazy { displaySizeProvider.invoke() }
    private val fontSize: Flow<FontSize> by lazy { fontSizeProvider.invoke() }

    override fun createWidget(context: Context) =
        TextReadingPreviewPreference(context, /* attrs= */ null).apply { isSelectable = false }

    override fun onCreate(context: PreferenceLifecycleContext) {
        super.onCreate(context)
        val previewPreference = context.findPreference<TextReadingPreviewPreference>(KEY)
        if (previewPreference != null) {
            context.lifecycleScope.launch {
                combine(displaySize, fontSize) { displaySizeData, fontSizeData ->
                        updatePreviewPagerConfigIndex(
                            previewPreference,
                            fontSizeData,
                            displaySizeData,
                        )
                    }
                    .collect()
            }
        }
    }

    private fun updatePreviewPagerConfigIndex(
        previewPreference: TextReadingPreviewPreference,
        fontSize: FontSize,
        displaySize: DisplaySize,
    ) {
        // LINT.IfChange(calculate_config_index)
        val updatedPagerIndex =
            fontSize.currentIndex * displaySize.values.size + displaySize.currentIndex
        // LINT.ThenChange(:configuration_order)

        if (previewPagerAdapter == null) {
            previewPagerAdapter =
                createPreviewPagerAdapter(
                        previewPreference.context,
                        fontSize.values,
                        displaySize.values,
                    )
                    .apply {
                        setPreviewLayer(
                            /* newLayerIndex= */ updatedPagerIndex,
                            /* currentLayerIndex= */ 0,
                            /* currentFrameIndex= */ 0,
                            /* animate= */ false,
                        )
                    }
            val isLayoutRtl =
                previewPreference.context.resources.configuration.layoutDirection ==
                    View.LAYOUT_DIRECTION_RTL
            previewPreference.setPreviewAdapter(previewPagerAdapter)
            previewPreference.setCurrentItem(
                if (isLayoutRtl) previewPagerAdapter!!.count - 1 else 0
            )
            previewPreference.setLastLayerIndex(updatedPagerIndex)
        } else {
            previewPreference.notifyPreviewPagerChanged(updatedPagerIndex)
        }
    }

    private fun createPreviewPagerAdapter(
        context: Context,
        fontSizes: FloatArray,
        displaySizes: IntArray,
    ): PreviewPagerAdapter {
        val isLayoutRtl =
            context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        return PreviewPagerAdapter(
            context,
            isLayoutRtl,
            getPreviewSamples(context),
            createConfig(context.resources.configuration, fontSizes, displaySizes),
        )
    }

    /** Returns an array of resource IDs for preview layouts. */
    private fun getPreviewSamples(context: Context): IntArray {
        val previews =
            context.resources.obtainTypedArray(R.array.config_text_reading_preview_samples)
        val previewCount = previews.length()
        val previewSamples =
            IntArray(previewCount) { index ->
                previews.getResourceId(index, R.layout.screen_zoom_preview_1)
            }
        previews.recycle()
        return previewSamples
    }

    /**
     * Creates an array of [Configuration] objects based on an original configuration, a set of font
     * sizes, and a set of display sizes.
     *
     * Each generated [Configuration] in the array represents a unique combination of a font size
     * and a display size. The font scale and density DPI of the original configuration are
     * overridden with values from the input arrays.
     *
     * The total number of configurations generated will be `fontSizes.size * displaySizes.size`.
     * The configurations are ordered such that all display size variations for the first font size
     * are listed, then all display size variations for the second font size, and so on.
     *
     * For example, if `fontSizes = [1.0f, 1.5f]` and `displaySizes = [240, 320]`, the generated
     * configurations would apply:
     * 1. fontScale = 1.0f, densityDpi = 240
     * 2. fontScale = 1.0f, densityDpi = 320
     * 3. fontScale = 1.5f, densityDpi = 240
     * 4. fontScale = 1.5f, densityDpi = 320
     *
     * @param origConfig The base [Configuration] to be copied for each new variant. Its `fontScale`
     *   and `densityDpi` will be replaced.
     * @param fontSizes An array of font scale multipliers.
     * @param displaySizes An array of display density DPI values.
     * @return An array of [Configuration] objects, each representing a unique combination of a font
     *   size and display size.
     */
    // LINT.IfChange(configuration_order)
    private fun createConfig(
        origConfig: Configuration,
        fontSizes: FloatArray,
        displaySizes: IntArray,
    ): Array<Configuration> {
        val totalConfigurationSize = fontSizes.size * displaySizes.size
        return Array(totalConfigurationSize) { index ->
            val config = Configuration(origConfig)
            config.fontScale = fontSizes[index / displaySizes.size]
            config.densityDpi = displaySizes[index % displaySizes.size]
            config
        }
    }

    override fun isAvailable(context: Context): Boolean {
        // TODO(b/428700479): Preview preference is hidden in non default displays as preview is
        //  created via default display density configuration which results in unrealistic
        //  preview in other displays.
        return context.getDisplayId() == Display.DEFAULT_DISPLAY
    }

    // LINT.ThenChange(:calculate_config_index)

    companion object {
        const val KEY = "preview"
    }
}
