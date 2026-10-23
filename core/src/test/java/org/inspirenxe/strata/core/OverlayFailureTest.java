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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.event.CauseStackImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Overlays stay consistent when the handlers told about them throw or close other overlays.
 */
class OverlayFailureTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aPopupThatClosesAnotherAsItClosesDoesNotBreakTheLightDismiss() {
        this.test.show(this.ui.absolutePanel());
        final Overlay lower = this.ui.overlays().popup(new Box(this.ui, 20, 20),
                new Rect(0, 0, 10, 10));
        final Overlay upper = this.ui.overlays().popup(new Box(this.ui, 20, 20),
                new Rect(100, 0, 10, 10));
        upper.onEvent(Closable.CLOSED, event -> lower.close());

        this.test.click(250, 250);

        assertFalse(upper.isOpen());
        assertFalse(lower.isOpen());
    }

    @Test
    void aDialogWhoseFocusHandlerThrowsStillClosesAllTheWay() {
        final TextBox below = this.ui.textBox();
        this.test.show(this.ui.absolutePanel().add(below, 0, 0));
        below.requestFocus();
        final TextBox inside = this.ui.textBox();
        final Overlay dialog = this.ui.overlays()
                .dialog(this.ui.absolutePanel().add(inside, 100, 100));
        assertSame(inside, this.test.focused());
        inside.onEvent(Focusable.FOCUS_LOST, event -> {
            throw new IllegalStateException("handler failed on purpose");
        });
        final List<String> heard = new ArrayList<>();
        dialog.onEvent(Closable.CLOSED, event -> heard.add("closed"));

        assertThrows(IllegalStateException.class, dialog::close);

        assertFalse(dialog.isOpen());
        assertFalse(inside.isAttached());
        assertEquals(List.of("closed"), heard);
        assertSame(below, this.test.focused());
    }

    @Test
    void aPaintHandlerMayCloseAnOverlay() {
        this.test.show(this.ui.absolutePanel());
        final Box painted = new Box(this.ui, 20, 20);
        this.ui.overlays().popup(painted, new Rect(0, 0, 10, 10));
        this.ui.overlays().popup(new Box(this.ui, 20, 20), new Rect(50, 0, 10, 10));
        final Overlay last = this.ui.overlays().popup(new Box(this.ui, 20, 20),
                new Rect(100, 0, 10, 10));
        painted.onEvent(Component.PAINT, event -> last.close());

        this.test.host().render(new NullCanvas());

        assertFalse(last.isOpen());
    }

    @Test
    void aClosedOverlayReportsTheUserOnAPlatformsOwnCauseStack() throws InterruptedException {
        // A stack that is not this thread's, as an engine's own stack is not core's
        final CauseStackImpl[] other = new CauseStackImpl[1];
        final Thread thread = new Thread(() -> other[0] = CauseStackImpl.forThread());
        thread.start();
        thread.join();
        final TestPlatform platform = new TestPlatform();
        platform.setCauses(other[0]);
        final TestHost host = new TestHost(platform, Themes.flat(), 300, 300);
        host.show(host.factory().absolutePanel());
        final List<Origin> heard = new ArrayList<>();
        host.factory().overlays().popup(new Box(host.factory(), 20, 20), new Rect(0, 0, 10, 10))
                .onEvent(Closable.CLOSED, event -> heard.add(event.origin()));

        host.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE);

        assertEquals(1, heard.size());
        assertInstanceOf(Origin.User.class, heard.get(0));
    }

    @Test
    void aToastStaysAToastWhenTheClockReadsBelowZero() {
        this.test.show(this.ui.absolutePanel());
        this.test.platform().advanceMillis(-100_000L);
        final Overlay toast = this.ui.overlays().toast(new Box(this.ui, 20, 20), 1_000L);

        this.test.host().key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE);
        assertTrue(toast.isOpen(), "Escape leaves toasts open");

        this.test.platform().advanceMillis(1_000L);
        this.test.host().render(new NullCanvas());
        assertFalse(toast.isOpen(), "it closes when its time runs out");
    }
}
