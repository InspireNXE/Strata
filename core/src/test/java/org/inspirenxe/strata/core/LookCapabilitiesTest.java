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
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.control.Caption;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.Conventions;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The capabilities that let a theme or a platform match an engine's own controls: metrics, caption cutting and
 * scrolling, centering on whole units, tooltip placement, caret and selection looks, the press convention, and the
 * list, slider, tab, message box, separator and busy indicator keys.
 */
class LookCapabilitiesTest {
    private static final ThemeId MOD = ThemeId.of("mod", "look");

    private static Theme flatWith(final Consumer<Theme.Builder> changes) {
        final Theme.Builder builder = Themes.flat().toBuilder();
        changes.accept(builder);
        return builder.build();
    }

    private static DrawLog render(final TestHost test) {
        final DrawLog canvas = new DrawLog();
        test.host().render(canvas);
        return canvas;
    }

    /**
     * Returns where each text with this content was drawn, as {@code x,y}.
     */
    private static List<String> textAt(final DrawLog log, final String text) {
        final String prefix = "text " + text + " ";
        return log.lines(prefix).stream().map(line -> line.substring(prefix.length()).split(" ")[0]).toList();
    }

    @Nested
    class Metrics {
        @Test
        void aThemeHoldsMetricsBesideItsOtherValues() {
            final Theme theme = Theme.builder("t").metric("a.delay", 250f).build();
            assertEquals(250f, theme.metricOr("a.delay", 0f));
            assertEquals(OptionalDouble.of(250), theme.metric("a.delay"));
            assertEquals(OptionalDouble.empty(), theme.metric("b"));
            assertTrue(theme.has("a.delay"));
            assertEquals(Map.of("a.delay", 250f), theme.toBuilder().build().metrics());
            assertThrows(IllegalArgumentException.class, () -> Theme.builder("t").metric("x", Float.NaN));
        }

        @Test
        void filesOverridesAndRefsSetMetrics() {
            final Theme theme = resolve("{ \"parent\": \"strata:flat\", \"metrics\": { \"a\": 2, \"b\": 0.5 } }",
                    ThemeRef.of(MOD).override("b", 9f));
            assertEquals(2f, theme.metricOr("a", 0f));
            // The ref's override wins over the file
            assertEquals(9f, theme.metricOr("b", 0f));
            assertEquals(Optional.of(ThemeResolver.REF_LAYER), theme.layer("b"));
            assertEquals(Map.of("c", 1f), ThemeOverrides.of().with("c", 1f).metrics());
            assertEquals(4f, ThemeRef.of(MOD).override(ThemeOverrides.of().with("d", 4f)).metrics().get("d"));
        }

        @Test
        void aMetricMustBeANumber() {
            // The only file is skipped, so the id has nothing to resolve
            assertThrows(ThemeException.class, () -> resolve("{ \"metrics\": { \"a\": \"fast\" } }", ThemeRef.of(MOD)));
        }

        private static Theme resolve(final String file, final ThemeRef ref) {
            return new ThemeResolver(new ThemeSource() {
                @Override
                public Optional<Theme> base(final ThemeId id) {
                    return Optional.empty();
                }

                @Override
                public List<ThemeDocument> documents(final ThemeId id) {
                    return id.equals(MOD) ? List.of(new ThemeDocument("test", file)) : List.of();
                }

                @Override
                public long version() {
                    return 0;
                }
            }).resolve(ref);
        }
    }

    @Nested
    class Centering {
        @Test
        void wholeSpacesLeaveTheSpareUnitOnTheRight() {
            assertEquals(5f, Caption.center(0f, 21f, 10f));
            assertEquals(5f, Caption.center(0f, 20f, 11f));
            assertEquals(5f, Caption.center(0f, 21f, 11f));
            assertEquals(1.25f, Caption.center(0f, 2.5f, 0f));
        }
    }

    @Nested
    class Captions {
        /**
         * Shows a button 60 wide in a theme and returns where its caption was drawn. The flat padding is 8 at the
         * sides, and test text is 6 units a character.
         */
        private String captionAt(final Theme theme, final String caption, final long millis) {
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final Button button = test.factory().button(caption).setWidth(60f);
            test.show(test.factory().absolutePanel().add(button, 0, 0));
            test.platform().advanceMillis(millis);
            return textAt(render(test), caption).getFirst().split(",")[0];
        }

        @Test
        void aCaptionThatFitsIsCentered() {
            assertEquals("24", this.captionAt(Themes.flat(), "ab", 0L));
        }

        @Test
        void aCaptionTooWideStartsAtTheClipArea() {
            assertEquals("8", this.captionAt(Themes.flat(), "abcdefghijklmn", 0L));
            final Theme clipped = flatWith(builder -> builder.insets(Button.STYLE_CLIP, new Insets(2f, 0f, 2f, 0f)));
            assertEquals("2", this.captionAt(clipped, "abcdefghijklmn", 0L));
        }

        @Test
        void aScrollingCaptionSweepsBetweenItsEnds() {
            final Theme scrolling = flatWith(builder -> builder.metric(Button.STYLE_TEXT_SCROLL, 500f)
                    .metric(Button.STYLE_TEXT_SCROLL_MINIMUM, 3000f));
            // 84 wide in 44: 40 over, so one sweep takes 20 seconds and starts at the end
            assertEquals("-32", this.captionAt(scrolling, "abcdefghijklmn", 0L));
            assertEquals("8", this.captionAt(scrolling, "abcdefghijklmn", 10_000L));
        }
    }

    @Nested
    class Tooltips {
        @Test
        void theDelayComesFromTheThemeUntilTheAdapterSetsOne() {
            final Theme theme = flatWith(builder -> builder.metric(Tooltip.STYLE_DELAY, 250f));
            final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(new TestPlatform(), theme);
            host.open(ui -> ui.absolutePanel());
            assertEquals(250L, host.root().tooltipDelay());
            host.setTooltipDelay(40L);
            assertEquals(40L, host.root().tooltipDelay());
            final ScreenHostImpl flat = (ScreenHostImpl) StrataCore.createScreenHost(new TestPlatform(), Themes.flat());
            flat.open(ui -> ui.absolutePanel());
            assertEquals(Tooltip.DEFAULT_DELAY_MILLIS, flat.root().tooltipDelay());
        }

        @Test
        void theBoxSitsAtTheThemesOffsetFromTheWholeUnitPointer() {
            final Theme theme = flatWith(builder -> builder.metric(Tooltip.STYLE_OFFSET_X, 9f)
                    .metric(Tooltip.STYLE_OFFSET_Y, -15f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final Button button = test.factory().button("Go").setWidth(100f);
            button.setTooltip(Tooltip.text("tip"));
            test.show(test.factory().absolutePanel().add(button, 0, 20));
            test.host().pointerMoved(20.6f, 30.4f, Modifiers.NONE);
            render(test);
            // The box's corner is at (29, 15), and the text 4 units inside it
            assertEquals(List.of("33,19"), textAt(render(test), "tip"));
        }

        @Test
        void theBoxCanSitBelowItsComponent() {
            final Theme theme = flatWith(builder -> builder.metric(Tooltip.STYLE_BELOW, 1f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 200);
            final Button button = test.factory().button("Go").setWidth(100f);
            button.setTooltip(Tooltip.text("tip"));
            test.show(test.factory().absolutePanel().add(button, 0, 20));
            test.host().pointerMoved(20f, 25f, Modifiers.NONE);
            render(test);
            // 8 below the top of a 20-tall button: the box's top is 25 + round(17 - 0.4 * 12), text 4 inside
            assertEquals(List.of("36,41"), textAt(render(test), "tip"));
        }

        @Test
        void aFrameAndOutsetDrawPastTheBox() {
            final Theme theme = flatWith(builder -> builder.paint(Tooltip.STYLE_BACKGROUND, Paint.solid(0xFF101010))
                    .paint(Tooltip.STYLE_FRAME, Paint.solid(0xFF5000FF)).insets(Tooltip.STYLE_OUTSET, Insets.of(9f)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final Button button = test.factory().button("Go").setWidth(100f);
            button.setTooltip(Tooltip.text("tip"));
            test.show(test.factory().absolutePanel().add(button, 0, 20));
            test.host().pointerMoved(20f, 30f, Modifiers.NONE);
            render(test);
            final List<String> fills = render(test).lines("fill ");
            // The box is 18 + 8 wide and 9 + 8 tall at (32, 18), grown by 9 on every side
            assertTrue(fills.contains("fill 23,9 44x35 #FF101010"), fills.toString());
            assertTrue(fills.contains("fill 23,9 44x35 #FF5000FF"), fills.toString());
        }
    }

    @Nested
    class Activation {
        private final List<String> heard = new ArrayList<>();

        private TestHost host(final boolean onPress) {
            final TestPlatform platform = new TestPlatform();
            platform.setConventions(new Conventions(onPress));
            return new TestHost(platform, Themes.flat(), 300, 100);
        }

        @Test
        void buttonsActOnReleaseByDefault() {
            final TestHost test = this.host(false);
            final Button button = test.factory().button("Go").onEvent(Button.PRESSED, event -> this.heard.add("go"));
            test.show(test.factory().absolutePanel().add(button, 0, 0));
            test.host().pointerButton(5, 5, MouseButton.LEFT, true, Modifiers.NONE);
            assertEquals(List.of(), this.heard);
            test.host().pointerButton(5, 5, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(List.of("go"), this.heard);
        }

        @Test
        void aPlatformCanMakeThemActOnPress() {
            final TestHost test = this.host(true);
            final Button button = test.factory().button("Go").onEvent(Button.PRESSED, event -> this.heard.add("go"));
            final CheckBox box = test.factory().checkBox("Box").onEvent(Toggle.TOGGLED, event -> this.heard.add("box"));
            test.show(test.factory().absolutePanel().add(button, 0, 0).add(box, 0, 50));
            test.host().pointerButton(5, 5, MouseButton.LEFT, true, Modifiers.NONE);
            assertEquals(List.of("go"), this.heard);
            test.host().pointerButton(5, 5, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(List.of("go"), this.heard);
            test.host().pointerButton(5, 55, MouseButton.LEFT, true, Modifiers.NONE);
            assertEquals(List.of("go", "box"), this.heard);
            test.host().pointerButton(5, 55, MouseButton.LEFT, false, Modifiers.NONE);
            assertTrue(box.isSelected());
        }
    }

    @Nested
    class Heights {
        @Test
        void aButtonMeasuresTwentyTallButTakesLess() {
            final TestHost test = new TestHost(300, 100);
            final Button measured = test.factory().button("Go");
            final Button small = test.factory().button("Go").setHeight(16f);
            final TextBox field = test.factory().textBox().setHeight(14f);
            test.show(test.factory().absolutePanel().add(measured, 0, 0).add(small, 0, 30).add(field, 0, 60));
            assertEquals(20f, measured.bounds().height());
            assertEquals(16f, small.bounds().height());
            assertEquals(14f, field.bounds().height());
        }
    }

    @Nested
    class TextFields {
        private static final TextStyle SELECTED = TextStyle.DEFAULT.withColor(0xFF1F1FFF);

        private final Theme theme = flatWith(builder -> builder
                .metric(TextBox.STYLE_CARET_UNDERSCORE, 1f)
                .metric(TextBox.STYLE_CARET_EXTEND, 1f)
                .metric(TextBox.STYLE_SELECTION_EXTEND, 1f)
                .metric(TextBox.STYLE_PLACEHOLDER_HIDE, 1f)
                .textStyle(TextBox.STYLE_TEXT_SELECTED, SELECTED));
        private TestHost test;
        private TextBox box;
        private Button other;

        private void show(final Theme theme) {
            this.test = new TestHost(new TestPlatform(), theme, 300, 100);
            this.box = this.test.factory().textBox();
            this.other = this.test.factory().button("Other");
            this.test.show(this.test.factory().absolutePanel().add(this.box, 0, 0).add(this.other, 0, 40));
        }

        @Test
        void aCaretAtTheEndIsAnUnderscoreAfterTheText() {
            this.show(this.theme);
            this.box.setText("ab");
            this.box.requestFocus();
            // The text starts 4 in and is 12 wide; the line is half a unit down in the 10 the padding leaves
            assertEquals(List.of("17,5.5"), textAt(render(this.test), "_"));
        }

        @Test
        void aCaretInsideTheTextIsABarReachingPastTheLine() {
            this.show(this.theme);
            this.box.requestFocus();
            this.box.setText("ab").select(1, 1);
            final List<String> fills = render(this.test).lines("fill 10,4.5 1x11 ");
            assertEquals(1, fills.size());
        }

        @Test
        void theFlatThemeKeepsTheBar() {
            this.show(Themes.flat());
            this.box.setText("ab");
            this.box.requestFocus();
            final DrawLog log = render(this.test);
            assertEquals(List.of(), textAt(log, "_"));
            assertEquals(1, log.lines("fill 16,5.5 1x9 ").size());
        }

        @Test
        void thePlaceholderHidesWhileFocused() {
            this.show(this.theme);
            this.box.setPlaceholder("Search");
            assertEquals(1, textAt(render(this.test), "Search").size());
            this.box.requestFocus();
            assertEquals(0, textAt(render(this.test), "Search").size());
        }

        @Test
        void selectedTextIsDrawnInTheSelectedStyle() {
            this.show(this.theme);
            this.box.requestFocus();
            this.box.setText("abcd").select(1, 3);
            final DrawLog log = render(this.test);
            assertTrue(log.textStyles().contains(SELECTED), log.textStyles().toString());
            // The selection reaches a unit above and below the line
            assertEquals(1, log.lines("fill 10,4.5 12x11 ").size(), log.lines().toString());
        }
    }

    @Nested
    class Sliders {
        @Test
        void anArrowCanMoveTheThumbAUnit() {
            final Theme theme = flatWith(builder -> builder.metric(Slider.STYLE_KEY_TRAVEL, 1f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final Slider slider = test.factory().slider().setWidth(58f);
            test.show(test.factory().absolutePanel().add(slider, 0, 0));
            // 50 units of travel over a range of 1
            assertEquals(0.02d, slider.keyStep(), 1e-9);
            final TestHost flat = new TestHost(300, 100);
            final Slider plain = flat.factory().slider().setWidth(58f);
            flat.show(flat.factory().absolutePanel().add(plain, 0, 0));
            assertEquals(0.01d, plain.keyStep(), 1e-9);
        }
    }

    @Nested
    class Lists {
        @Test
        void rowsCanBeACenteredColumn() {
            final Theme theme = flatWith(builder -> builder.metric(ListView.STYLE_ROW_WIDTH, 220f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 200);
            final List<Label> cells = new ArrayList<>();
            final ComponentFactory ui = test.factory();
            final ListView<String> list = ui.<String>listView(item -> {
                final Label label = ui.cellLabel(item);
                cells.add(label);
                return label;
            }).setItems(List.of("a", "b"));
            test.show(ui.absolutePanel().add(list.setSize(300f, 100f), 0, 0));
            final Rect first = cells.getFirst().bounds();
            // The column starts at 40; the default cell sits 4 in from each side of its row
            assertEquals(44f, first.x());
        }

        @Test
        void aCellWiderThanTheColumnIsClippedToIt() {
            final Theme theme = flatWith(builder -> builder.metric(ListView.STYLE_ROW_WIDTH, 220f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 200);
            final ComponentFactory ui = test.factory();
            final ListView<String> list = ui.<String>listView(item -> ui.label(item).setAnchor(Anchor.MIDDLE_LEFT))
                    .setItems(List.of("a".repeat(80)));
            test.show(ui.absolutePanel().add(list.setSize(300f, 100f), 0, 0));
            final List<String> clips = new ArrayList<>();
            test.host().render(new DrawLog() {
                @Override
                public void pushClip(final Rect rect) {
                    clips.add(DrawLog.rect(rect));
                }
            });
            // The rows are clipped to the 220-unit column, not the whole list, so the text stays off the bar
            assertTrue(clips.contains("40,0 220x100"), clips.toString());
        }

        @Test
        void edgesDrawOutsideAStandaloneList() {
            final Theme theme = flatWith(builder -> builder.paint(ListView.STYLE_EDGE, Paint.solid(0xBF000000))
                    .paint(ListView.STYLE_EDGE_OUTER, Paint.solid(0x33FFFFFF)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 200);
            final ListView<String> list = test.factory().<String>listView().setItems(List.of("a"));
            test.show(test.factory().absolutePanel().add(list.setSize(100f, 50f), 0, 10));
            final List<String> fills = render(test).lines("fill ");
            assertTrue(fills.contains("fill 0,9 100x1 #BF000000"), fills.toString());
            assertTrue(fills.contains("fill 0,8 100x1 #33FFFFFF"), fills.toString());
            assertTrue(fills.contains("fill 0,60 100x1 #BF000000"), fills.toString());
            assertTrue(fills.contains("fill 0,61 100x1 #33FFFFFF"), fills.toString());
        }
    }

    @Nested
    class Tabs {
        @Test
        void theSelectedTabCanHaveAnUnderline() {
            final Theme theme = flatWith(builder -> builder.paint(TabBar.STYLE_UNDERLINE, Paint.solid(0xFFFFFFFF)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final TabBar tabs = test.factory().tabBar().addTab("Buy").addTab("Sell");
            test.show(test.factory().absolutePanel().add(tabs.setSize(100f, 24f), 0, 0));
            final List<String> lines = render(test).lines("fill ");
            // Two tabs 50 wide; "Buy" is 18 wide, centered in the first, 2 above its bottom
            assertTrue(lines.contains("fill 16,22 18x1 #FFFFFFFF"), lines.toString());
        }
    }

    @Nested
    class TabWidths {
        @Test
        void aWholeBarGivesWholeTabs() {
            final TestHost test = new TestHost(300, 100);
            final TabBar tabs = test.factory().tabBar().addTab("A").addTab("B").addTab("C");
            test.show(test.factory().absolutePanel().add(tabs.setSize(100f, 24f), 0, 0));
            final List<String> faces = render(test).lines("fill ").stream()
                    .filter(line -> line.contains("x24 ")).map(line -> line.split(" ")[1]).toList();
            // 100 over three: 34, 33 and 33
            assertEquals(List.of("0,0", "34,0", "67,0"), faces);
        }
    }

    @Nested
    class ListPadding {
        @Test
        void rowsStartBelowThePadding() {
            final Theme theme = flatWith(builder -> builder.insets(ListView.STYLE_PADDING, new Insets(0f, 2f, 0f, 2f)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 200);
            final List<Label> cells = new ArrayList<>();
            final ComponentFactory ui = test.factory();
            final ListView<String> list = ui.<String>listView(item -> {
                final Label label = ui.cellLabel(item);
                cells.add(label);
                return label;
            }).setItems(List.of("a", "b"));
            test.show(ui.absolutePanel().add(list.setSize(100f, 100f), 0, 0));
            // The row starts 2 down; the 9-tall label is centered in its 20-tall row
            assertEquals(7.5f, cells.getFirst().bounds().y());
        }
    }

    @Nested
    class Toasts {
        @Test
        void theFirstLineCanBeATitle() {
            final TextStyle title = TextStyle.DEFAULT.withColor(0xFFFFFF00);
            final Theme theme = flatWith(builder -> builder.textStyle("toast.title", title)
                    .metric("toast.line.gap", 3f).metric("toast.width.minimum", 160f).metric("toast.margin", 0f)
                    .insets("toast.padding", new Insets(18f, 7f, 12f, 4f)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 200);
            test.show(test.factory().absolutePanel());
            test.factory().overlays().toast("Saved\nPosted.");
            test.host().layout();
            final DrawLog log = render(test);
            // 160 wide against the right edge: text 18 in, lines 9 tall and 3 apart
            assertEquals(List.of("258,7"), textAt(log, "Saved"));
            assertEquals(List.of("258,19"), textAt(log, "Posted."));
            assertTrue(log.textStyles().contains(title));
        }
    }

    @Nested
    class MessageBoxes {
        @Test
        void buttonsCanShareTheRow() {
            final Theme theme = flatWith(builder -> builder.metric(MessageBox.STYLE_BUTTON_WIDTH, 0f)
                    .metric(MessageBox.STYLE_MESSAGE_WIDTH, 214f)
                    .metric(MessageBox.STYLE_BUTTON_GAP, 6f)
                    .metric(MessageBox.STYLE_BUTTON_WIDTH_MAXIMUM, 150f)
                    .metric(MessageBox.STYLE_CLOSE, 0f));
            final TestHost test = new TestHost(new TestPlatform(), theme, 400, 300);
            final MessageBox box = test.factory().messageBox("Title", "Hello", MessageBox.Buttons.YES_NO);
            test.show(test.factory().absolutePanel());
            test.factory().overlays().dialog(box);
            test.host().layout();
            final List<Component<?>> buttons = test.tabOrder().stream()
                    .filter(component -> component instanceof Button).toList();
            assertEquals(2, buttons.size());
            // (214 - 6) / 2
            assertEquals(104f, buttons.get(0).bounds().width());
            assertEquals(104f, buttons.get(1).bounds().width());
            assertEquals(6f, buttons.get(1).bounds().x() - buttons.get(0).bounds().right());
            assertFalse(render(test).lines("text x ").size() > 0);
        }
    }

    @Nested
    class Separators {
        @Test
        void aShadowDoublesTheSeparator() {
            final Theme theme = flatWith(builder -> builder.paint(Separator.STYLE_SHADOW, Paint.solid(0xBF000000)));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            final Separator separator = test.factory().separator();
            test.show(test.factory().absolutePanel().add(separator.setWidth(100f), 0, 0));
            assertEquals(2f, separator.bounds().height());
            final List<String> fills = render(test).lines("fill ");
            assertTrue(fills.contains("fill 0,1 100x1 #BF000000"), fills.toString());
        }
    }

    @Nested
    class Busy {
        @Test
        void theDotsCanBeLetters() {
            final Theme theme = flatWith(builder -> builder.textStyle("busy.dots.text", TextStyle.DEFAULT));
            final TestHost test = new TestHost(new TestPlatform(), theme, 300, 100);
            test.show(test.factory().absolutePanel().add(test.factory().busyIndicator(), 0, 0));
            assertEquals(1, textAt(render(test), "O o o").size());
            test.platform().advanceMillis(300L);
            assertEquals(1, textAt(render(test), "o O o").size());
        }
    }
}
