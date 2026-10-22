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
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.input.KeyRepeater;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Held step buttons, held scroll bar tracks and arrows, and precise scrolls on a spinner (design 4.57).
 */
class HoldRepeatTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void frameAfter(final long millis) {
        this.test.platform().advanceMillis(millis);
        this.test.host().render(new NullCanvas());
    }

    private void press(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
    }

    private void release(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    @Nested
    class Spinners {
        private final Spinner spinner = HoldRepeatTest.this.ui.spinner().setRange(0, 100);
        private final List<String> origins = new ArrayList<>();

        Spinners() {
            this.spinner.setSize(80, 20);
            this.spinner.onEvent(Ranged.CHANGE, event -> this.origins.add(
                    event.origin() instanceof Origin.User ? "user" : "other"));
            HoldRepeatTest.this.test.show(HoldRepeatTest.this.ui.absolutePanel().add(this.spinner, 10, 10));
            HoldRepeatTest.this.test.host().layout();
        }

        private float upX() {
            return this.spinner.bounds().right() - Spinner.BUTTON_WIDTH / 2f;
        }

        @Test
        void aHeldStepButtonRepeatsOnTheKeyRepeatTimingUntilReleased() {
            HoldRepeatTest.this.press(this.upX(), 13);
            assertEquals(1d, this.spinner.value());
            HoldRepeatTest.this.frameAfter(KeyRepeater.INITIAL_DELAY_MILLIS - 1);
            assertEquals(1d, this.spinner.value());
            HoldRepeatTest.this.frameAfter(1);
            assertEquals(2d, this.spinner.value());
            HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            assertEquals(4d, this.spinner.value());
            HoldRepeatTest.this.release(this.upX(), 13);
            HoldRepeatTest.this.frameAfter(1000);
            assertEquals(4d, this.spinner.value());
            assertEquals(List.of("user", "user", "user", "user"), this.origins);
        }

        @Test
        void theLowerButtonRepeatsDownAndADisabledSpinnerStops() {
            this.spinner.setValue(50);
            HoldRepeatTest.this.press(this.upX(), 27);
            HoldRepeatTest.this.frameAfter(KeyRepeater.INITIAL_DELAY_MILLIS);
            assertEquals(48d, this.spinner.value());
            this.spinner.setEnabled(false);
            HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            this.spinner.setEnabled(true);
            HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            assertEquals(48d, this.spinner.value());
        }

        @Test
        void preciseScrollsAddUpToOneStepPerNotch() {
            final Rect bounds = this.spinner.bounds();
            for (int i = 0; i < 9; i++) {
                HoldRepeatTest.this.test.host().scroll(bounds.x() + 5, bounds.y() + 5, 0, 0.25f, ScrollKind.PRECISE,
                        Modifiers.NONE);
            }
            assertEquals(2d, this.spinner.value());
            HoldRepeatTest.this.test.host().scroll(bounds.x() + 5, bounds.y() + 5, 0, -0.5f, ScrollKind.PRECISE,
                    Modifiers.NONE);
            assertEquals(2d, this.spinner.value());
            HoldRepeatTest.this.test.host().scroll(bounds.x() + 5, bounds.y() + 5, 0, -0.5f, ScrollKind.PRECISE,
                    Modifiers.NONE);
            assertEquals(1d, this.spinner.value());
            HoldRepeatTest.this.test.host().scroll(bounds.x() + 5, bounds.y() + 5, 0, 1f, ScrollKind.WHEEL,
                    Modifiers.NONE);
            assertEquals(2d, this.spinner.value());
        }
    }

    @Nested
    class ScrollBars {
        private final ScrollBar bar = HoldRepeatTest.this.ui.scrollBar().setExtent(20, 220);

        ScrollBars() {
            this.bar.setSize(10, 100);
            HoldRepeatTest.this.test.show(HoldRepeatTest.this.ui.absolutePanel().add(this.bar, 0, 0));
            HoldRepeatTest.this.test.host().layout();
        }

        @Test
        void aHeldTrackPagesUntilTheThumbReachesThePointer() {
            // A page is 10 here, the visible amount less a scroll rate, and the thumb is 10 long at the top
            HoldRepeatTest.this.press(5, 45);
            assertEquals(10d, this.bar.value());
            HoldRepeatTest.this.frameAfter(KeyRepeater.INITIAL_DELAY_MILLIS);
            assertEquals(20d, this.bar.value());
            for (int i = 0; i < 20; i++) {
                HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            }
            final Rect thumb = HoldRepeatTest.this.thumbOf(this.bar);
            assertTrue(thumb.y() <= 45 && thumb.bottom() > 45, thumb + " at " + this.bar.value());
            final double reached = this.bar.value();
            HoldRepeatTest.this.frameAfter(KeyRepeater.INTERVAL_MILLIS);
            assertEquals(reached, this.bar.value());
            HoldRepeatTest.this.release(5, 45);
        }

        @Test
        void arrowsStepByTheScrollRateAndRepeatWhileHeld() {
            this.bar.setArrowsShown(true);
            assertTrue(this.bar.arrowsShown());
            HoldRepeatTest.this.test.host().layout();
            HoldRepeatTest.this.press(5, 95);
            assertEquals(10d, this.bar.value());
            HoldRepeatTest.this.frameAfter(KeyRepeater.INITIAL_DELAY_MILLIS);
            assertEquals(20d, this.bar.value());
            HoldRepeatTest.this.release(5, 95);
            HoldRepeatTest.this.press(5, 5);
            HoldRepeatTest.this.release(5, 5);
            assertEquals(10d, this.bar.value());
            HoldRepeatTest.this.frameAfter(KeyRepeater.INITIAL_DELAY_MILLIS);
            assertEquals(10d, this.bar.value());
        }

        @Test
        void arrowsAreTrianglesInTheThumbsPaintWithoutTheirKeys() {
            this.bar.setArrowsShown(true);
            HoldRepeatTest.this.test.host().layout();
            final DrawLog log = new DrawLog();
            HoldRepeatTest.this.test.host().render(log);
            final int thumb = ((Paint.Solid) Themes.flat().paint(ScrollBar.STYLE_THUMB).orElseThrow()).argb();
            final String color = String.format("#%08X", thumb);
            // A 10 unit button holds a triangle 7 wide and 4 long: the start one tip up, the end one tip down
            assertTrue(log.lines().contains("fill 4,3 1x1 " + color), log.lines().toString());
            assertTrue(log.lines().contains("fill 1,6 7x1 " + color), log.lines().toString());
            assertTrue(log.lines().contains("fill 1,93 7x1 " + color), log.lines().toString());
            assertTrue(log.lines().contains("fill 4,96 1x1 " + color), log.lines().toString());
        }

        @Test
        void anArrowKeyPaintsTheButtonInsteadAndThePressedFaceWhileHeld() {
            final TestHost host = new TestHost(new TestPlatform(), Themes.flat().toBuilder()
                    .paint(ScrollBar.STYLE_ARROW, Paint.solid(0xFF111111))
                    .paint("scroll.arrow.down.pressed", Paint.solid(0xFF222222))
                    .build(), 100, 100);
            final ScrollBar bar = host.factory().scrollBar().setExtent(20, 220).setArrowsShown(true);
            bar.setSize(10, 100);
            host.show(host.factory().absolutePanel().add(bar, 0, 0));
            host.host().pointerButton(5, 95, MouseButton.LEFT, true, Modifiers.NONE);
            final DrawLog log = new DrawLog();
            host.host().render(log);
            assertTrue(log.lines().contains("fill 0,0 10x10 #FF111111"), log.lines().toString());
            assertTrue(log.lines().contains("fill 0,90 10x10 #FF222222"), log.lines().toString());
            assertEquals(Orientation.VERTICAL, bar.orientation());
        }
    }

    private Rect thumbOf(final ScrollBar bar) {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final int thumb = ((Paint.Solid) Themes.flat().paint(ScrollBar.STYLE_THUMB).orElseThrow()).argb();
        final String color = String.format("#%08X", thumb);
        final String line = log.lines().stream().filter(entry -> entry.startsWith("fill") && entry.endsWith(color))
                .findFirst().orElseThrow();
        final String[] parts = line.split(" ");
        final String[] at = parts[1].split(",");
        final String[] size = parts[2].split("x");
        return new Rect(Float.parseFloat(at[0]), Float.parseFloat(at[1]), Float.parseFloat(size[0]),
                Float.parseFloat(size[1]));
    }
}
