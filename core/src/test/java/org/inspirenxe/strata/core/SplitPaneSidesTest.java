/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Minimums on every layout, resize policies and collapsible sides of a split pane (design 4.57). The pane starts
 * 204 wide, so 200 is shared and each side starts at 100.
 */
class SplitPaneSidesTest {
    private final TestHost test = new TestHost(204, 100);
    private final ComponentFactory ui = this.test.factory();
    private final Button left = this.ui.button("Left").setAnchor(Anchor.FILL);
    private final Button right = this.ui.button("Right").setAnchor(Anchor.FILL);
    private final SplitPane pane = this.ui.splitPane().setFirst(this.left).setSecond(this.right);
    private final List<String> heard = new ArrayList<>();

    SplitPaneSidesTest() {
        this.pane.setAnchor(Anchor.FILL)
                .onEvent(SplitPane.COLLAPSE, event -> this.heard.add("ask " + event.value()))
                .onEvent(SplitPane.COLLAPSED, event -> this.heard.add(event.previous() + "->" + event.value()));
        this.test.show(this.pane);
    }

    private void resize(final float width) {
        this.test.host().resize(width, 100);
        this.test.host().layout();
    }

    private void doubleClickDivider() {
        // Far enough from any earlier click that these two make a double click
        this.test.platform().advanceMillis(1000);
        final float x = this.left.bounds().right() + SplitPane.DIVIDER_SIZE / 2f;
        this.test.click(x, 50);
        this.test.click(x, 50);
        this.test.host().layout();
    }

    @Test
    void minimumsHoldWheneverThePaneIsLaidOut() {
        this.pane.setMinSizes(150, 20).setPosition(0.8f);
        this.resize(184);
        assertEquals(150f, this.left.bounds().width());
        assertEquals(30f, this.right.bounds().width());
        assertEquals(0.8f, this.pane.position());
        this.resize(504);
        assertEquals(400f, this.left.bounds().width());
    }

    @Test
    void withoutRoomForBothTheSecondGivesWayUnlessThePolicyKeepsIt() {
        this.pane.setMinSizes(100, 80);
        this.resize(154);
        assertEquals(100f, this.left.bounds().width());
        assertEquals(50f, this.right.bounds().width());
        this.pane.setResizePolicy(SplitPane.ResizePolicy.KEEP_SECOND);
        this.test.host().layout();
        assertEquals(70f, this.left.bounds().width());
        assertEquals(80f, this.right.bounds().width());
    }

    @Test
    void keepFirstKeepsTheFirstSidesLengthAsThePaneGrows() {
        this.pane.setResizePolicy(SplitPane.ResizePolicy.KEEP_FIRST).setPosition(0.25f);
        this.test.host().layout();
        assertEquals(50f, this.left.bounds().width());
        final List<Float> moved = new ArrayList<>();
        this.pane.onEvent(SplitPane.MOVED, event -> moved.add(event.value()));
        this.resize(404);
        assertEquals(50f, this.left.bounds().width());
        assertEquals(350f, this.right.bounds().width());
        assertEquals(0.125f, this.pane.position());
        assertEquals(List.of(), moved);
    }

    @Test
    void keepSecondKeepsTheSecondSidesLength() {
        this.pane.setResizePolicy(SplitPane.ResizePolicy.KEEP_SECOND);
        this.test.host().layout();
        this.resize(304);
        assertEquals(100f, this.right.bounds().width());
        assertEquals(200f, this.left.bounds().width());
        this.resize(104);
        assertEquals(100f, this.left.bounds().width() + this.right.bounds().width());
        this.resize(304);
        assertEquals(100f, this.right.bounds().width());
    }

    @Test
    void aDoubleClickOnTheDividerCollapsesAndRestoresTheCollapsibleSide() {
        this.pane.setCollapsible(true, false).setPosition(0.3f);
        this.test.host().layout();
        this.doubleClickDivider();
        assertEquals(Optional.of(SplitPane.Side.FIRST), this.pane.collapsed());
        assertTrue(this.left.parent().isEmpty());
        assertEquals(200f, this.right.bounds().width());
        this.test.platform().advanceMillis(1000);
        this.test.click(2f, 50);
        this.test.click(2f, 50);
        this.test.host().layout();
        assertTrue(this.pane.collapsed().isEmpty());
        assertEquals(60f, this.left.bounds().width());
        assertEquals(0.3f, this.pane.position());
        assertEquals(List.of("ask FIRST", "null->FIRST", "ask null", "FIRST->null"), this.heard);
    }

    @Test
    void enterOnTheFocusedDividerCollapsesTheSecondWhenOnlyItIsCollapsible() {
        this.pane.setCollapsible(false, true);
        this.pane.requestFocus();
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().layout();
        assertEquals(Optional.of(SplitPane.Side.SECOND), this.pane.collapsed());
        assertEquals(200f, this.left.bounds().width());
        this.test.host().key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
        assertEquals(0.5f, this.pane.position());
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        assertTrue(this.pane.collapsed().isEmpty());
    }

    @Test
    void aCanceledRequestAndPanesWithNothingCollapsibleStayAsTheyAre() {
        this.doubleClickDivider();
        this.pane.requestFocus();
        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
        assertTrue(this.pane.collapsed().isEmpty());
        this.pane.setCollapsible(true, true).onEvent(SplitPane.COLLAPSE, event -> event.cancel());
        this.doubleClickDivider();
        assertTrue(this.pane.collapsed().isEmpty());
        assertEquals(List.of("ask FIRST"), this.heard);
    }

    @Test
    void codeCollapsesEitherSideAndReplacingACollapsedSideWaitsForTheRestore() {
        this.pane.setCollapsed(SplitPane.Side.SECOND);
        this.pane.setCollapsed(SplitPane.Side.FIRST);
        assertEquals(List.of("null->SECOND", "SECOND->FIRST"), this.heard);
        assertTrue(this.right.parent().isPresent());
        final Button other = this.ui.button("Other").setAnchor(Anchor.FILL);
        this.pane.setFirst(other);
        assertTrue(other.parent().isEmpty());
        this.pane.setCollapsed(null);
        this.test.host().layout();
        assertTrue(other.parent().isPresent());
        assertEquals(100f, other.bounds().width());
        assertEquals(List.of(this.pane.first().orElseThrow()), List.of(other));
    }

    @Test
    void draggingTheDividerOfACollapsedPaneRestoresItFirst() {
        this.pane.setCollapsible(true, false).setCollapsed(SplitPane.Side.FIRST);
        this.test.host().layout();
        this.heard.clear();
        this.test.host().pointerButton(2, 50, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerMoved(30, 50, Modifiers.NONE);
        this.test.host().pointerMoved(60, 50, Modifiers.NONE);
        this.test.host().pointerButton(60, 50, MouseButton.LEFT, false, Modifiers.NONE);
        this.test.host().layout();
        assertTrue(this.pane.collapsed().isEmpty());
        assertEquals(List.of("ask null", "FIRST->null"), this.heard);
        assertEquals(58f, this.left.bounds().width());
    }
}
