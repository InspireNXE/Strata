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
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The display's density: hosts and stacks take it from the adapter, components read it from their environment and
 * painters, and a change lays everything out again.
 */
class DensityTest {
    private final TestHost test = new TestHost(200, 100);

    /**
     * A chart's grid line, written as a developer would: one device pixel thick, whatever the density.
     */
    static final class GridLine extends CustomComponent<GridLine> {
        final List<Float> measured = new ArrayList<>();
        final List<Float> painted = new ArrayList<>();
        final List<Float> scaled = new ArrayList<>();
        private Painter painter;

        GridLine(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            this.measured.add(this.environment().pixelsPerUnit());
            return new Size(50f, 10f);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            this.painted.add(painter.pixelsPerUnit());
            painter.fill(new Rect(bounds.x(), bounds.y(), bounds.width(), 1f / painter.pixelsPerUnit()), 0xFF808080);
            this.painter = painter;
            painter.scaled(1.5f, this::recordScaled);
        }

        private void recordScaled() {
            this.scaled.add(this.painter.pixelsPerUnit());
        }
    }

    @Test
    void theDensityIsOneUntilTheAdapterSetsIt() {
        final GridLine line = new GridLine(this.test.factory());
        this.test.show(line);
        this.test.host().render(new NullCanvas());
        assertEquals(List.of(1f), line.measured);
        assertEquals(List.of(1f), line.painted);
        assertEquals(List.of(1.5f), line.scaled, "a scale multiplies the painter's density");
    }

    @Test
    void aChangeOfDensityLaysOutAgainAndReachesPainting() {
        final GridLine line = new GridLine(this.test.factory());
        this.test.show(line);
        this.test.host().layout();
        this.test.host().setPixelsPerUnit(2f);
        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);
        assertEquals(List.of(1f, 2f), line.measured, "a new density measures again");
        assertEquals(List.of(2f), line.painted);
        assertEquals(List.of(3f), line.scaled);
        assertEquals(0.5f, canvas.fills().get(0).height(), "the hairline is one device pixel");
    }

    @Test
    void settingTheSameDensityLaysNothingOut() {
        final GridLine line = new GridLine(this.test.factory());
        this.test.show(line);
        this.test.host().setPixelsPerUnit(1.5f);
        this.test.host().layout();
        final int measures = line.measured.size();
        this.test.host().setPixelsPerUnit(1.5f);
        this.test.host().layout();
        assertEquals(measures, line.measured.size());
        assertEquals(1.5f, line.measured.get(measures - 1));
    }

    @Test
    void aDensityMustBeFiniteAndPositive() {
        assertThrows(IllegalArgumentException.class, () -> this.test.host().setPixelsPerUnit(0f));
        assertThrows(IllegalArgumentException.class, () -> this.test.host().setPixelsPerUnit(-2f));
        assertThrows(IllegalArgumentException.class, () -> this.test.host().setPixelsPerUnit(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> this.test.host().setPixelsPerUnit(Float.POSITIVE_INFINITY));
    }

    @Test
    void aStackSetsTheDensityOfEveryScreenAndOfLaterOnes() {
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        stack.resize(200, 100);
        final List<GridLine> lines = new ArrayList<>();
        stack.push(ui -> {
            final GridLine line = new GridLine(ui);
            lines.add(line);
            return line;
        });
        stack.setPixelsPerUnit(2f);
        stack.push(ui -> {
            final GridLine line = new GridLine(ui);
            lines.add(line);
            return line;
        });
        stack.render(new NullCanvas());
        assertEquals(List.of(2f), lines.get(1).painted, "a screen pushed later has the stack's density");
        assertEquals(2f, lines.get(0).measured.get(lines.get(0).measured.size() - 1), "and so does one below it");
        assertThrows(IllegalArgumentException.class, () -> stack.setPixelsPerUnit(0f));
    }
}
