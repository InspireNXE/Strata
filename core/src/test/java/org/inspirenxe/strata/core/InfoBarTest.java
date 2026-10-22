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
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.display.InfoBar;
import org.inspirenxe.strata.api.control.display.InfoBar.Severity;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The info bar: its layout on one line and wrapped, opening and closing, the close button, the action, its theme keys
 * and its state. Text in the test platform is 6 units per code point and 9 per line. With the default padding of 6
 * and 4 and the accent strip of 3, a bar's text starts at x 9 and y 4, and a one-line bar is 17 tall. The close
 * button is 12 by 9 at the right end, 6 in from the edge.
 */
class InfoBarTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();
    private StackPanel panel;

    /**
     * Shows a bar alone at the top of a column of the given width.
     */
    private InfoBar show(final InfoBar bar, final float width) {
        this.panel = this.ui.stackPanel().setWidth(width).add(bar);
        this.test.show(this.panel);
        return bar;
    }

    private InfoBar logged(final InfoBar bar) {
        return bar.onEvent(Openable.OPEN, event -> this.heard.add("open?"))
                .onEvent(Openable.OPENED, event -> this.heard.add("opened"))
                .onEvent(Closable.CLOSE, event -> this.heard.add("close?"))
                .onEvent(Closable.CLOSED, event -> this.heard.add("closed"));
    }

    private DrawLog render() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas;
    }

    /**
     * Finds the close button: the first tab stop that is not a {@code Button}, since every other control here is one.
     */
    private Component<?> closeButton() {
        return this.test.tabOrder().stream()
                .filter(component -> !(component instanceof Button))
                .findFirst().orElseThrow();
    }

    private boolean key(final Key key) {
        return this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    @Nested
    class Layout {
        @Test
        void sharesOneLineWhenItFits() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Saved", "All good"),
                    300);
            assertEquals(new Rect(0, 0, 300, 17), bar.bounds());
            assertEquals(List.of("fill 0,0 300x17 #FF24262A", "fill 0,0 3x17 #FF4C86E0", "text Saved 9,4 #FFFFFFFF",
                    "text All good 43,4 #FFFFFFFF", "text x 285,4 #FFA0A0A0"), InfoBarTest.this.render().lines());
            assertEquals(new Rect(282, 4, 12, 9), InfoBarTest.this.closeButton().bounds());
        }

        @Test
        void wrapsTheMessageBelowTheTitleWhenItDoesNotFit() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.ERROR, "Not saved",
                    "The server did not answer in time."), 120);
            // The column is 120 less 15 of strip and padding and 16 for the close button: 89
            assertEquals(new Rect(0, 0, 120, 44), bar.bounds());
            assertEquals(List.of("text Not saved 9,4 #FFFFFFFF", "text The server did 9,13 #FFFFFFFF",
                    "text not answer in 9,22 #FFFFFFFF", "text time. 9,31 #FFFFFFFF", "text x 105,4 #FFA0A0A0"),
                    InfoBarTest.this.render().lines("text"));
        }

        @Test
        void putsTheActionBelowTheMessageWhenWrapped() {
            final Button retry = InfoBarTest.this.ui.button("Retry");
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.ERROR, "Not saved",
                    "The server did not answer in time.").setAction(retry), 120);
            // Four lines end at 36; the button, 46 by 20, goes 4 below them
            assertEquals(new Rect(9, 44, 46, 20), retry.bounds());
            assertEquals(new Rect(0, 0, 120, 68), bar.bounds());
        }

        @Test
        void putsTheActionAfterTheMessageOnOneLine() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.SUCCESS, "Deleted", "Ana Lima")
                    .setAction("Undo", () -> { }), 300);
            final Component<?> undo = bar.action().orElseThrow();
            assertEquals(new Rect(107, 4, 24, 9), undo.bounds());
            assertEquals(17f, bar.bounds().height());
        }

        @Test
        void takesOneLineWhenTheWidthIsUnbounded() {
            final InfoBar bar = InfoBarTest.this.ui.infoBar(Severity.WARNING, "Low stock",
                    "Only three crates of pears are left at the morning market.");
            InfoBarTest.this.test.show(InfoBarTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(bar));
            // 15 of strip and padding, 54 and 348 of text, 4 between them, 16 for the close button
            assertEquals(new Rect(0, 0, 437, 17), bar.bounds());
        }

        @Test
        void stacksItsLinesAtTheWidestWhenTheWidthIsUnboundedAndTheTextBreaks() {
            final InfoBar bar = InfoBarTest.this.ui.infoBar(Severity.WARNING, "Low stock", "Only three\ncrates left");
            InfoBarTest.this.test.show(InfoBarTest.this.ui.stackPanel(Orientation.HORIZONTAL).add(bar));
            // 15 of strip and padding, the widest line "crates left" at 66, 16 for the close button; three lines
            assertEquals(new Rect(0, 0, 97, 35), bar.bounds());
            assertEquals(List.of("text Low stock 9,4 #FFFFFFFF", "text Only three 9,13 #FFFFFFFF",
                    "text crates left 9,22 #FFFFFFFF", "text x 82,4 #FFA0A0A0"),
                    InfoBarTest.this.render().lines("text"));
        }

        @Test
        void isALineTallWhenEmpty() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 200);
            assertEquals(new Rect(0, 0, 200, 17), bar.bounds());
            assertEquals(Severity.INFO, bar.severity());
            assertEquals(Text.empty(), bar.title());
            assertEquals(Text.empty(), bar.text());
            assertEquals(Optional.empty(), bar.action());
        }

        @Test
        void leavesOutTheCloseButtonWhenNotClosable() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Saved", "")
                    .setClosable(false), 300);
            assertFalse(bar.isClosable());
            assertEquals(List.of("text Saved 9,4 #FFFFFFFF"), InfoBarTest.this.render().lines("text"));
            assertEquals(List.of(), InfoBarTest.this.test.tabOrder());
            bar.setClosable(true);
            InfoBarTest.this.test.host().layout();
            assertEquals(1, InfoBarTest.this.test.tabOrder().size());
        }
    }

    @Nested
    class OpeningAndClosing {
        @Test
        void aClosedBarMeasuresZeroAndLeavesTheTree() {
            final Button undo = InfoBarTest.this.ui.button("Undo");
            final InfoBar bar = InfoBarTest.this.logged(InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(
                    Severity.INFO, "Saved", "").setAction(undo), 300));
            assertTrue(bar.isOpen());
            bar.close();
            InfoBarTest.this.test.host().layout();
            assertFalse(bar.isOpen());
            assertEquals(0f, bar.bounds().height());
            assertFalse(undo.isAttached());
            assertEquals(List.of(), InfoBarTest.this.test.tabOrder());
            assertEquals(List.of(), InfoBarTest.this.render().lines());
            assertEquals(List.of("closed"), InfoBarTest.this.heard);

            bar.close();
            assertEquals(List.of("closed"), InfoBarTest.this.heard);
            bar.open();
            InfoBarTest.this.test.host().layout();
            assertTrue(undo.isAttached());
            // The button is 20 tall, so the bar is 28
            assertEquals(28f, bar.bounds().height());
            assertEquals(2, InfoBarTest.this.test.tabOrder().size());
            bar.open();
            // Opening from code never asks
            assertEquals(List.of("closed", "opened"), InfoBarTest.this.heard);
        }

        @Test
        void theCloseButtonAsksFirst() {
            final InfoBar bar = InfoBarTest.this.logged(InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(
                    Severity.INFO, "Saved", ""), 300));
            final AtomicInteger refuse = new AtomicInteger(1);
            bar.onEvent(Closable.CLOSE, event -> {
                if (refuse.getAndDecrement() > 0) {
                    event.cancel();
                }
            });
            InfoBarTest.this.test.click(288, 8);
            assertTrue(bar.isOpen());
            InfoBarTest.this.test.click(288, 8);
            assertFalse(bar.isOpen());
            assertEquals(List.of("close?", "close?", "closed"), InfoBarTest.this.heard);
        }

        @Test
        void enterAndSpaceOnTheFocusedCloseButtonClose() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Saved", ""), 300);
            final Component<?> close = InfoBarTest.this.closeButton();
            InfoBarTest.this.test.host().root().moveFocus(false);
            assertSame(close, InfoBarTest.this.test.focused());
            assertTrue(InfoBarTest.this.key(Key.SPACE));
            assertFalse(bar.isOpen());
            assertNull(InfoBarTest.this.test.focused());

            bar.open();
            InfoBarTest.this.test.host().layout();
            InfoBarTest.this.test.host().root().moveFocus(false);
            assertTrue(InfoBarTest.this.key(Key.ENTER));
            assertFalse(bar.isOpen());
        }

        @Test
        void hidingIsNotClosing() {
            final InfoBar bar = InfoBarTest.this.logged(InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300));
            bar.setVisible(false);
            InfoBarTest.this.test.host().layout();
            assertTrue(bar.isOpen());
            assertEquals(List.of(), InfoBarTest.this.heard);
        }

        @Test
        void theCloseButtonsTooltipSaysClose() {
            InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300);
            final HasTooltip<?> close = (HasTooltip<?>) InfoBarTest.this.closeButton();
            assertEquals(Tooltip.text(Text.translatable("strata.button.close")), close.tooltip().orElseThrow());
            assertEquals("Close", Text.translatable("strata.button.close").plain());
        }

        @Test
        void aDisabledBarTakesNoInput() {
            final InfoBar bar = InfoBarTest.this.logged(InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300));
            bar.setEnabled(false);
            assertEquals(List.of(), InfoBarTest.this.test.tabOrder());
            InfoBarTest.this.test.click(288, 8);
            assertTrue(bar.isOpen());
            assertEquals(List.of(), InfoBarTest.this.heard);
        }
    }

    @Nested
    class Actions {
        @Test
        void eventsOfTheActionPassTheBarAndDoNotCloseIt() {
            final Button retry = InfoBarTest.this.ui.button("Retry");
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.ERROR, "Failed", "")
                    .setAction(retry), 300);
            InfoBarTest.this.panel.onEvent(Button.PRESSED, event -> InfoBarTest.this.heard.add("pressed"));
            InfoBarTest.this.test.click(retry.bounds().x() + 2, retry.bounds().y() + 2);
            assertEquals(List.of("pressed"), InfoBarTest.this.heard);
            assertTrue(bar.isOpen());
        }

        @Test
        void theShorthandIsALinkThatRunsTheAction() {
            final AtomicInteger runs = new AtomicInteger();
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Removed", "")
                    .setAction(Text.of("Undo"), runs::incrementAndGet), 300);
            final Button undo = (Button) bar.action().orElseThrow();
            assertTrue(undo.isLink());
            assertEquals(Text.of("Undo"), undo.text());
            undo.press();
            assertEquals(1, runs.get());
            assertTrue(bar.isOpen());
        }

        @Test
        void comeBeforeTheCloseButtonInTheTabOrder() {
            final Button retry = InfoBarTest.this.ui.button("Retry");
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300);
            bar.setAction(retry);
            InfoBarTest.this.test.host().layout();
            final List<Component<?>> order = InfoBarTest.this.test.tabOrder();
            assertEquals(2, order.size());
            assertSame(retry, order.getFirst());
        }

        @Test
        void areReplacedAndRemoved() {
            final Button first = InfoBarTest.this.ui.button("First");
            final Button second = InfoBarTest.this.ui.button("Second");
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar().setAction(first), 300);
            bar.setAction(second);
            assertFalse(first.isAttached());
            assertTrue(second.isAttached());
            assertEquals(Optional.of(second), bar.action());
            bar.setAction((Component<?>) null);
            assertFalse(second.isAttached());
            assertEquals(Optional.empty(), bar.action());
        }

        @Test
        void setWhileClosedJoinOnOpening() {
            final Button retry = InfoBarTest.this.ui.button("Retry");
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300).close();
            bar.setAction(retry);
            assertFalse(retry.isAttached());
            bar.open();
            assertTrue(retry.isAttached());
        }

        @Test
        void refuseComponentsInATreeOrFromAnotherFactory() {
            final InfoBar bar = InfoBarTest.this.ui.infoBar();
            final Button placed = InfoBarTest.this.ui.button("Placed");
            InfoBarTest.this.ui.stackPanel().add(placed);
            assertThrows(IllegalArgumentException.class, () -> bar.setAction(placed));
            final TestHost other = new TestHost(100, 100);
            assertThrows(IllegalArgumentException.class, () -> bar.setAction(other.factory().button("Other")));
        }
    }

    @Nested
    class Theming {
        @Test
        void readsEachSeveritysBackgroundAndAccent() {
            final Theme theme = Themes.flat().toBuilder()
                    .paint(InfoBar.STYLE_BACKGROUND_ERROR, Paint.solid(0xFF400000))
                    .build();
            InfoBarTest.this.test.host().setTheme(theme);
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar().setClosable(false), 100);
            assertEquals(List.of("fill 0,0 100x17 #FF24262A", "fill 0,0 3x17 #FF4C86E0"),
                    InfoBarTest.this.render().lines());
            bar.setSeverity(Severity.ERROR);
            InfoBarTest.this.test.host().layout();
            assertEquals(List.of("fill 0,0 100x17 #FF400000", "fill 0,0 3x17 #FFE05050"),
                    InfoBarTest.this.render().lines());
            bar.setSeverity(Severity.WARNING);
            assertEquals("fill 0,0 3x17 #FFE8A33D", InfoBarTest.this.render().lines().get(1));
            bar.setSeverity(Severity.SUCCESS);
            assertEquals("fill 0,0 3x17 #FF43A047", InfoBarTest.this.render().lines().get(1));
        }

        @Test
        void drawsNoAccentWhenTheThemeHasNone() {
            final Theme theme = Theme.builder("plain")
                    .paint(InfoBar.STYLE_BACKGROUND, Paint.solid(0xFF101010))
                    .build();
            final TestHost host = new TestHost(new TestPlatform(), theme, 200, 100);
            host.show(host.factory().stackPanel().setWidth(100).add(host.factory().infoBar().setClosable(false)));
            final DrawLog canvas = new DrawLog();
            host.host().render(canvas);
            assertEquals(List.of("fill 0,0 100x17 #FF101010"), canvas.lines());
        }

        @Test
        void showsTheSeveritysIconALineTall() {
            InfoBarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .paint(InfoBar.STYLE_ICON_INFO, Paint.solid(0xFF0000FF))
                    .build());
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Note", "")
                    .setClosable(false), 200);
            final List<String> lines = InfoBarTest.this.render().lines();
            assertTrue(lines.contains("fill 9,4 9x9 #FF0000FF"));
            assertTrue(lines.contains("text Note 22,4 #FFFFFFFF"));
            // Another severity has no icon here, and hiding the icon gives its space back
            bar.setSeverity(Severity.ERROR);
            InfoBarTest.this.test.host().layout();
            assertTrue(InfoBarTest.this.render().lines().contains("text Note 9,4 #FFFFFFFF"));
            bar.setSeverity(Severity.INFO).setIconShown(false);
            InfoBarTest.this.test.host().layout();
            assertFalse(bar.isIconShown());
            assertTrue(InfoBarTest.this.render().lines().contains("text Note 9,4 #FFFFFFFF"));
        }

        @Test
        void drawsTheTitleInBoldMessageTextUnlessItHasAStyle() {
            InfoBarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .textStyle(InfoBar.STYLE_TEXT, TextStyle.DEFAULT.withColor(0xFF112233))
                    .build());
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Title", "Message"), 300);
            final DrawLog canvas = InfoBarTest.this.render();
            assertEquals(List.of("text Title 9,4 #FF112233", "text Message 43,4 #FF112233", "text x 285,4 #FFA0A0A0"),
                    canvas.lines("text"));
            assertEquals(TextStyle.DEFAULT.withColor(0xFF112233).withBold(true), canvas.textStyles().getFirst());
            assertEquals(TextStyle.DEFAULT.withColor(0xFF112233), canvas.textStyles().get(1));
            InfoBarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .textStyle(InfoBar.STYLE_TITLE, TextStyle.DEFAULT.withColor(0xFF445566))
                    .build());
            InfoBarTest.this.test.host().layout();
            assertTrue(InfoBarTest.this.render().lines().contains("text Title 9,4 #FF445566"));
            assertEquals(Text.of("Title"), bar.title());
        }

        @Test
        void drawsTheCloseButtonWithItsOwnKeysOrTheWindows() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300);
            assertEquals(List.of("text x 285,4 #FFA0A0A0"), InfoBarTest.this.render().lines("text x"));
            InfoBarTest.this.test.host().pointerMoved(288, 8, Modifiers.NONE);
            assertEquals(List.of("text x 285,4 #FFFFFFFF"), InfoBarTest.this.render().lines("text x"));
            InfoBarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .textStyle(InfoBar.STYLE_CLOSE, TextStyle.DEFAULT.withColor(0xFF000011))
                    .textStyle(InfoBar.STYLE_CLOSE_HOVERED, TextStyle.DEFAULT.withColor(0xFF000022))
                    .textStyle(Window.STYLE_CLOSE, TextStyle.DEFAULT.withColor(0xFF000033))
                    .build());
            InfoBarTest.this.test.host().layout();
            InfoBarTest.this.test.host().pointerMoved(100, 100, Modifiers.NONE);
            assertEquals(List.of("text x 285,4 #FF000011"), InfoBarTest.this.render().lines("text x"));
            InfoBarTest.this.test.host().pointerMoved(288, 8, Modifiers.NONE);
            assertEquals(List.of("text x 285,4 #FF000022"), InfoBarTest.this.render().lines("text x"));
            assertTrue(bar.isOpen());
        }

        @Test
        void ringsTheFocusedCloseButton() {
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(), 300);
            InfoBarTest.this.test.host().root().moveFocus(false);
            final List<String> lines = InfoBarTest.this.render().lines();
            // The hovered style while focused, then the button.focus ring around the 12 by 9 button
            assertTrue(lines.contains("text x 285,4 #FFFFFFFF"));
            assertTrue(lines.contains("fill 282,4 12x1 #FF6A9FFF"));
            assertTrue(bar.isOpen());
        }

        @Test
        void readsItsPadding() {
            InfoBarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .insets(InfoBar.STYLE_PADDING, new Insets(1, 2, 3, 4))
                    .build());
            final InfoBar bar = InfoBarTest.this.show(InfoBarTest.this.ui.infoBar(Severity.INFO, "Hi", ""), 300);
            assertEquals(new Rect(0, 0, 300, 15), bar.bounds());
            assertEquals(List.of("text Hi 4,2 #FFFFFFFF", "text x 288,2 #FFA0A0A0"),
                    InfoBarTest.this.render().lines("text"));
        }

        @Test
        void requiresItsBackground() {
            final TestHost bare = new TestHost(new TestPlatform(), Theme.builder("bare").build(), 100, 100);
            final ThemeException error = assertThrows(ThemeException.class, () -> bare.show(bare.factory().infoBar()));
            assertEquals(Set.of(InfoBar.STYLE_BACKGROUND), error.missingKeys());
        }
    }

    @Test
    void snapshotsItsState() {
        final InfoBar bar = this.show(this.ui.infoBar(Severity.WARNING, "Low", "Pears"), 300);
        final List<ComponentSnapshot> snapshots = new ArrayList<>();
        bar.onEvent(Closable.CLOSE, event -> snapshots.add(event.snapshot()));
        this.test.click(288, 8);
        assertEquals(new InfoBar.State(Severity.WARNING, Text.of("Low"), Text.of("Pears"), true),
                snapshots.getFirst().state());
        assertFalse(bar.isOpen());
    }
}
