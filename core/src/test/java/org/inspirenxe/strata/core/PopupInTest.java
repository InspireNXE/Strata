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
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.menu.Flyout;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Popups laid out in an area: {@code Overlays.popupIn}, core's moving of the area, and {@code Flyout.openIn}. Text in
 * the test platform is 6 units per code point and 9 per line.
 */
class PopupInTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private void show() {
        this.test.show(this.ui.stackPanel().add(this.ui.button("Behind")
                .onEvent(Button.PRESSED, event -> this.heard.add("behind pressed"))));
    }

    @Test
    void stretchedContentFillsTheArea() {
        this.show();
        final StackPanel pane = this.ui.stackPanel().add(this.ui.label("Pane"));
        pane.setAnchor(Anchor.FILL);
        this.ui.overlays().popupIn(pane, new Rect(10, 20, 100, 150));
        this.test.host().layout();
        assertEquals(new Rect(10, 20, 100, 150), pane.bounds());
    }

    @Test
    void otherContentTakesItsSizeWhereItsAlignmentPlacesIt() {
        this.show();
        final Label centered = this.ui.label("Hi");
        centered.setAnchor(Anchor.CENTER);
        this.ui.overlays().popupIn(centered, new Rect(10, 20, 100, 150));
        this.test.host().layout();
        assertEquals(new Rect(54, 90.5f, 12, 9), centered.bounds());
    }

    @Test
    void theAreaIsCutToTheHost() {
        this.show();
        final StackPanel pane = this.ui.stackPanel();
        pane.setAnchor(Anchor.FILL);
        this.ui.overlays().popupIn(pane, new Rect(250, -20, 100, 100));
        this.test.host().layout();
        assertEquals(new Rect(250, 0, 50, 80), pane.bounds());
    }

    @Test
    void closesOnAPressOutsideAndOnEscapeLikeAPopup() {
        this.show();
        final StackPanel pane = this.ui.stackPanel();
        pane.setAnchor(Anchor.FILL);
        final Overlay overlay = this.ui.overlays().popupIn(pane, new Rect(150, 0, 150, 200));
        assertFalse(overlay.isModal());
        overlay.onEvent(Closable.CLOSED, event -> this.heard.add("closed"));
        this.test.click(10, 5);
        assertFalse(overlay.isOpen());
        // The press closed the popup and went no further
        assertEquals(List.of("closed"), this.heard);

        final Overlay again = this.ui.overlays().popupIn(this.ui.stackPanel(), new Rect(150, 0, 150, 200));
        assertTrue(this.test.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE));
        assertFalse(again.isOpen());
    }

    @Test
    void coreCanMoveTheArea() {
        this.show();
        final StackPanel pane = this.ui.stackPanel();
        pane.setAnchor(Anchor.FILL);
        final Overlay overlay = this.ui.overlays().popupIn(pane, new Rect(0, 0, 50, 50));
        this.test.host().root().setPopupArea(overlay, new Rect(10, 10, 60, 70));
        this.test.host().layout();
        assertEquals(new Rect(10, 10, 60, 70), pane.bounds());
        final Overlay anchored = this.ui.overlays().popup(this.ui.stackPanel(), new Rect(0, 0, 10, 10));
        assertThrows(IllegalArgumentException.class,
                () -> this.test.host().root().setPopupArea(anchored, new Rect(0, 0, 1, 1)));
    }

    @Test
    void refusesContentInATree() {
        this.show();
        final Label placed = this.ui.label("Placed");
        this.ui.stackPanel().add(placed);
        assertThrows(IllegalArgumentException.class,
                () -> this.ui.overlays().popupIn(placed, new Rect(0, 0, 10, 10)));
    }

    @Test
    void aFlyoutOpensInAnAreaAtItsSize() {
        this.show();
        final Flyout flyout = this.ui.flyout(this.ui.label("Hello"));
        // Beside the button: from its right edge to the host's
        flyout.openIn(new Rect(60, 0, 240, 200));
        assertTrue(flyout.isOpen());
        // The label is 30 by 9; the flyout's padding of 4 makes 38 by 17 at the area's corner
        assertEquals(new Rect(60, 0, 38, 17), flyout.bounds());
        // Opening an open flyout does nothing
        flyout.openIn(new Rect(0, 0, 10, 10));
        assertEquals(new Rect(60, 0, 38, 17), flyout.bounds());
        flyout.close();
        assertFalse(flyout.isOpen());
    }

    @Test
    void aFlyoutInAnAreaIsNoLargerThanIt() {
        this.show();
        final Flyout flyout = this.ui.flyout(this.ui.spacer(500, 500));
        flyout.openIn(new Rect(100, 50, 80, 60));
        assertEquals(new Rect(100, 50, 80, 60), flyout.bounds());
    }

    @Test
    void aFlyoutInAnAreaKeepsItsLightDismissAndFocus() {
        this.show();
        final Button inside = this.ui.button("Inside");
        final Flyout flyout = this.ui.flyout(inside).setLightDismiss(false);
        flyout.openIn(new Rect(150, 100, 150, 100));
        assertSame(inside, this.test.focused());
        this.test.click(10, 5);
        assertTrue(flyout.isOpen());
        assertEquals(List.of("behind pressed"), this.heard);
    }
}
