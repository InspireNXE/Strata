package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vertical slider, the scroll bar, the indeterminate progress bar and the busy indicator. Text in the test
 * platform is 6 units per code point and 9 units per line.
 */
class RangeControlsTest {
    private final TestHost test = new TestHost(300, 200);
    private final AbsolutePanel panel = this.test.factory().absolutePanel();

    private void show() {
        this.test.show(this.panel);
    }

    private void press(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, true, Modifiers.NONE);
    }

    private void release(final float x, final float y) {
        this.test.host().pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    private boolean wheel(final float x, final float y, final float scrollX, final float scrollY) {
        return this.test.host().scroll(x, y, scrollX, scrollY, ScrollKind.WHEEL, Modifiers.NONE);
    }

    private List<Fill> render() {
        final FillCanvas canvas = new FillCanvas();
        this.test.host().render(canvas);
        return canvas.fills;
    }

    @Test
    void theFlatThemeDefinesEveryNewKey() {
        final List<String> keys = List.of(ScrollBar.STYLE_THUMB_DISABLED, ScrollBar.STYLE_SLIM_TRACK,
                ScrollBar.STYLE_SLIM_THUMB, ScrollBar.STYLE_SLIM_THUMB_HOVERED, ScrollBar.STYLE_SLIM_THUMB_DISABLED,
                ProgressBar.STYLE_SEGMENT, BusyIndicator.STYLE_DOT, BusyIndicator.STYLE_DOT_ACTIVE,
                BusyIndicator.STYLE_TEXT);
        assertEquals(List.of(), List.copyOf(Themes.flat().missing(keys)));
    }

    @Nested
    class VerticalSliders {
        // 150 tall: the thumb's center travels from y = 146 (the minimum) up to y = 4, 142 units for the range
        private final Slider slider = RangeControlsTest.this.test.factory().slider(Orientation.VERTICAL)
                .setRange(0, 142);
        private final List<Double> seen = new ArrayList<>();

        VerticalSliders() {
            this.slider.onEvent(Ranged.CHANGED, event -> this.seen.add(event.value()));
            RangeControlsTest.this.panel.add(this.slider, 0, 0);
            RangeControlsTest.this.show();
        }

        @Test
        void itStandsUpAndKeepsHorizontalAsTheDefault() {
            assertEquals(Orientation.HORIZONTAL, RangeControlsTest.this.test.factory().slider().orientation());
            assertEquals(Orientation.VERTICAL, this.slider.orientation());
            assertEquals(new Rect(0, 0, 20, 150), this.slider.bounds());
        }

        @Test
        void aPressCountsFromTheBottom() {
            RangeControlsTest.this.press(10, 96);
            assertEquals(50d, this.slider.value(), 1e-9);
            RangeControlsTest.this.press(10, 149);
            assertEquals(0d, this.slider.value());
            RangeControlsTest.this.press(10, 1);
            assertEquals(142d, this.slider.value());
        }

        @Test
        void draggingUpRaisesTheValue() {
            RangeControlsTest.this.press(10, 146);
            RangeControlsTest.this.test.host().pointerMoved(10, 120, Modifiers.NONE);
            assertEquals(26d, this.slider.value(), 1e-9);
            RangeControlsTest.this.test.host().pointerMoved(80, 90, Modifiers.NONE);
            assertEquals(56d, this.slider.value(), 1e-9);
            RangeControlsTest.this.release(80, 90);
            assertEquals(2, this.seen.size());
        }

        @Test
        void upAndRightRaiseIt() {
            this.slider.setStep(1).requestFocus();
            RangeControlsTest.this.key(Key.UP);
            RangeControlsTest.this.key(Key.RIGHT);
            assertEquals(2d, this.slider.value());
            RangeControlsTest.this.key(Key.DOWN);
            assertEquals(1d, this.slider.value());
        }

        @Test
        void theThumbSitsAtTheBottomForTheMinimumAndTheTopForTheMaximum() {
            assertTrue(RangeControlsTest.this.render().contains(new Fill(new Rect(0, 142, 20, 8), 0xFF5A5E66)));
            this.slider.setValue(142);
            assertTrue(RangeControlsTest.this.render().contains(new Fill(new Rect(0, 0, 20, 8), 0xFF5A5E66)));
        }
    }

    @Nested
    class ScrollBars {
        // 100 tall, showing 50 of 150: the thumb is a third of the track and travels 200/3 units for the range 0..100
        private final ScrollBar bar = RangeControlsTest.this.test.factory().scrollBar()
                .setExtent(50, 150)
                .setHeight(100);
        private final List<Double> seen = new ArrayList<>();

        ScrollBars() {
            this.bar.onEvent(Ranged.CHANGED, event -> this.seen.add(event.value()));
            RangeControlsTest.this.panel.add(this.bar, 0, 0);
            RangeControlsTest.this.show();
        }

        @Test
        void defaults() {
            final ScrollBar plain = RangeControlsTest.this.test.factory().scrollBar();
            assertEquals(Orientation.VERTICAL, plain.orientation());
            assertEquals(0d, plain.min());
            assertEquals(0d, plain.max());
            assertEquals(0d, plain.visibleAmount());
            assertEquals(10d, plain.scrollRate());
            assertFalse(plain.isSlim());
            assertFalse(plain.isFocusable());
            assertEquals(CursorKind.HAND, plain.cursor());
            assertEquals(Orientation.HORIZONTAL,
                    RangeControlsTest.this.test.factory().scrollBar(Orientation.HORIZONTAL).orientation());
            assertEquals(new Rect(0, 0, 6, 100), this.bar.bounds());
        }

        @Test
        void setExtentSetsTheRangeAndTheVisibleAmount() {
            assertEquals(0d, this.bar.min());
            assertEquals(100d, this.bar.max());
            assertEquals(50d, this.bar.visibleAmount());
            this.bar.setValue(80).setExtent(50, 60);
            assertEquals(10d, this.bar.value());
            this.bar.setExtent(50, 20);
            assertEquals(0d, this.bar.max());
            assertEquals(List.of(80d, 10d, 0d), this.seen);
        }

        @Test
        void theThumbIsSizedByTheVisibleShareAndCachedBetweenFrames() {
            this.bar.setValue(50);
            final List<Fill> first = RangeControlsTest.this.render();
            final Rect thumb = first.get(first.size() - 1).rect();
            assertEquals(0f, thumb.x());
            assertEquals(6f, thumb.width());
            assertEquals(100f / 3f, thumb.height(), 1e-4);
            assertEquals(100f / 3f, thumb.y(), 1e-4);
            final List<Fill> second = RangeControlsTest.this.render();
            assertSame(thumb, second.get(second.size() - 1).rect());
        }

        @Test
        void aPressOnTheTrackPagesTowardThePointer() {
            RangeControlsTest.this.press(3, 80);
            assertEquals(40d, this.bar.value());
            RangeControlsTest.this.press(3, 2);
            assertEquals(0d, this.bar.value());
        }

        @Test
        void draggingTheThumbFollowsThePointer() {
            this.bar.setValue(40);
            // The thumb runs from 80/3 to 140/3; grab it 10/3 below its top
            RangeControlsTest.this.press(3, 30);
            RangeControlsTest.this.test.host().pointerMoved(40, 190f / 3f, Modifiers.NONE);
            assertEquals(90d, this.bar.value(), 1e-3);
            RangeControlsTest.this.release(40, 190f / 3f);
        }

        @Test
        void theWheelMovesItAndPassesOnAtTheEnd() {
            assertTrue(RangeControlsTest.this.wheel(3, 50, 0, -1));
            assertEquals(10d, this.bar.value());
            assertTrue(RangeControlsTest.this.wheel(3, 50, 0, 1));
            assertEquals(0d, this.bar.value());
            assertFalse(RangeControlsTest.this.wheel(3, 50, 0, 1));
        }

        @Test
        void aHorizontalBarTakesVerticalScrolling() {
            this.bar.setOrientation(Orientation.HORIZONTAL).setHeight(Float.NaN);
            RangeControlsTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 100, 6), this.bar.bounds());
            assertTrue(RangeControlsTest.this.wheel(50, 3, 0, -2));
            assertEquals(20d, this.bar.value());
        }

        @Test
        void keysMoveAFocusableBar() {
            this.bar.setFocusable(true).requestFocus();
            RangeControlsTest.this.key(Key.DOWN);
            assertEquals(10d, this.bar.value());
            RangeControlsTest.this.key(Key.PAGE_DOWN);
            assertEquals(50d, this.bar.value());
            RangeControlsTest.this.key(Key.RIGHT);
            assertEquals(50d, this.bar.value());
            RangeControlsTest.this.key(Key.END);
            assertEquals(100d, this.bar.value());
            RangeControlsTest.this.key(Key.HOME);
            assertEquals(0d, this.bar.value());
        }

        @Test
        void requestsCanBeCanceledOrRewritten() {
            this.bar.onEvent(Ranged.CHANGE, event -> {
                if (event.value() > 60d) {
                    event.cancel();
                } else {
                    event.setValue(event.value() + 1d);
                }
            });
            RangeControlsTest.this.press(3, 80);
            assertEquals(41d, this.bar.value());
            RangeControlsTest.this.press(3, 80);
            assertEquals(41d, this.bar.value());
            this.bar.setValue(90);
            assertEquals(90d, this.bar.value());
        }

        @Test
        void aBarWithNothingToScrollIgnoresInput() {
            this.bar.setExtent(100, 80);
            RangeControlsTest.this.press(3, 80);
            assertFalse(RangeControlsTest.this.wheel(3, 50, 0, -1));
            assertEquals(0d, this.bar.value());
            final List<Fill> fills = RangeControlsTest.this.render();
            assertEquals(new Fill(new Rect(0, 0, 6, 100), 0xFF3A3A3A), fills.get(fills.size() - 1));
        }

        @Test
        void aSlimBarIsThinWithAShorterThumb() {
            this.bar.setSlim(true).setExtent(10, 1000).setHeight(Float.NaN);
            RangeControlsTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 2, 100), this.bar.bounds());
            final List<Fill> fills = RangeControlsTest.this.render();
            assertEquals(new Fill(new Rect(0, 0, 2, 16), 0xFF8A8F98), fills.get(fills.size() - 1));
        }

        @Test
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> this.bar.setRange(2, 1));
            assertThrows(IllegalArgumentException.class, () -> this.bar.setValue(Double.NaN));
            assertThrows(IllegalArgumentException.class, () -> this.bar.setVisibleAmount(-1));
            assertThrows(IllegalArgumentException.class, () -> this.bar.setExtent(Double.POSITIVE_INFINITY, 1));
            assertThrows(IllegalArgumentException.class, () -> this.bar.setScrollRate(0));
            this.bar.setRange(5, 5);
            assertEquals(5d, this.bar.value());
        }
    }

    @Nested
    class IndeterminateProgress {
        private final ProgressBar bar = RangeControlsTest.this.test.factory().progressBar()
                .setValue(0.5)
                .setWidth(100);

        IndeterminateProgress() {
            RangeControlsTest.this.panel.add(this.bar, 0, 0);
            RangeControlsTest.this.show();
        }

        @Test
        void aSegmentCrossesTheBarByTheClock() {
            assertFalse(this.bar.isIndeterminate());
            this.bar.setIndeterminate(true);
            // A 25-unit segment over 125 whole-unit steps in 1500 ms; step 0 shows nothing
            assertEquals(1, RangeControlsTest.this.render().size());
            RangeControlsTest.this.test.platform().advanceMillis(600);
            final List<Fill> fills = RangeControlsTest.this.render();
            assertEquals(new Fill(new Rect(25, 0, 25, 13), 0xFF43A047), fills.get(1));
            assertSame(fills.get(1).rect(), RangeControlsTest.this.render().get(1).rect());
            RangeControlsTest.this.test.platform().advanceMillis(1500);
            assertSame(fills.get(1).rect(), RangeControlsTest.this.render().get(1).rect());
            RangeControlsTest.this.test.platform().advanceMillis(888);
            assertEquals(new Rect(99, 0, 1, 13), RangeControlsTest.this.render().get(1).rect());
        }

        @Test
        void theValueIsKeptAndShowsAgain() {
            this.bar.setIndeterminate(true).setIndeterminate(false);
            assertEquals(0.5d, this.bar.value());
            assertEquals(new Fill(new Rect(0, 0, 50, 13), 0xFF2E7D32), RangeControlsTest.this.render().get(1));
        }
    }

    @Nested
    class BusyIndicators {
        private final BusyIndicator busy = RangeControlsTest.this.test.factory().busyIndicator("Wait");

        BusyIndicators() {
            RangeControlsTest.this.panel.add(this.busy, 0, 0);
            RangeControlsTest.this.show();
        }

        private int lit() {
            final List<Fill> fills = RangeControlsTest.this.render();
            int lit = -1;
            for (int i = 0; i < 3; i++) {
                if (fills.get(fills.size() - 3 + i).argb() == 0xFFE0E0E0) {
                    lit = i;
                }
            }
            return lit;
        }

        @Test
        void itMeasuresTheCaptionAndTheDots() {
            assertEquals("", RangeControlsTest.this.test.factory().busyIndicator().text());
            assertEquals(new Rect(0, 0, 24, 17), this.busy.bounds());
            this.busy.setText("");
            RangeControlsTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 20, 4), this.busy.bounds());
            final List<Fill> fills = RangeControlsTest.this.render();
            assertEquals(List.of(new Rect(0, 0, 4, 4), new Rect(8, 0, 4, 4), new Rect(16, 0, 4, 4)),
                    fills.stream().map(Fill::rect).toList());
        }

        @Test
        void theLitDotBouncesByTheClock() {
            final List<Integer> seen = new ArrayList<>();
            for (int i = 0; i < 6; i++) {
                seen.add(this.lit());
                RangeControlsTest.this.test.platform().advanceMillis(BusyIndicator.FRAME_MILLIS);
            }
            assertEquals(List.of(0, 1, 2, 1, 0, 1), seen);
        }

        @Test
        void itDrawsTheCaptionAboveTheDots() {
            final FillCanvas canvas = new FillCanvas();
            RangeControlsTest.this.test.host().render(canvas);
            assertEquals(List.of("Wait"), canvas.texts);
            assertEquals(new Rect(2, 13, 4, 4), canvas.fills.get(0).rect());
        }
    }

    /**
     * A fill and its color.
     */
    record Fill(Rect rect, int argb) {
    }

    /**
     * Records fills with their colors, and the text drawn.
     */
    static final class FillCanvas implements Canvas {
        final List<Fill> fills = new ArrayList<>();
        final List<String> texts = new ArrayList<>();

        @Override
        public void fill(final Rect rect, final int argb) {
            this.fills.add(new Fill(rect, argb));
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.texts.add(span.text());
        }

        @Override
        public void pushClip(final Rect rect) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }
}
