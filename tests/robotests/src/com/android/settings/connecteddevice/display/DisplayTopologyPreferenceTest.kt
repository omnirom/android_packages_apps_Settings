/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.android.settings.connecteddevice.display

import android.content.Context
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.DisplayTopology
import android.hardware.display.DisplayTopology.POSITION_BOTTOM
import android.hardware.display.DisplayTopology.POSITION_LEFT
import android.hardware.display.DisplayTopology.POSITION_RIGHT
import android.hardware.display.DisplayTopology.POSITION_TOP
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Size
import android.view.Display.DEFAULT_DISPLAY
import android.view.Display.Mode
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewManager
import android.widget.FrameLayout
import androidx.preference.PreferenceViewHolder
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.view.MotionEventBuilder
import com.android.settings.R
import com.android.settings.flags.FakeFeatureFlagsImpl
import com.android.settings.flags.Flags.FLAG_SHOW_STACKED_MIRRORING_DISPLAY_CONNECTED_DISPLAY_SETTING
import com.android.settings.flags.Flags.FLAG_SHOW_TABBED_CONNECTED_DISPLAY_SETTING
import com.google.common.truth.Truth.assertThat
import java.util.function.Consumer
import kotlin.math.abs
import kotlin.math.min
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DisplayTopologyPreferenceTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val featureFlags = FakeFeatureFlagsImpl()
    val injector = TestInjector(context, featureFlags)
    val preference = DisplayTopologyPreference(injector)
    val rootView = View.inflate(context, preference.layoutResource, /* root= */ null)
    val holder = PreferenceViewHolder.createInstanceForTests(rootView)

    init {
        preference.onBindViewHolder(holder)

        featureFlags.setFlag(FLAG_SHOW_STACKED_MIRRORING_DISPLAY_CONNECTED_DISPLAY_SETTING, true)
        featureFlags.setFlag(FLAG_SHOW_TABBED_CONNECTED_DISPLAY_SETTING, false)
    }

    class TestInjector(context: Context, featureFlags: FakeFeatureFlagsImpl) :
        ConnectedDisplayInjector(context) {
        var displaysSize = mutableMapOf<Int, Size>()
        var topology: DisplayTopology? = null

        var topologyListener: Consumer<DisplayTopology>? = null
        var displayListener: DisplayManager.DisplayListener? = null

        override var displayTopology: DisplayTopology?
            get() = topology
            set(value) {
                topology = value
            }

        override val flags = DesktopExperienceFlags(featureFlags)

        /** A log of events related to wallpaper revealing. */
        val revealLog = mutableListOf<String>()

        override val densityDpi = DisplayMetrics.DENSITY_DEFAULT

        override fun getLogicalSize(displayId: Int): Size? {
            return displaysSize[displayId]
        }

        override fun getDisplays(): List<DisplayDevice> {
            return displaysSize
                .map { it ->
                    val mode = Mode(it.value.width, it.value.height, 60f)
                    DisplayDevice(
                        it.key,
                        "test:123",
                        "HDMI",
                        mode,
                        listOf(mode),
                        /* isEnabled= */ DisplayIsEnabled.YES,
                        /* isConnectedDisplay= */ true,
                    )
                }
                .toList()
        }

        override fun registerTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != null) {
                throw IllegalStateException("already have a listener registered: $topologyListener")
            }
            topologyListener = listener
        }

        override fun unregisterTopologyListener(listener: Consumer<DisplayTopology>) {
            if (topologyListener != listener) {
                throw IllegalStateException("no such listener registered: $listener")
            }
            topologyListener = null
        }

        override fun registerDisplayListener(listener: DisplayManager.DisplayListener) {
            if (displayListener != null) {
                throw IllegalStateException("already have a listener registered: $displayListener")
            }
            displayListener = listener
        }

        override fun unregisterDisplayListener(listener: DisplayManager.DisplayListener) {
            if (displayListener != listener) {
                throw IllegalStateException("no such listener registered: $listener")
            }
            displayListener = null
        }

        override fun revealWallpaper(displayId: Int): RevealedWallpaper {
            val childView = View(context)
            val viewManager =
                object : ViewManager {
                    override fun addView(view: View, params: ViewGroup.LayoutParams) {
                        revealLog.add("unexpected invocation of addView")
                    }

                    override fun updateViewLayout(view: View, params: ViewGroup.LayoutParams) {
                        revealLog.add("unexpected invocation of updateViewLayout")
                    }

                    override fun removeView(view: View) {
                        if (childView === view) {
                            revealLog.add("removed wallpaper revealer for display $displayId")
                        } else {
                            revealLog.add("invalid invocation of removeView")
                        }
                    }
                }

            revealLog.add("revealWallpaper invoked for display $displayId")

            return RevealedWallpaper(displayId, childView, viewManager)
        }
    }

    /** Returns the bounds of the non-highlighting part of the block relative to the parent. */
    private fun virtualBounds(block: DisplayBlock): RectF {
        val d = block.highlightPx.toFloat()
        val x = block.x + d
        val y = block.y + d
        // Using layoutParams as a proxy for the actual width and height appears to be standard
        // practice in Robolectric tests, as they do not actually process layout requests.
        val w = block.layoutParams.width - 2 * d
        val h = block.layoutParams.height - 2 * d
        return RectF(x, y, x + w, y + h)
    }

    private fun getPaneChildren(): List<DisplayBlock> =
        (0..<preference.controller.paneContent.childCount)
            .map { preference.controller.paneContent.getChildAt(it) as DisplayBlock }
            .toList()

    private fun setupSingleDisplay() {
        val root =
            DisplayTopology.TreeNode(
                DISPLAY_ID_1,
                DISPLAY_SIZE_1.width,
                DISPLAY_SIZE_1.height,
                DISPLAY_DENSITY,
                POSITION_LEFT,
                /* offset= */ 0f,
            )

        injector.topology = DisplayTopology(root, DISPLAY_ID_1)
        injector.displaysSize = mutableMapOf(Pair(DISPLAY_ID_1, DISPLAY_SIZE_1))
    }

    private fun setupTwoDisplays(childPosition: Int, childOffset: Float) {
        val child =
            DisplayTopology.TreeNode(
                DISPLAY_ID_2,
                DISPLAY_SIZE_2.width,
                DISPLAY_SIZE_2.height,
                DISPLAY_DENSITY,
                childPosition,
                childOffset,
            )
        val root =
            DisplayTopology.TreeNode(
                DISPLAY_ID_1,
                DISPLAY_SIZE_1.width,
                DISPLAY_SIZE_1.height,
                DISPLAY_DENSITY,
                POSITION_LEFT,
                /* offset= */ 0f,
            )
        root.addChild(child)

        injector.topology = DisplayTopology(root, DISPLAY_ID_1)
        injector.displaysSize =
            mutableMapOf(Pair(DISPLAY_ID_1, DISPLAY_SIZE_1), Pair(DISPLAY_ID_2, DISPLAY_SIZE_2))
    }

    /** Uses the topology in the injector to populate and prepare the pane for interaction. */
    private fun preparePane() {
        // This layoutParams needs to be non-null for the global layout handler.
        preference.controller.paneHolder.layoutParams =
            FrameLayout.LayoutParams(/* width= */ 640, /* height= */ 480)

        // Force pane width to have a reasonable value (hundreds of dp) so the TopologyScale is
        // calculated reasonably.
        preference.controller.paneContent.left = 0
        preference.controller.paneContent.right = 640

        preference.onAttached()
        preference.controller.refreshPane()
    }

    /**
     * Sets up a simple topology in the pane with two displays. Returns the left-hand display and
     * right-hand display in order in a list. The right-hand display is the root.
     */
    private fun setupPaneWithTwoDisplays(
        childPosition: Int = POSITION_LEFT,
        childOffset: Float = 42f,
    ): List<DisplayBlock> {
        setupTwoDisplays(childPosition, childOffset)

        preparePane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(2)

        // Block of child display is on the left.
        return if (paneChildren[0].x < paneChildren[1].x) paneChildren else paneChildren.reversed()
    }

    private fun assertSelected(block: DisplayBlock, expected: Boolean) {
        val vis = if (expected) View.VISIBLE else View.INVISIBLE
        assertThat(block.selectionMarkerView.visibility).isEqualTo(vis)
    }

    private fun setMirroringMode(enable: Boolean) {
        Settings.Secure.putInt(
            context.contentResolver,
            Settings.Secure.MIRROR_BUILT_IN_DISPLAY,
            if (enable) 1 else 0,
        )
    }

    private fun dragBlockWithOneMoveEvent(
        block: DisplayBlock,
        startTimeMs: Long,
        endTimeMs: Long,
        xDiff: Float,
        yDiff: Float,
    ) {
        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .setEventTime(startTimeMs)
                .build()
        )
        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(xDiff, yDiff)
                .setEventTime((startTimeMs + endTimeMs) / 2)
                .build()
        )
        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_UP)
                .setPointer(xDiff, yDiff)
                .setEventTime(endTimeMs)
                .build()
        )
    }

    @Test
    fun disabledTopology() {
        preference.onAttached()
        preference.controller.refreshPane()

        assertThat(preference.controller.paneContent.childCount).isEqualTo(0)
        // TODO(b/352648432): update test when we show the main display even when
        // a topology is not active.
        assertThat(preference.controller.topologyHint.text).isEqualTo("")
    }

    @Test
    fun twoDisplaysGenerateBlocks() {
        val (childBlock, rootBlock) = setupPaneWithTwoDisplays()
        val childBounds = virtualBounds(childBlock)
        val rootBounds = virtualBounds(rootBlock)

        // After accounting for padding, child should be half the length of root in each dimension.
        assertThat(childBounds.width()).isEqualTo(rootBounds.width() / 2)
        assertThat(childBounds.height()).isEqualTo(rootBounds.height() / 2)
        assertThat(childBounds.top).isGreaterThan(rootBounds.top)
        assertSelected(childBlock, false)
        assertSelected(rootBlock, false)
        assertThat(rootBounds.left).isEqualTo(childBounds.right)

        assertThat(preference.controller.topologyHint.text)
            .isEqualTo(context.getString(R.string.external_display_topology_hint))
    }

    @Test
    fun threeDisplaysMirroringGenerateBlocks() {
        setMirroringMode(true)
        setupPaneWithTwoDisplays()
        val newDisplaySize = Size(500, 500)
        injector.topology!!.addDisplay(
            DISPLAY_ID_3,
            newDisplaySize.width,
            newDisplaySize.height,
            DISPLAY_DENSITY,
        )
        injector.displaysSize[DISPLAY_ID_3] = newDisplaySize
        preference.controller.refreshPane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(3)

        for (i in 1..<paneChildren.size) {
            // Bounds are arranged 45 degrees diagonally from the top left corner, in a decreasing
            // X and increasing Y, since the backmost display will be the first on the list.
            val bounds = virtualBounds(paneChildren[i])
            val prevBounds = virtualBounds(paneChildren[i - 1])
            assertThat(bounds.left).isLessThan(prevBounds.left)
            assertThat(bounds.top).isGreaterThan(prevBounds.top)
            assertSelected(paneChildren[i], false)
        }

        assertThat(preference.controller.topologyHint.text).isEqualTo("")
    }

    @Test
    fun mirroringMode_usesLetterboxScalingCorrectly() {
        setMirroringMode(true)
        setupPaneWithTwoDisplays()
        val paneChildren = getPaneChildren()

        val externalMonitorBlock = paneChildren.find { it.logicalDisplayId == DISPLAY_ID_2 }
        assertThat(externalMonitorBlock).isNotNull()
        assertThat(externalMonitorBlock!!.surfaceSize).isEqualTo(DISPLAY_SIZE_1)

        val blockBounds = virtualBounds(externalMonitorBlock)
        val expectedScaleX = blockBounds.width() / DISPLAY_SIZE_1.width.toFloat()
        val expectedScaleY = blockBounds.height() / DISPLAY_SIZE_1.height.toFloat()
        val expectedLetterboxScale = min(expectedScaleX, expectedScaleY)
        assertThat(externalMonitorBlock.surfaceScale).isWithin(0.01f).of(expectedLetterboxScale)
        // Assert scaled surface size fits within the block size
        assertThat(expectedLetterboxScale * DISPLAY_SIZE_1.width.toFloat() <= blockBounds.width())
            .isTrue()
        assertThat(expectedLetterboxScale * DISPLAY_SIZE_1.height.toFloat() <= blockBounds.height())
            .isTrue()
    }

    @Test
    fun dragDisplayDownward() {
        val (leftBlock, _) = setupPaneWithTwoDisplays()

        preference.controller.timesRefreshedBlocks = 0

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setPointer(0f, 0f)
                .setAction(MotionEvent.ACTION_DOWN)
                .build()

        // Move the left block half of its height downward. This is 40 pixels in display
        // coordinates. The original offset is 42, so the new offset will be 42 + 40.
        val leftBounds = virtualBounds(leftBlock)
        val moveEvent =
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, leftBounds.height() / 2f)
                .build()
        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        leftBlock.dispatchTouchEvent(downEvent)
        leftBlock.dispatchTouchEvent(moveEvent)
        leftBlock.dispatchTouchEvent(upEvent)

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_LEFT)
        assertThat(child.offset).isWithin(1f).of(82f)

        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun dragRootDisplayToNewSide() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()
        val leftBounds = virtualBounds(leftBlock)

        assertThat(injector.revealLog)
            .containsExactly(
                "revealWallpaper invoked for display $DISPLAY_ID_1",
                "revealWallpaper invoked for display $DISPLAY_ID_2",
            )

        injector.revealLog.clear()

        preference.controller.timesRefreshedBlocks = 0

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()

        // Move the right block left and upward. We won't move it into exactly the correct position,
        // relying on the clamp algorithm to choose the correct side and offset.
        val moveEvent =
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(-leftBounds.width(), -leftBounds.height() / 2f)
                .build()

        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        assertThat(leftBlock.y).isGreaterThan(rightBlock.y)

        rightBlock.dispatchTouchEvent(downEvent)
        rightBlock.dispatchTouchEvent(moveEvent)
        rightBlock.dispatchTouchEvent(upEvent)

        assertThat(injector.revealLog).isEmpty()

        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_BOTTOM)
        assertThat(child.offset).isWithin(1f).of(0f)

        // After rearranging blocks, the original block views should still be present.
        val paneChildren = getPaneChildren()
        assertThat(paneChildren.indexOf(leftBlock)).isNotEqualTo(-1)
        assertThat(paneChildren.indexOf(rightBlock)).isNotEqualTo(-1)

        // Left edge of both blocks should be aligned after dragging.
        assertThat(paneChildren[0].x).isWithin(1f).of(paneChildren[1].x)

        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun noRefreshForUnmovingDrag() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()

        preference.controller.timesRefreshedBlocks = 0

        val downEvent =
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()

        val upEvent = MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()

        rightBlock.dispatchTouchEvent(downEvent)
        rightBlock.dispatchTouchEvent(upEvent)

        // After drag, the original views should still be present.
        val paneChildren = getPaneChildren()
        assertThat(paneChildren.indexOf(leftBlock)).isNotEqualTo(-1)
        assertThat(paneChildren.indexOf(rightBlock)).isNotEqualTo(-1)

        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun keepOriginalViewsWhenAddingMore() {
        setupPaneWithTwoDisplays()
        assertThat(injector.revealLog)
            .containsExactly(
                "revealWallpaper invoked for display $DISPLAY_ID_1",
                "revealWallpaper invoked for display $DISPLAY_ID_2",
            )
        injector.revealLog.clear()
        val childrenBefore = getPaneChildren()
        val newDisplaySize = Size(320, 240)
        injector.topology!!.addDisplay(
            DISPLAY_ID_3,
            newDisplaySize.width,
            newDisplaySize.height,
            DISPLAY_DENSITY,
        )
        injector.displaysSize[DISPLAY_ID_3] = newDisplaySize
        preference.controller.refreshPane()
        assertThat(injector.revealLog)
            .containsExactly("revealWallpaper invoked for display $DISPLAY_ID_3")
        injector.revealLog.clear()
        val childrenAfter = getPaneChildren()

        assertThat(childrenBefore).hasSize(2)
        assertThat(childrenAfter).hasSize(3)
        assertThat(childrenAfter.subList(0, 2)).isEqualTo(childrenBefore)

        assertThat(injector.topology!!.removeDisplay(DISPLAY_ID_2)).isTrue()
        assertThat(injector.topology!!.removeDisplay(DISPLAY_ID_3)).isTrue()
        preference.controller.refreshPane()
        assertThat(injector.revealLog)
            .containsExactly(
                "removed wallpaper revealer for display $DISPLAY_ID_2",
                "removed wallpaper revealer for display $DISPLAY_ID_3",
            )
    }

    @Test
    fun changeToMirroringModeRemoveExistingRevealedWallpapersExceptDefault() {
        setupPaneWithTwoDisplays()
        assertThat(injector.revealLog)
            .containsExactly(
                "revealWallpaper invoked for display $DISPLAY_ID_1",
                "revealWallpaper invoked for display $DISPLAY_ID_2",
            )
        injector.revealLog.clear()

        setMirroringMode(true)
        preference.controller.refreshPane()
        assertThat(injector.revealLog)
            .containsExactly("removed wallpaper revealer for display $DISPLAY_ID_2")
        injector.revealLog.clear()
    }

    @Test
    fun stopMirroringModeRevealWallpapers() {
        setMirroringMode(true)
        setupPaneWithTwoDisplays()
        preference.controller.refreshPane()
        assertThat(injector.revealLog)
            .containsExactly("revealWallpaper invoked for display $DISPLAY_ID_1")
        injector.revealLog.clear()

        setMirroringMode(false)
        preference.controller.refreshPane()
        assertThat(injector.revealLog)
            .containsExactly("revealWallpaper invoked for display $DISPLAY_ID_2")
        injector.revealLog.clear()
    }

    @Test
    fun applyNewTopologyViaListenerUpdate() {
        setupPaneWithTwoDisplays()

        preference.controller.timesRefreshedBlocks = 0
        val newDisplaySize = Size(300, 320)
        val newTopology = injector.topology!!.copy()
        newTopology.addDisplay(
            DISPLAY_ID_3,
            newDisplaySize.width,
            newDisplaySize.height,
            DISPLAY_DENSITY,
        )
        injector.displaysSize[DISPLAY_ID_3] = newDisplaySize

        injector.topology = newTopology
        injector.topologyListener!!.accept(newTopology)

        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(3)

        // Look for a display with the same unusual aspect ratio as the one we've added.
        val expectedAspectRatio = newDisplaySize.width.toFloat() / newDisplaySize.height.toFloat()
        assertThat(
                paneChildren
                    .map { virtualBounds(it) }
                    .map { it.width() / it.height() }
                    .filter { abs(it - expectedAspectRatio) < 0.001f }
            )
            .hasSize(1)
    }

    @Test
    fun ignoreListenerUpdateOfUnchangedTopology() {
        setupTwoDisplays(POSITION_TOP, /* offset= */ 12.0f)
        preparePane()

        preference.controller.timesRefreshedBlocks = 0
        setupTwoDisplays(POSITION_TOP, /* offset= */ 12.1f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun cannotMoveSingleDisplay() {
        setupSingleDisplay()
        preparePane()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(1)
        val block = paneChildren[0]

        val origY = block.y

        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()
        )
        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 30f)
                .build()
        )

        assertThat(block.y).isWithin(0.01f).of(origY)

        block.dispatchTouchEvent(
            MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()
        )

        // Block should be back to original position.
        assertThat(block.y).isWithin(0.01f).of(origY)
    }

    @Test
    fun cannotMoveDisplayMirroringMode() {
        setMirroringMode(true)
        setupPaneWithTwoDisplays()

        val paneChildren = getPaneChildren()
        assertThat(paneChildren).hasSize(2)
        for (block in paneChildren) {
            val origY = block.y

            block.dispatchTouchEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(MotionEvent.ACTION_DOWN)
                    .setPointer(0f, 0f)
                    .build()
            )
            assertSelected(block, false)

            block.dispatchTouchEvent(
                MotionEventBuilder.newBuilder()
                    .setAction(MotionEvent.ACTION_MOVE)
                    .setPointer(0f, 30f)
                    .build()
            )
            assertThat(block.y).isWithin(0.01f).of(origY)

            block.dispatchTouchEvent(
                MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()
            )
            assertThat(block.y).isWithin(0.01f).of(origY)
        }
    }

    @Test
    fun updatedTopologyCancelsDragIfNonTrivialChange() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(143.76f)

        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()
        )
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 30f)
                .build()
        )
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(173.76f)

        // Offset is only different by 0.5 dp, so the drag will not cancel.
        setupTwoDisplays(POSITION_LEFT, /* childOffset= */ 41.5f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(173.76f)
        // Move block farther downward.
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, 50f)
                .build()
        )
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(193.76f)

        setupTwoDisplays(POSITION_LEFT, /* childOffset= */ 20f)
        injector.topologyListener!!.accept(injector.topology!!)

        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(115.60f)
        // Another move in the opposite direction should not move the left block.
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_MOVE)
                .setPointer(0f, -20f)
                .build()
        )
        assertThat(leftBlock.positionInPane.y).isWithin(0.05f).of(115.60f)
    }

    @Test
    fun highlightDuringDrag() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertSelected(leftBlock, false)
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()
        )
        assertSelected(leftBlock, true)
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()
        )
        assertSelected(leftBlock, false)
    }

    @Test
    fun showTabbedConnectedDisplaySettingFlagOn_keepHighlightAfterDrag() {
        featureFlags.setFlag(FLAG_SHOW_TABBED_CONNECTED_DISPLAY_SETTING, true)
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, /* childOffset= */ 42f)

        assertSelected(leftBlock, false)
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder()
                .setAction(MotionEvent.ACTION_DOWN)
                .setPointer(0f, 0f)
                .build()
        )
        assertSelected(leftBlock, true)
        leftBlock.dispatchTouchEvent(
            MotionEventBuilder.newBuilder().setAction(MotionEvent.ACTION_UP).build()
        )
        assertSelected(leftBlock, true)
    }

    @Test
    fun accidentalDrag_LittleAndBriefEnoughToBeAccidental() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, childOffset = 42f)
        val startTime = 424242L
        val startX = leftBlock.x
        val startY = leftBlock.y

        preference.controller.timesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
            leftBlock,
            startTime,
            endTimeMs = startTime + preference.controller.accidentalDragTimeLimitMs - 10,
            xDiff = preference.controller.accidentalDragDistancePx - 1f,
            yDiff = 0f,
        )
        assertThat(leftBlock.x).isEqualTo(startX)
        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(0)

        dragBlockWithOneMoveEvent(
            leftBlock,
            startTime,
            endTimeMs = startTime + preference.controller.accidentalDragTimeLimitMs - 10,
            xDiff = 0f,
            yDiff = preference.controller.accidentalDragDistancePx - 1f,
        )
        assertThat(leftBlock.y).isEqualTo(startY)
        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(0)
    }

    @Test
    fun accidentalDrag_TooFarToBeAccidentalXAxis() {
        val (topBlock, _) = setupPaneWithTwoDisplays(POSITION_TOP, childOffset = -42f)
        val startTime = 88888L
        val startX = topBlock.x

        preference.controller.timesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
            topBlock,
            startTime,
            endTimeMs = startTime + preference.controller.accidentalDragTimeLimitMs - 10,
            xDiff = preference.controller.accidentalDragDistancePx + 1f,
            yDiff = 0f,
        )
        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
        assertThat(topBlock.x).isNotEqualTo(startX)
    }

    @Test
    fun accidentalDrag_TooFarToBeAccidentalYAxis() {
        val (leftBlock, _) = setupPaneWithTwoDisplays(POSITION_LEFT, childOffset = 42f)
        val startTime = 88888L
        val startY = leftBlock.y

        preference.controller.timesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
            leftBlock,
            startTime,
            endTimeMs = startTime + preference.controller.accidentalDragTimeLimitMs - 10,
            xDiff = 0f,
            yDiff = preference.controller.accidentalDragDistancePx + 1f,
        )
        assertThat(leftBlock.y).isNotEqualTo(startY)
        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun accidentalDrag_TooSlowToBeAccidental() {
        val (topBlock, _) = setupPaneWithTwoDisplays(POSITION_TOP, childOffset = -42f)
        val startTime = 88888L
        val startX = topBlock.x

        preference.controller.timesRefreshedBlocks = 0
        dragBlockWithOneMoveEvent(
            topBlock,
            startTime,
            endTimeMs = startTime + preference.controller.accidentalDragTimeLimitMs + 10,
            xDiff = preference.controller.accidentalDragDistancePx - 1f,
            yDiff = 0f,
        )
        assertThat(topBlock.x).isNotEqualTo(startX)
        assertThat(preference.controller.timesRefreshedBlocks).isEqualTo(1)
    }

    @Test
    fun nonMirroringMode_updateOnlyFromDisplayTopologyUpdate() {
        setupPaneWithTwoDisplays()
        assertThat(getPaneChildren()).hasSize(2)

        val newDisplaySize = Size(500, 500)
        injector.topology!!.addDisplay(
            DISPLAY_ID_3,
            newDisplaySize.width,
            newDisplaySize.height,
            DISPLAY_DENSITY,
        )
        injector.displaysSize.put(DISPLAY_ID_3, newDisplaySize)

        injector.displayListener!!.onDisplayAdded(DISPLAY_ID_3)
        // In non-mirroring mode display listener update should be ignored, update will come from
        // DisplayTopologyListener update
        assertThat(getPaneChildren()).hasSize(2)

        injector.topologyListener!!.accept(injector.topology!!)
        // Pane updated
        assertThat(getPaneChildren()).hasSize(3)
    }

    @Test
    fun mirroringMode_noUpdateFromDisplayListenerUpdate() {
        setMirroringMode(true)
        setupPaneWithTwoDisplays()
        assertThat(getPaneChildren()).hasSize(2)

        val newDisplaySize = Size(500, 500)
        injector.topology!!.addDisplay(
            DISPLAY_ID_3,
            newDisplaySize.width,
            newDisplaySize.height,
            DISPLAY_DENSITY,
        )
        injector.displaysSize.put(DISPLAY_ID_3, newDisplaySize)

        injector.topologyListener!!.accept(injector.topology!!)
        // In mirroring mode display topology update should be ignored, update will come from
        // DisplayListener update
        assertThat(getPaneChildren()).hasSize(2)

        injector.displayListener!!.onDisplayAdded(DISPLAY_ID_3)
        // Pane updated
        assertThat(getPaneChildren()).hasSize(3)
    }

    @Test
    fun a11yActionMoveUp_movesBlock() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()
        val originalY = leftBlock.y

        // Trigger the "move up" accessibility action
        leftBlock.performAccessibilityAction(R.id.action_move_display_block_up, null)

        // Verify the block has moved up from its original position
        assertThat(leftBlock.y).isLessThan(originalY)

        // Verify the topology was updated correctly
        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_LEFT)
        // The offset should now be smaller (moved up)
        assertThat(child.offset).isLessThan(42f)
    }

    @Test
    fun a11yActionMoveDown_movesBlock() {
        val (leftBlock, rightBlock) = setupPaneWithTwoDisplays()
        val originalY = leftBlock.y

        // Trigger the "move down" accessibility action
        leftBlock.performAccessibilityAction(R.id.action_move_display_block_down, null)

        // Verify the block has moved down from its original position
        assertThat(leftBlock.y).isGreaterThan(originalY)

        // Verify the topology was updated correctly
        val rootChildren = injector.topology!!.root!!.children
        assertThat(rootChildren).hasSize(1)
        val child = rootChildren[0]
        assertThat(child.position).isEqualTo(POSITION_LEFT)
        // The offset should now be larger (moved down)
        assertThat(child.offset).isGreaterThan(42f)
    }

    @Test
    fun a11yAction_movesBlockToAdjacentSnapPoint() {
        // Arrange the displays so the child is on the right, but vertically offset,
        // creating an unambiguous 'up' move to align the centers.
        // The main display is 160dp tall, the child is 80dp. An offset of 40 puts the
        // child's top edge aligned with the parent's center.
        val (leftBlock, rightBlock) =
            setupPaneWithTwoDisplays(childPosition = POSITION_RIGHT, childOffset = 40f)

        // Get the initial Y position of the block to be moved.
        val originalY = rightBlock.y

        // Perform a single "move up" action. This should be enough to trigger
        // the clamping logic to snap the block into vertical alignment.
        rightBlock.performAccessibilityAction(R.id.action_move_display_block_up, null)

        // The block should have moved up to a new, stable position.
        assertThat(rightBlock.y).isLessThan(originalY)

        // Verify the underlying topology reflects the change. A perfectly centered
        // alignment would result in an offset of (parentHeight - childHeight) / 2
        // which is (160 - 80) / 2 = 40. Since we started at 40, a slight move up
        // should get clamped to a smaller offset. We just check it changed.
        val child = injector.topology!!.root!!.children[0]
        assertThat(child.position).isEqualTo(POSITION_RIGHT)
        assertThat(child.offset).isLessThan(40f)
    }

    @Test
    fun a11yAction_cannotMoveSingleDisplay() {
        setupSingleDisplay()
        preparePane()

        val block = getPaneChildren().first()
        val originalX = block.x
        val originalY = block.y

        // Attempt to move the single block
        block.performAccessibilityAction(R.id.action_move_display_block_down, null)

        // Verify the block has not moved, as there's nothing to rearrange
        assertThat(block.x).isWithin(0.01f).of(originalX)
        assertThat(block.y).isWithin(0.01f).of(originalY)
    }

    private companion object {
        private const val DISPLAY_ID_1 = DEFAULT_DISPLAY
        private const val DISPLAY_ID_2 = 123
        private const val DISPLAY_ID_3 = 456
        private val DISPLAY_SIZE_1 = Size(200, 160)
        private val DISPLAY_SIZE_2 = Size(100, 80)
        private const val DISPLAY_DENSITY = 160
    }
}
