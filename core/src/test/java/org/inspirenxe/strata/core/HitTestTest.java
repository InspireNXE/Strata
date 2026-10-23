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
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Finding the component under a point (design 4.48).
 */
class HitTestTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();
    private final Box left = new Box(this.ui, 40, 40);
    private final Box right = new Box(this.ui, 40, 40);
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.left, 10, 10).add(this.right, 100, 10);

    HitTestTest() {
        this.test.show(this.panel);
    }

    private Component<?> at(final float x, final float y) {
        return this.ui.componentAt(x, y).orElse(null);
    }

    @Test
    void findsTheDeepestComponentAndItsContainerAroundIt() {
        assertSame(this.left, this.at(20, 20));
        assertSame(this.right, this.at(110, 20));
        assertSame(this.panel, this.at(80, 30));
        assertEquals(Optional.empty(), this.ui.componentAt(-5, 20));
    }

    @Test
    void skipsHiddenComponentsAndFindsDisabledOnes() {
        this.left.setVisible(false);
        this.right.setEnabled(false);
        assertSame(this.panel, this.at(20, 20));
        assertSame(this.right, this.at(110, 20));
    }

    @Test
    void laysOutFirstSoAMovedComponentIsFoundWhereItIsNow() {
        final Box moved = new Box(this.ui, 20, 20);
        this.panel.add(moved, 150, 60);
        assertSame(moved, this.at(155, 65));
    }

    @Test
    void looksAtOverlaysFromTheTopAndStopsAtADialog() {
        final Box popup = new Box(this.ui, 30, 30);
        this.ui.overlays().popup(popup, new Rect(10, 10, 10, 10));
        assertSame(popup, this.at(15, 25));
        assertSame(this.right, this.at(110, 20));

        final Box dialog = new Box(this.ui, 20, 20);
        dialog.setAlignment(HorizontalAlignment.CENTER, VerticalAlignment.MIDDLE);
        this.ui.overlays().dialog(dialog);
        assertEquals(Optional.empty(), this.ui.componentAt(110, 20));
        assertSame(dialog, this.at(100, 50));
    }
}
