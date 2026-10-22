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
import org.inspirenxe.strata.api.control.button.ToggleSwitch;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.TextArea;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.NavigationView;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The motion of the built-in controls (design 4.51): off by default, and driven by the test platform's clock once a
 * theme sets a duration.
 */
class BuiltInMotionTest {
    private static final int KNOB = 0xFF00FF00;
    private static final int FILL = 0xFF0000FF;

    private TestPlatform platform;
    private TestHost test;
    private ComponentFactory ui;

    private void host(final Theme theme) {
        this.platform = new TestPlatform();
        this.test = new TestHost(this.platform, theme, 400, 300);
        this.ui = this.test.factory();
    }

    private static Theme with(final String key, final float millis) {
        return Themes.flat().toBuilder().metric(key, millis)
                .paint(ToggleSwitch.STYLE_KNOB, Paint.solid(KNOB))
                .paint(ToggleSwitch.STYLE_KNOB_HOVERED, Paint.solid(KNOB))
                .paint(ProgressBar.STYLE_FILL, Paint.solid(FILL))
                .build();
    }

    /**
     * Moves the clock and draws a frame, returning what it drew.
     */
    private DrawLog frame(final long millis) {
        this.platform.advanceMillis(millis);
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log;
    }

    private static String fillOf(final DrawLog log, final int color) {
        final String suffix = String.format(" #%08X", color);
        return log.lines("fill").stream().filter(line -> line.endsWith(suffix)).findFirst().orElseThrow();
    }

    private static float x(final String fill) {
        return Float.parseFloat(fill.split(" ")[1].split(",")[0]);
    }

    private static float width(final String fill) {
        return Float.parseFloat(fill.split(" ")[2].split("x")[0]);
    }

    @Nested
    class Toggles {
        @Test
        void theKnobJumpsByDefault() {
            BuiltInMotionTest.this.host(with("unused", 0f));
            final ToggleSwitch toggle = BuiltInMotionTest.this.ui.toggleSwitch(Text.of("Music"));
            BuiltInMotionTest.this.test.show(toggle);
            final float off = x(fillOf(BuiltInMotionTest.this.frame(0L), KNOB));
            toggle.setSelected(true);
            assertTrue(x(fillOf(BuiltInMotionTest.this.frame(0L), KNOB)) > off);
        }

        @Test
        void theKnobSlidesOverTheThemesTime() {
            BuiltInMotionTest.this.host(with(ToggleSwitch.STYLE_ANIMATION, 100f));
            final ToggleSwitch toggle = BuiltInMotionTest.this.ui.toggleSwitch(Text.of("Music"));
            BuiltInMotionTest.this.test.show(toggle);
            final float off = x(fillOf(BuiltInMotionTest.this.frame(0L), KNOB));
            toggle.setSelected(true);
            assertTrue(toggle.isSelected(), "the state changes at once");
            final float start = x(fillOf(BuiltInMotionTest.this.frame(0L), KNOB));
            final float middle = x(fillOf(BuiltInMotionTest.this.frame(50L), KNOB));
            final float end = x(fillOf(BuiltInMotionTest.this.frame(50L), KNOB));
            assertEquals(off, start);
            assertTrue(middle > off && middle < end, off + " < " + middle + " < " + end);
            assertEquals(end, x(fillOf(BuiltInMotionTest.this.frame(50L), KNOB)), "at rest");
        }

        @Test
        void aDetachedSwitchJumps() {
            BuiltInMotionTest.this.host(with(ToggleSwitch.STYLE_ANIMATION, 100f));
            final ToggleSwitch toggle = BuiltInMotionTest.this.ui.toggleSwitch(Text.of("Music")).setSelected(true);
            final ToggleSwitch reference = BuiltInMotionTest.this.ui.toggleSwitch(Text.of("Music"));
            final StackPanel panel = BuiltInMotionTest.this.ui.stackPanel();
            panel.add(toggle);
            BuiltInMotionTest.this.test.show(panel);
            final DrawLog log = BuiltInMotionTest.this.frame(0L);
            assertTrue(x(fillOf(log, KNOB)) > toggle.bounds().x() + 10f, "drawn at the right end at once");
            assertFalse(reference.isSelected());
        }
    }

    @Nested
    class ProgressBars {
        @Test
        void theFillEasesAndTheValueChangesAtOnce() {
            BuiltInMotionTest.this.host(with(ProgressBar.STYLE_ANIMATION, 200f));
            final ProgressBar bar = BuiltInMotionTest.this.ui.progressBar();
            bar.setWidth(100f);
            final List<Double> changes = new ArrayList<>();
            bar.onEvent(Ranged.CHANGED, event -> changes.add(event.value()));
            BuiltInMotionTest.this.test.show(bar);
            BuiltInMotionTest.this.frame(0L);
            bar.setValue(1.0);
            assertEquals(1.0, bar.value());
            assertEquals(List.of(1.0), changes);
            assertTrue(BuiltInMotionTest.this.frame(0L).lines("fill").stream()
                    .noneMatch(line -> line.endsWith(String.format(" #%08X", FILL))), "empty on the first step");
            final float middle = width(fillOf(BuiltInMotionTest.this.frame(100L), FILL));
            assertTrue(middle > 0f && middle < 100f, "part way: " + middle);
            assertEquals(100f, width(fillOf(BuiltInMotionTest.this.frame(100L), FILL)));
        }
    }

    @Nested
    class Expanders {
        private Expander expander;
        private Label content;

        private void show(final float millis) {
            BuiltInMotionTest.this.host(with(Expander.STYLE_ANIMATION, millis));
            this.content = BuiltInMotionTest.this.ui.label(Text.of("Inside"));
            this.content.setHeight(40f);
            this.expander = BuiltInMotionTest.this.ui.expander(Text.of("More")).setContent(this.content);
            final StackPanel panel = BuiltInMotionTest.this.ui.stackPanel();
            panel.add(this.expander);
            BuiltInMotionTest.this.test.show(panel);
            BuiltInMotionTest.this.frame(0L);
        }

        @Test
        void theContentAppearsAtOnceByDefault() {
            this.show(0f);
            final float header = this.expander.bounds().height();
            this.expander.setExpanded(true);
            BuiltInMotionTest.this.frame(0L);
            assertEquals(header + 40f, this.expander.bounds().height());
            this.expander.setExpanded(false);
            assertFalse(this.content.isAttached());
        }

        @Test
        void theContentGrowsAndShrinksOverTheThemesTime() {
            this.show(100f);
            final float header = this.expander.bounds().height();
            this.expander.setExpanded(true);
            assertTrue(this.content.isAttached(), "a child at once");
            BuiltInMotionTest.this.frame(0L);
            assertEquals(header, this.expander.bounds().height());
            BuiltInMotionTest.this.frame(50L);
            final float middle = this.expander.bounds().height();
            assertTrue(middle > header && middle < header + 40f, "part way: " + middle);
            assertEquals(40f, this.content.bounds().height(), "the content keeps its size and is cut");
            BuiltInMotionTest.this.frame(50L);
            assertEquals(header + 40f, this.expander.bounds().height());

            this.expander.setExpanded(false);
            assertFalse(this.expander.isExpanded());
            BuiltInMotionTest.this.frame(50L);
            assertTrue(this.content.isAttached(), "still a child while it shrinks");
            BuiltInMotionTest.this.frame(50L);
            assertFalse(this.content.isAttached(), "gone once it shrank away");
            assertEquals(header, this.expander.bounds().height());
        }
    }

    @Nested
    class Scrolling {
        @Test
        void aScrollPanelGlidesForTheWheelAndJumpsForCode() {
            BuiltInMotionTest.this.host(with(ScrollPanel.STYLE_SMOOTH, 100f));
            final ScrollPanel panel = BuiltInMotionTest.this.ui.scrollPanel();
            panel.setHeight(50f);
            final Label tall = BuiltInMotionTest.this.ui.label(Text.of("Tall"));
            tall.setHeight(500f);
            panel.add(tall);
            BuiltInMotionTest.this.test.show(panel);
            BuiltInMotionTest.this.frame(0L);
            assertTrue(BuiltInMotionTest.this.test.host().scroll(5, 5, 0f, -1f, ScrollKind.WHEEL, Modifiers.NONE));
            assertTrue(BuiltInMotionTest.this.test.host().scroll(5, 5, 0f, -1f, ScrollKind.WHEEL, Modifiers.NONE));
            assertEquals(0f, panel.scrollY(), "nothing moves before the next frame");
            BuiltInMotionTest.this.frame(50L);
            assertTrue(panel.scrollY() > 0f && panel.scrollY() < 20f, "part way: " + panel.scrollY());
            BuiltInMotionTest.this.frame(50L);
            assertEquals(20f, panel.scrollY(), "both notches add up");
            panel.setScroll(0f, 100f);
            assertEquals(100f, panel.scrollY());
        }

        @Test
        void aListGlidesToTheRowCodeNamed() {
            BuiltInMotionTest.this.host(with(ScrollPanel.STYLE_SMOOTH, 100f));
            final ListView<String> list = BuiltInMotionTest.this.ui.<String>listView()
                    .setItems(IntStream.range(0, 100).mapToObj(i -> "Row " + i).toList());
            list.setHeight(60f);
            BuiltInMotionTest.this.test.show(list);
            BuiltInMotionTest.this.frame(0L);
            list.scrollToIndex(50);
            BuiltInMotionTest.this.frame(0L);
            assertEquals(0f, list.scrollY(), "the glide starts from where it was");
            BuiltInMotionTest.this.frame(50L);
            final float middle = list.scrollY();
            BuiltInMotionTest.this.frame(60L);
            final float end = list.scrollY();
            assertTrue(middle > 0f && middle < end, "part way: " + middle + " of " + end);
            list.setScrollY(0f);
            assertEquals(0f, list.scrollY(), "code moves at once");
        }

        @Test
        void aTextAreaGlidesForTheWheel() {
            BuiltInMotionTest.this.host(with(ScrollPanel.STYLE_SMOOTH, 100f));
            final TextArea area = BuiltInMotionTest.this.ui.textArea();
            area.setText(String.join("\n", IntStream.range(0, 50).mapToObj(i -> "Line " + i).toList()));
            area.setHeight(60f);
            area.setWidth(100f);
            BuiltInMotionTest.this.test.show(area);
            BuiltInMotionTest.this.frame(0L);
            // Whichever way the area has room to scroll
            final float start = area.scrollY();
            final float notch = start > 0f ? 1f : -1f;
            assertTrue(BuiltInMotionTest.this.test.host().scroll(20, 20, 0f, notch, ScrollKind.WHEEL, Modifiers.NONE));
            assertEquals(start, area.scrollY(), "nothing moves before the next frame");
            BuiltInMotionTest.this.frame(50L);
            final float middle = area.scrollY();
            BuiltInMotionTest.this.frame(60L);
            final float end = area.scrollY();
            assertTrue(Math.abs(middle - start) > 0f && Math.abs(middle - start) < Math.abs(end - start),
                    "part way: " + middle + " between " + start + " and " + end);
        }
    }

    @Nested
    class NavigationPanes {
        @Test
        void thePaneSlidesInFromTheLeft() {
            BuiltInMotionTest.this.host(with(NavigationView.STYLE_ANIMATION, 100f));
            final NavigationView view = BuiltInMotionTest.this.ui.navigationView()
                    .addItem(BuiltInMotionTest.this.ui.navigationItem("General"))
                    .setDisplayMode(NavigationView.DisplayMode.COMPACT);
            BuiltInMotionTest.this.test.show(view);
            BuiltInMotionTest.this.frame(0L);
            view.setExpanded(true);
            assertTrue(view.isExpanded());
            final var pane = BuiltInMotionTest.this.ui.overlays().open().getFirst().content();
            BuiltInMotionTest.this.frame(0L);
            final float width = pane.bounds().width();
            assertEquals(-width, pane.bounds().x(), "starts a pane's width to the left");
            BuiltInMotionTest.this.frame(50L);
            assertTrue(pane.bounds().x() > -width && pane.bounds().x() < 0f, "part way: " + pane.bounds().x());
            BuiltInMotionTest.this.frame(50L);
            assertEquals(0f, pane.bounds().x());
        }

        @Test
        void thePaneClosingMidSlideComesBackAtRest() {
            BuiltInMotionTest.this.host(with(NavigationView.STYLE_ANIMATION, 100f));
            final NavigationView view = BuiltInMotionTest.this.ui.navigationView()
                    .addItem(BuiltInMotionTest.this.ui.navigationItem("General"))
                    .setDisplayMode(NavigationView.DisplayMode.COMPACT);
            BuiltInMotionTest.this.test.show(view);
            BuiltInMotionTest.this.frame(0L);
            view.setExpanded(true);
            final var pane = BuiltInMotionTest.this.ui.overlays().open().getFirst().content();
            BuiltInMotionTest.this.frame(50L);
            view.setExpanded(false);
            BuiltInMotionTest.this.frame(100L);
            assertEquals(view.bounds().x(), pane.bounds().x(), "back in the view, with no offset left");
        }
    }
}
