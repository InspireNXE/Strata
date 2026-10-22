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
import org.inspirenxe.strata.api.layout.GridLength;
import org.inspirenxe.strata.api.layout.GridPanel;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sizes, lengths, spacing and offsets refuse values that would poison layout.
 */
class SizeValuesTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void sizesMustBeFiniteAndNotNegative() {
        final Box box = new Box(this.ui, 30, 10);

        assertThrows(IllegalArgumentException.class, () -> box.setMinSize(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> box.setMinSize(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> box.setMinSize(Float.POSITIVE_INFINITY, 0));
        assertThrows(IllegalArgumentException.class, () -> box.setMaxSize(0, Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> box.setMaxSize(0, -1));
        assertThrows(IllegalArgumentException.class, () -> box.setWidth(-1));
        assertThrows(IllegalArgumentException.class, () -> box.setHeight(Float.POSITIVE_INFINITY));
        box.setMaxSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY).setSize(Float.NaN, Float.NaN);
    }

    @Test
    void theMinimumWinsOverASmallerMaximum() {
        final Box box = new Box(this.ui, 30, 10);
        box.setMinSize(50, 0).setMaxSize(20, Float.POSITIVE_INFINITY);
        this.test.show(this.ui.absolutePanel().add(box, 0, 0));

        assertEquals(50, box.bounds().width());
    }

    @Test
    void aMeasuredHandlerCannotSetANonFiniteSize() {
        final Box box = new Box(this.ui, 30, 10);
        box.onEvent(Component.MEASURED, event -> event.setWidth(Float.NaN));

        assertThrows(IllegalArgumentException.class,
                () -> this.test.show(this.ui.absolutePanel().add(box, 0, 0)));
    }

    @Test
    void aRectangleWithANanSizeIsEmptyAndAnEmptyOneOverlapsNothing() {
        assertTrue(new Rect(0, 0, Float.NaN, 5).isEmpty());
        assertEquals(0f, new Rect(0, 0, Float.NaN, 5).width());
        assertFalse(new Rect(5, 5, 0, 0).intersects(new Rect(0, 0, 10, 10)));
        assertFalse(new Rect(0, 0, 10, 10).intersects(new Rect(5, 5, 0, 0)));
    }

    @Test
    void gridLengthsAndSpacingMustBeFinite() {
        assertThrows(IllegalArgumentException.class, () -> GridLength.weighted(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> GridLength.weighted(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> GridLength.fixed(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> GridLength.fixed(Float.POSITIVE_INFINITY));
        final GridPanel grid = this.ui.gridPanel().columns(GridLength.auto(), GridLength.auto());

        assertThrows(NullPointerException.class, () -> grid.columns(GridLength.auto(), null));
        assertEquals(2, grid.columnDefinitions().size(), "a bad call keeps the old definitions");
        assertThrows(IllegalArgumentException.class, () -> grid.setSpacing(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class,
                () -> this.ui.stackPanel().setSpacing(Float.POSITIVE_INFINITY));
    }

    @Test
    void aScrollOffsetMustNotBeNan() {
        final ScrollPanel panel = this.ui.scrollPanel();

        assertThrows(IllegalArgumentException.class, () -> panel.setScroll(Float.NaN, 0));
        assertThrows(IllegalArgumentException.class, () -> panel.scrollBy(0, Float.NaN));
    }
}
