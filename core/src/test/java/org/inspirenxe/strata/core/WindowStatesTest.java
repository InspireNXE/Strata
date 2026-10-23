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
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Active, maximized and collapsed windows (design 4.57).
 */
class WindowStatesTest {
    private final TestHost test = new TestHost(400, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();
    private final Button ok = this.ui.button("OK");
    private final Window window = this.ui.window("Plan").add(this.ok);
    private final Window other = this.ui.window("Notes").add(this.ui.button("Save"));

    WindowStatesTest() {
        this.window.setSize(120, 80);
        this.window.setAlignment(org.inspirenxe.strata.api.layout.HorizontalAlignment.LEFT,
                org.inspirenxe.strata.api.layout.VerticalAlignment.TOP);
        this.other.setSize(120, 80);
        this.window.onEvent(Window.MAXIMIZE, event -> this.heard.add("maximize? " + event.value()))
                .onEvent(Window.MAXIMIZED, event -> this.heard.add("maximized " + event.value()))
                .onEvent(Window.COLLAPSE, event -> this.heard.add("collapse? " + event.value()))
                .onEvent(Window.COLLAPSED, event -> this.heard.add("collapsed " + event.value()));
        final AbsolutePanel panel = this.ui.absolutePanel();
        panel.setAnchor(Anchor.FILL);
        this.test.show(panel.add(this.window, 10, 10).add(this.other, 200, 10));
        this.test.host().layout();
    }

    private void clickIn(final Rect bounds, final float dx, final float dy) {
        this.test.click(bounds.x() + dx, bounds.y() + dy);
    }

    @Test
    void theWindowWithFocusIsActiveAndElseTheOnePressedLast() {
        assertTrue(this.other.isActive());
        assertFalse(this.window.isActive());
        this.ok.requestFocus();
        assertTrue(this.window.isActive());
        assertFalse(this.other.isActive());
        this.test.host().root().clearFocus();
        this.clickIn(this.other.bounds(), 60, 50);
        this.clickIn(this.window.bounds(), 60, 70);
        this.test.host().root().clearFocus();
        assertTrue(this.window.isActive());
        assertFalse(this.other.isActive());
    }

    @Test
    void anInactiveWindowDrawsTheInactiveTitleBar() {
        this.ok.requestFocus();
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final Rect bar = this.other.bounds();
        assertTrue(log.lines().stream().anyMatch(line -> line.startsWith("fill " + DrawLog.rect(bar).split(" ")[0])
                && line.endsWith("#FF1F2023")), log.lines().toString());
    }

    @Test
    void theMaximizeButtonFillsTheSlotAndRestoresWhatItHad() {
        // An anchor panel gives each child the whole panel as its slot
        ((AbsolutePanel) this.window.parent().orElseThrow()).remove(this.window);
        this.test.show(this.ui.anchorPanel().add(this.window).setAnchor(Anchor.FILL));
        this.window.setOffset(10, 10).setMaximizable(true);
        this.test.host().layout();
        final Rect before = this.window.bounds();
        // The maximize button sits left of the close button, each 12 wide and a unit apart
        this.clickIn(before, before.width() - 13f - 7f, 5f);
        this.test.host().layout();
        assertTrue(this.window.isMaximized());
        assertEquals(400f, this.window.bounds().width());
        assertEquals(300f, this.window.bounds().height());
        this.window.setMaximized(false);
        this.test.host().layout();
        assertEquals(before, this.window.bounds());
        assertEquals(List.of("maximize? true", "maximized true", "maximized false"), this.heard);
    }

    @Test
    void aDoubleClickOnTheTitleBarMaximizesAMaximizableWindow() {
        final Rect bar = this.window.bounds();
        this.clickIn(bar, 30, 5);
        this.clickIn(bar, 30, 5);
        assertFalse(this.window.isMaximized());
        this.window.setMaximizable(true);
        this.test.platform().advanceMillis(1000);
        this.test.host().layout();
        this.clickIn(bar, 30, 5);
        this.clickIn(bar, 30, 5);
        assertTrue(this.window.isMaximized());
    }

    @Test
    void theCollapseButtonShowsTheTitleBarAloneAndACanceledRequestKeepsIt() {
        this.window.setCollapsible(true);
        this.test.host().layout();
        final Rect before = this.window.bounds();
        this.clickIn(before, before.width() - 13f - 7f, 5f);
        this.test.host().layout();
        assertTrue(this.window.isCollapsed());
        assertFalse(this.ok.isVisible());
        assertTrue(this.window.bounds().height() < 30f, this.window.bounds().toString());
        this.window.setCollapsed(false);
        this.test.host().layout();
        assertTrue(this.ok.isVisible());
        assertEquals(before, this.window.bounds());
        this.window.onEvent(Window.COLLAPSE, event -> event.cancel());
        this.test.platform().advanceMillis(1000);
        this.clickIn(before, before.width() - 13f - 7f, 5f);
        assertFalse(this.window.isCollapsed());
        assertEquals(List.of("collapse? true", "collapsed true", "collapsed false", "collapse? true"), this.heard);
    }
}
