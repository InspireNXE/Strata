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
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.text.TextInput;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextOverflow;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Border;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tier 2 controls, rich labels and the text box additions. Text in the test platform is 6 units per code point
 * and 9 per line.
 */
class ControlsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void center(final Rect bounds, final MouseButton button) {
        final float x = bounds.x() + bounds.width() / 2f;
        final float y = bounds.y() + bounds.height() / 2f;
        this.test.host().pointerMoved(x, y, Modifiers.NONE);
        this.test.host().pointerButton(x, y, button, true, Modifiers.NONE);
        this.test.host().pointerButton(x, y, button, false, Modifiers.NONE);
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
    }

    @Nested
    class Labels {
        @Test
        void spansJoinTheirTextAndMeasureTogether() {
            final Label label = ControlsTest.this.ui.label(Text.concat(Text.of("Diamond"), Text.of(" x 64").italic()));
            ControlsTest.this.test.show(label);
            assertEquals("Diamond x 64", label.text().plain());
            assertEquals(12 * 6f, label.desiredWidth());
        }

        @Test
        void scaleChangesTheMeasuredSize() {
            final Label label = ControlsTest.this.ui.label("abcd").setTextScale(0.5f);
            ControlsTest.this.test.show(label);
            assertEquals(12f, label.desiredWidth());
            assertEquals(4.5f, label.desiredHeight());
        }

        @Test
        void anEllipsisLabelAsksNoMoreThanItIsOffered() {
            final Label label = ControlsTest.this.ui.label("abcdefghijklmnopqrstuvwxyz").setOverflow(TextOverflow.ELLIPSIS);
            label.setMaxSize(60, Float.POSITIVE_INFINITY);
            ControlsTest.this.test.show(label);
            assertEquals(60f, label.desiredWidth());
            final NullCanvas canvas = new NullCanvas();
            ControlsTest.this.test.host().render(canvas);
            assertEquals(List.of("abcdefg..."), canvas.texts().stream().map(TextSpan::text).toList());
        }

        @Test
        void wrappedSpansKeepTheirStyles() {
            final TextStyle red = TextStyle.DEFAULT.withColor(0xFFFF0000);
            final Label label = ControlsTest.this.ui.label(Text.of("one two ").append("three four", red)).setWrap(true);
            label.setMaxSize(48, Float.POSITIVE_INFINITY);
            ControlsTest.this.test.show(label);
            final NullCanvas canvas = new NullCanvas();
            ControlsTest.this.test.host().render(canvas);
            assertEquals(List.of(new TextSpan("one two", label.style()), new TextSpan("three", red), new TextSpan("four", red)),
                    canvas.texts());
        }
    }

    @Nested
    class TextBoxes {
        private TextBox box;

        private void show() {
            this.box = ControlsTest.this.ui.textBox();
            ControlsTest.this.test.show(this.box);
            this.box.requestFocus();
        }

        private void type(final String text) {
            text.codePoints().forEach(code -> ControlsTest.this.test.host().charTyped(code, Modifiers.NONE));
        }

        @Test
        void anEditHandlerCanRejectAnEdit() {
            this.show();
            final List<String> heard = new ArrayList<>();
            this.box.onEvent(TextInput.EDIT, event -> {
                if (!event.value().matches("\\d*")) {
                    event.cancel();
                }
            }).onEvent(TextInput.EDITED, event -> heard.add(event.value()));
            this.type("1a2");
            assertEquals("12", this.box.text());
            assertEquals(List.of("1", "12"), heard);
        }

        @Test
        void anEditHandlerCanRewriteAnEdit() {
            this.show();
            this.box.onEvent(TextInput.EDIT, event -> event.setValue(
                    event.value().isEmpty() ? "" : Long.toString(Math.min(Long.parseLong(event.value()), 50))));
            this.type("99");
            assertEquals("50", this.box.text());
            assertEquals(2, this.box.caret());
        }

        @Test
        void enterSubmitsAndIsConsumed() {
            this.show();
            final List<String> submitted = new ArrayList<>();
            this.box.setText("go").onEvent(TextBox.SUBMITTED, event -> submitted.add(event.value()));
            assertTrue(ControlsTest.this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE));
            assertEquals(List.of("go"), submitted);
        }

        @Test
        void aReadOnlyBoxDoesNotEdit() {
            this.show();
            this.box.setText("fixed").setReadOnly(true);
            this.type("x");
            ControlsTest.this.key(Key.BACKSPACE);
            assertEquals("fixed", this.box.text());
        }

        @Test
        void selectAllOnFocusSelectsEverything() {
            this.box = ControlsTest.this.ui.textBox().setText("12").setSelectAllOnFocus(true);
            ControlsTest.this.test.show(this.box);
            this.box.requestFocus();
            assertEquals(0, this.box.selectionStart());
            assertEquals(2, this.box.selectionEnd());
        }

        @Test
        void aClickThatFocusesSelectsEverythingAndTheNextClickPlacesTheCaret() {
            this.box = ControlsTest.this.ui.textBox().setText("1234").setSelectAllOnFocus(true);
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel().add(this.box, 0, 0));
            final Rect bounds = this.box.bounds();
            ControlsTest.this.test.click(bounds.x() + 2, bounds.y() + 2);
            assertEquals(0, this.box.selectionStart());
            assertEquals(4, this.box.selectionEnd());
            ControlsTest.this.test.platform().advanceMillis(1000);
            ControlsTest.this.test.click(bounds.x() + 2, bounds.y() + 2);
            assertEquals(this.box.selectionStart(), this.box.selectionEnd());
        }
    }

    @Nested
    class ComboBoxes {
        private ComboBox<String> combo;

        private void show() {
            this.combo = ControlsTest.this.ui.<String>comboBox(String::toUpperCase).setItems(List.of("a", "b", "c"));
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel().add(this.combo, 10, 10));
        }

        @Test
        void theFirstOptionStartsSelected() {
            this.show();
            assertEquals(Optional.of("a"), this.combo.selected());
            // "A": padding, one character, padding, the arrow, padding; one line plus 6
            assertEquals(4 + 6 + 4 + 7 + 4, this.combo.desiredWidth());
            assertEquals(15f, this.combo.desiredHeight());
        }

        @Test
        void clickingAnOptionSelectsItAndCloses() {
            this.show();
            final List<String> heard = new ArrayList<>();
            this.combo.onEvent(SingleSelection.SELECTED, e -> heard.add(this.combo.items().get(e.value())));
            ControlsTest.this.center(this.combo.bounds(), MouseButton.LEFT);
            assertTrue(this.combo.isOpen());
            ControlsTest.this.test.host().layout();
            // Rows are a line plus 4 tall, below the box
            final float rowY = this.combo.bounds().bottom() + 13 * 2 + 6;
            ControlsTest.this.test.click(this.combo.bounds().x() + 5, rowY);
            assertFalse(this.combo.isOpen());
            assertEquals(List.of("c"), heard);
        }

        @Test
        void keysChangeTheSelectionWhileClosed() {
            this.show();
            this.combo.requestFocus();
            ControlsTest.this.key(Key.DOWN);
            ControlsTest.this.key(Key.DOWN);
            assertEquals(2, this.combo.selectedIndex());
            ControlsTest.this.key(Key.HOME);
            assertEquals(0, this.combo.selectedIndex());
        }

        @Test
        void escapeClosesWithoutChanging() {
            this.show();
            this.combo.requestFocus();
            ControlsTest.this.key(Key.SPACE);
            assertTrue(this.combo.isOpen());
            ControlsTest.this.key(Key.DOWN);
            ControlsTest.this.key(Key.ESCAPE);
            assertFalse(this.combo.isOpen());
            assertEquals(0, this.combo.selectedIndex());
        }

        @Test
        void newOptionsKeepAnEqualSelection() {
            this.show();
            this.combo.select(1);
            this.combo.setItems(List.of("z", "b"));
            assertEquals(1, this.combo.selectedIndex());
            this.combo.setItems(List.of("x", "y"));
            assertEquals(0, this.combo.selectedIndex());
        }
    }

    @Nested
    class Windows {
        @Test
        void escapeClosesADialogWindowAndTellsListeners() {
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel());
            final List<String> closed = new ArrayList<>();
            final Window window = ControlsTest.this.ui.window("Title").onEvent(Closable.CLOSED, event -> closed.add("window"));
            window.add(ControlsTest.this.ui.button("OK"));
            final var overlay = ControlsTest.this.ui.overlays().dialog(window);
            ControlsTest.this.key(Key.ESCAPE);
            assertEquals(List.of("window"), closed);
            assertFalse(overlay.isOpen());
        }

        @Test
        void draggingTheTitleBarMovesTheWindowWithinItsSlot() {
            final Window window = ControlsTest.this.ui.window("Title");
            window.setSize(100, 60);
            ControlsTest.this.test.show(ControlsTest.this.ui.anchorPanel().add(window).setAnchor(Anchor.FILL));
            final Rect start = window.bounds();
            assertEquals(new Rect(100, 120, 100, 60), start);
            final var host = ControlsTest.this.test.host();
            host.pointerButton(130, 125, MouseButton.LEFT, true, Modifiers.NONE);
            host.pointerMoved(180, 145, Modifiers.NONE);
            host.pointerMoved(400, 145, Modifiers.NONE);
            host.pointerButton(400, 145, MouseButton.LEFT, false, Modifiers.NONE);
            host.layout();
            assertEquals(new Rect(200, 140, 100, 60), window.bounds());
        }

        @Test
        void theCloseButtonClosesTheWindow() {
            final List<String> closed = new ArrayList<>();
            final Window window = ControlsTest.this.ui.window("Title").onEvent(Closable.CLOSED, event -> closed.add("x"));
            window.setSize(100, 60);
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel().add(window, 0, 0));
            ControlsTest.this.test.click(100 - 7, 5);
            assertEquals(List.of("x"), closed);
        }
    }

    @Nested
    class MessageBoxes {
        @Test
        void aButtonReportsItsResultOnceAndCloses() {
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel());
            final List<MessageBox.Result> results = new ArrayList<>();
            final MessageBox box = ControlsTest.this.ui.messageBox("Delete", "Sure?", MessageBox.Buttons.YES_NO)
                    .onEvent(MessageBox.ANSWERED, event -> results.add(event.value()));
            final var overlay = ControlsTest.this.ui.overlays().dialog(box);
            // The first button is focused
            ControlsTest.this.key(Key.ENTER);
            assertEquals(List.of(MessageBox.Result.YES), results);
            assertFalse(overlay.isOpen());
        }

        @Test
        void escapeReportsTheDismissResult() {
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel());
            final List<MessageBox.Result> results = new ArrayList<>();
            ControlsTest.this.ui.overlays().dialog(ControlsTest.this.ui.messageBox("Save", "Save?", MessageBox.Buttons.YES_NO_CANCEL)
                    .onEvent(MessageBox.ANSWERED, event -> results.add(event.value())));
            ControlsTest.this.key(Key.ESCAPE);
            assertEquals(List.of(MessageBox.Result.CANCEL), results);
        }
    }

    @Nested
    class Tabs {
        @Test
        void tabsShareTheWidthAndClicksSelect() {
            final TabBar tabs = ControlsTest.this.ui.tabBar().addTab("Buy").addTab("Sell");
            tabs.setWidth(200);
            final List<Integer> heard = new ArrayList<>();
            tabs.onEvent(SingleSelection.SELECTED, event -> heard.add(event.value()));
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel().add(tabs, 0, 0));
            assertEquals(0, tabs.selectedIndex());
            assertEquals(16f, tabs.desiredHeight());
            ControlsTest.this.test.click(150, 5);
            assertEquals(List.of(1), heard);
        }

        @Test
        void aDisabledTabCannotBeChosenButStillShowsItsTooltip() {
            final TabBar tabs = ControlsTest.this.ui.tabBar()
                    .addTab("Buy")
                    .addTab(ControlsTest.this.ui.tab("Sell").setEnabled(false).setTooltip(Tooltip.text("Empty")));
            tabs.setWidth(200);
            ControlsTest.this.test.show(ControlsTest.this.ui.absolutePanel().add(tabs, 0, 0));
            ControlsTest.this.test.click(150, 5);
            assertEquals(0, tabs.selectedIndex());
            assertTrue(ControlsTest.this.test.host().root().tooltip().isPresent());
            tabs.requestFocus();
            ControlsTest.this.key(Key.RIGHT);
            assertEquals(0, tabs.selectedIndex());
        }
    }

    @Test
    void progressBarsClampTheirValueToTheRange() {
        final ProgressBar bar = this.ui.progressBar();
        assertEquals(1.0, bar.setValue(3).value());
        assertEquals(0.25, bar.setValue(0.25).value());
        assertThrows(IllegalArgumentException.class, () -> bar.setValue(Double.NaN));
        assertEquals(12.0, bar.setRange(12, 36).value());
        assertEquals(36.0, bar.setValue(50).value());
        assertThrows(IllegalArgumentException.class, () -> bar.setRange(5, 5));
    }

    @Test
    void separatorsStretchAlongTheirLine() {
        final Separator line = this.ui.separator(Orientation.HORIZONTAL);
        this.test.show(this.ui.stackPanel().setPadding(10).add(line).setAnchor(Anchor.FILL));
        assertEquals(new Rect(10, 10, 280, 1), line.bounds());
    }

    @Test
    void bordersDrawInsideTheBoundsWithoutChangingLayout() {
        final Box box = new Box(this.ui, 20, 10);
        box.setBorder(new Border(0xFFFFFFFF, new Insets(1, 2, 3, 0)));
        this.test.show(this.ui.absolutePanel().add(box, 0, 0));
        assertEquals(new Rect(0, 0, 20, 10), box.bounds());
        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);
        assertEquals(List.of(new Rect(0, 0, 20, 2), new Rect(0, 2, 1, 8), new Rect(17, 2, 3, 8)), canvas.fills());
    }
}
