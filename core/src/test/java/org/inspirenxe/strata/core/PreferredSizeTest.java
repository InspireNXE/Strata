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
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A host answers a toolkit that asks how large its widget wants to be.
 */
class PreferredSizeTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    /**
     * Text that wraps: as wide as it may be up to 100 units, and as tall as 1000 square units need.
     */
    static final class Wrapping extends CustomComponent<Wrapping> {
        Wrapping(final ComponentFactory factory) {
            super(factory);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            final float width = Math.min(availableWidth, 100f);
            return new Size(width, 1000f / width);
        }
    }

    private StackPanel column() {
        return this.ui.stackPanel().setSpacing(4).setPadding(5)
                .add(new Box(this.ui, 60, 20))
                .add(new Box(this.ui, 80, 30));
    }

    @Test
    void theContentIsMeasuredWithNoLimit() {
        this.test.show(this.column().setMargin(2));

        final Size preferred = this.test.host().preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

        assertEquals(new Size(80 + 10 + 4, 20 + 4 + 30 + 10 + 4), preferred);
    }

    @Test
    void contentThatWrapsAnswersForTheWidthOffered() {
        this.test.show(new Wrapping(this.ui));

        assertEquals(new Size(100, 10), this.test.host().preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        assertEquals(new Size(50, 20), this.test.host().preferredSize(50, Float.POSITIVE_INFINITY));
    }

    @Test
    void theNextLayoutUsesTheHostsOwnSizeAgain() {
        final Wrapping text = new Wrapping(this.ui);
        final StackPanel column = this.ui.stackPanel().add(text).setAnchor(Anchor.FILL);
        this.test.show(column);
        assertEquals(new Rect(0, 0, 100, 10), text.bounds());

        assertEquals(new Size(50, 20), this.test.host().preferredSize(50, Float.POSITIVE_INFINITY));
        assertEquals(new Rect(0, 0, 100, 10), text.bounds(), "measuring for the preferred size arranges nothing");
        this.test.host().layout();

        assertEquals(new Rect(0, 0, 300, 200), column.bounds());
        assertEquals(new Rect(0, 0, 100, 10), text.bounds());
        assertEquals(10, text.desiredHeight());
    }

    @Test
    void overlaysAreLeftOut() {
        this.test.show(this.column());
        this.test.host().root().overlays().popup(new Box(this.ui, 500, 500), new Rect(0, 0, 10, 10));

        assertEquals(new Size(90, 64), this.test.host().preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    void aHostWithNoScreenWantsNothing() {
        final ScreenHost host = StrataCore.createScreenHost(new TestPlatform(), Themes.flat());

        assertEquals(Size.ZERO, host.preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    void aStackAnswersForItsTopScreen() {
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        assertEquals(Size.ZERO, stack.preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        stack.push(factory -> new Box(factory, 40, 30));
        stack.push(factory -> new Box(factory, 70, 10));

        assertEquals(new Size(70, 10), stack.preferredSize(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
    }

    @Test
    void negativeOrNaNSpaceIsRefused() {
        this.test.show(this.column());

        assertThrows(IllegalArgumentException.class, () -> this.test.host().preferredSize(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> this.test.host().preferredSize(10, Float.NaN));
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        assertThrows(IllegalArgumentException.class, () -> stack.preferredSize(Float.NaN, 10));
    }
}
