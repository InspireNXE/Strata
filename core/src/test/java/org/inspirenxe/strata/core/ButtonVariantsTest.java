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
import org.inspirenxe.strata.api.control.attribute.item.SingleSelection;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CycleButton;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The cycle button, and the icon and link settings of the button. Text in the test platform is 6 units per code
 * point and 9 per line.
 */
class ButtonVariantsTest {
    private static final SpriteId ICON = SpriteId.of("test", "icon");

    private final TestHost test = new TestHost(300, 100);
    private final ComponentFactory ui = this.test.factory();

    private AbsolutePanel show(final org.inspirenxe.strata.api.component.Component<?> component) {
        final AbsolutePanel panel = this.ui.absolutePanel().add(component, 0, 0);
        this.test.show(panel);
        return panel;
    }

    private void click(final MouseButton button, final Modifiers modifiers) {
        this.test.host().pointerButton(5, 5, button, true, modifiers);
        this.test.host().pointerButton(5, 5, button, false, modifiers);
    }

    private void key(final Key key, final Modifiers modifiers) {
        this.test.host().key(key, KeyAction.PRESS, modifiers);
    }

    @Test
    void theFlatThemeDefinesTheLinkKeys() {
        final List<String> keys = List.of(Button.STYLE_LINK_TEXT, Button.STYLE_LINK_TEXT_HOVERED,
                Button.STYLE_LINK_TEXT_DISABLED);
        assertEquals(List.of(), List.copyOf(Themes.flat().missing(keys)));
    }

    @Nested
    class CycleButtons {
        private final List<String> seen = new ArrayList<>();
        private CycleButton<String> button;
        private AbsolutePanel form;

        private void show() {
            this.button = ButtonVariantsTest.this.ui.<String>cycleButton("Mode")
                    .setItems(List.of("Easy", "Normal", "Hard"))
                    .onEvent(SingleSelection.SELECT, event -> this.seen.add("select " + event.value()))
                    .onEvent(SingleSelection.SELECTED, event -> this.seen.add("selected " + event.value()
                            + " from " + event.previous()));
            this.form = ButtonVariantsTest.this.show(this.button);
        }

        private List<String> shown() {
            final NullCanvas canvas = new NullCanvas();
            ButtonVariantsTest.this.test.host().render(canvas);
            return canvas.texts().stream().map(TextSpan::text).toList();
        }

        @Test
        void showsTheLabelAndTheSelectedValue() {
            this.show();
            assertEquals(List.of("Mode: Easy"), this.shown());
            this.button.select(2);
            assertEquals(List.of("Mode: Hard"), this.shown());
        }

        @Test
        void showsOnlyTheLabelWithoutValuesAndOnlyTheValueWithoutLabel() {
            this.show();
            this.button.setItems(List.of());
            assertEquals(List.of("Mode"), this.shown());
            this.button.setText("").setItems(List.of("Easy"));
            assertEquals(List.of("Easy"), this.shown());
        }

        @Test
        void usesTheValueLabelFunction() {
            this.show();
            this.button.setValueLabelFunction(String::toUpperCase);
            assertEquals(List.of("Mode: EASY"), this.shown());
        }

        @Test
        void asksForTheWidestTextSoCyclingDoesNotResize() {
            this.show();
            assertEquals(new Rect(0, 0, 6 * "Mode: Normal".length() + 16, 20), this.button.bounds());
            this.button.select(2);
            ButtonVariantsTest.this.test.host().layout();
            assertEquals(6 * "Mode: Normal".length() + 16, this.button.bounds().width());
        }

        @Test
        void aLeftClickStepsForwardAndWraps() {
            this.show();
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            assertEquals(List.of("select 1", "selected 1 from 0", "select 2", "selected 2 from 1", "select 0",
                    "selected 0 from 2"), this.seen);
            assertEquals(0, this.button.selectedIndex());
        }

        @Test
        void aRightClickOrShiftClickStepsBackAndWraps() {
            this.show();
            ButtonVariantsTest.this.click(MouseButton.RIGHT, Modifiers.NONE);
            assertEquals(2, this.button.selectedIndex());
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.of(Modifier.SHIFT));
            assertEquals(1, this.button.selectedIndex());
        }

        @Test
        void enterAndSpaceStepAndShiftReverses() {
            this.show();
            this.button.requestFocus();
            ButtonVariantsTest.this.key(Key.ENTER, Modifiers.NONE);
            ButtonVariantsTest.this.key(Key.SPACE, Modifiers.NONE);
            assertEquals(2, this.button.selectedIndex());
            ButtonVariantsTest.this.key(Key.ENTER, Modifiers.of(Modifier.SHIFT));
            assertEquals(1, this.button.selectedIndex());
        }

        @Test
        void cancelingTheRequestKeepsTheValue() {
            this.show();
            this.button.onEvent(SingleSelection.SELECT, event -> event.cancel());
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            assertEquals(0, this.button.selectedIndex());
            assertEquals(List.of("select 1"), this.seen);
        }

        @Test
        void aHandlerMayRewriteTheRequestedIndex() {
            this.show();
            this.button.onEvent(SingleSelection.SELECT, event -> event.setValue(2));
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            assertEquals(2, this.button.selectedIndex());
        }

        @Test
        void aFormHearsTheSelection() {
            this.show();
            final List<Object> targets = new ArrayList<>();
            this.form.onEvent(SingleSelection.SELECTED, event -> targets.add(event.control()));
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            assertEquals(List.of(this.button), targets);
        }

        @Test
        void codeReportsWithoutAskingAndCycleWraps() {
            this.show();
            this.button.select(1).cycle(2).cycle(-4);
            assertEquals(List.of("selected 1 from 0", "selected 0 from 1", "selected 2 from 0"), this.seen);
            assertThrows(IndexOutOfBoundsException.class, () -> this.button.select(3));
        }

        @Test
        void cyclingFromNothingStartsAtTheEnds() {
            this.show();
            this.button.clearSelection().cycle(1);
            assertEquals(0, this.button.selectedIndex());
            this.button.clearSelection().cycle(-1);
            assertEquals(2, this.button.selectedIndex());
        }

        @Test
        void aDisabledButtonAndASingleValueIgnoreTheUser() {
            this.show();
            this.button.setEnabled(false);
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            this.button.setEnabled(true).setItems(List.of("Only"));
            this.seen.clear();
            ButtonVariantsTest.this.click(MouseButton.LEFT, Modifiers.NONE);
            assertEquals(List.of(), this.seen);
        }

        @Test
        void settingItemsKeepsTheSelectionByKey() {
            this.show();
            this.button.select(1).setKeyFunction(String::length);
            this.seen.clear();
            this.button.setItems(List.of("Hard", "Soft", "Normal"));
            assertEquals(2, this.button.selectedIndex());
            assertEquals(List.of("selected 2 from 1"), this.seen);
        }
    }

    @Nested
    class Icons {
        private Button button;

        private BasicControlsTest.SpriteCanvas render() {
            final BasicControlsTest.SpriteCanvas canvas = new BasicControlsTest.SpriteCanvas();
            ButtonVariantsTest.this.test.host().render(canvas);
            return canvas;
        }

        @Test
        void anIconGoesBeforeTheCaptionAndTheTwoAreCentered() {
            this.button = ButtonVariantsTest.this.ui.button("OK").setIcon(ICON);
            ButtonVariantsTest.this.show(this.button);
            assertEquals(new Rect(0, 0, 16 + 4 + 12 + 16, 26), this.button.bounds());
            assertEquals(List.of(new Rect(8, 5, 16, 16)), this.render().sprites);
            assertEquals(List.of("OK"), this.render().texts);
        }

        @Test
        void anIconAloneIsTwentyByTwenty() {
            this.button = ButtonVariantsTest.this.ui.button().setIcon(ICON);
            ButtonVariantsTest.this.show(this.button);
            assertEquals(new Rect(0, 0, 20, 20), this.button.bounds());
            assertEquals(List.of(new Rect(2, 2, 16, 16)), this.render().sprites);
            assertEquals(List.of(), this.render().texts);
        }

        @Test
        void theIconSizeAndPaddingChangeTheDesiredSize() {
            this.button = ButtonVariantsTest.this.ui.button().setIcon(ICON).setIconSize(10, 12);
            ButtonVariantsTest.this.show(this.button);
            assertEquals(new Rect(0, 0, 20, 20), this.button.bounds());
            this.button.setTextPadding(org.inspirenxe.strata.api.geometry.Insets.of(10, 6));
            ButtonVariantsTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 30, 24), this.button.bounds());
            assertThrows(IllegalArgumentException.class, () -> this.button.setIconSize(-1, 4));
        }

        @Test
        void aDisabledButtonDimsItsIconAndClearIconRemovesIt() {
            this.button = ButtonVariantsTest.this.ui.button("OK").setIcon(ICON);
            ButtonVariantsTest.this.show(this.button);
            assertEquals(List.of(0xFFFFFFFF), this.render().tints);
            this.button.setEnabled(false);
            assertEquals(List.of(0xFF808080), this.render().tints);
            assertEquals(ICON, this.button.iconSprite().orElseThrow());
            this.button.clearIcon();
            ButtonVariantsTest.this.test.host().layout();
            assertTrue(this.button.iconSprite().isEmpty());
            assertTrue(this.button.iconVisual().isEmpty());
            assertEquals(List.of(), this.render().sprites);
            assertEquals(new Rect(0, 0, 28, 20), this.button.bounds());
        }
    }

    @Nested
    class Links {
        private Button link;

        private NullCanvas render() {
            final NullCanvas canvas = new NullCanvas();
            ButtonVariantsTest.this.test.host().render(canvas);
            return canvas;
        }

        private void show() {
            this.link = ButtonVariantsTest.this.ui.button("Terms").setLink(true);
            ButtonVariantsTest.this.show(this.link);
        }

        @Test
        void aLinkDrawsOnlyItsCaptionAndNeedsNoPadding() {
            this.show();
            assertTrue(this.link.isLink());
            assertEquals(new Rect(0, 0, 30, 9), this.link.bounds());
            final NullCanvas canvas = this.render();
            assertEquals(List.of(), canvas.fills());
            assertEquals(1, canvas.texts().size());
            assertFalse(canvas.texts().getFirst().style().underline());
        }

        @Test
        void aLinkUnderlinesWhileHovered() {
            this.show();
            ButtonVariantsTest.this.test.host().pointerMoved(5, 4, Modifiers.NONE);
            assertTrue(this.render().texts().getFirst().style().underline());
            ButtonVariantsTest.this.test.host().pointerMoved(200, 90, Modifiers.NONE);
            assertFalse(this.render().texts().getFirst().style().underline());
        }

        @Test
        void aLinkPressesLikeAButtonAndCanBecomeOneAgain() {
            this.show();
            final List<String> seen = new ArrayList<>();
            this.link.onEvent(Button.PRESSED, event -> seen.add("pressed"));
            ButtonVariantsTest.this.test.click(5, 4);
            assertEquals(List.of("pressed"), seen);

            this.link.setLink(false);
            ButtonVariantsTest.this.test.host().layout();
            assertEquals(new Rect(0, 0, 46, 20), this.link.bounds());
            assertFalse(this.render().fills().isEmpty());
        }

        @Test
        void aMinimumSetInCodeOutlivesSwitchingToALinkAndBack() {
            final Button before = ButtonVariantsTest.this.ui.button("Go").setMinSize(50, 10);
            before.setLink(true);
            assertEquals(50f, before.minWidth());
            assertEquals(10f, before.minHeight());
            before.setLink(false);
            assertEquals(50f, before.minWidth());

            final Button after = ButtonVariantsTest.this.ui.button("Go").setLink(true).setMinSize(40, 0);
            after.setLink(false);
            assertEquals(40f, after.minWidth());
        }

        @Test
        void theButtonsOwnMinimumFollowsTheLink() {
            final Button button = ButtonVariantsTest.this.ui.button("Go");
            assertEquals(20f, button.minWidth());
            button.setLink(true);
            assertEquals(0f, button.minWidth());
            button.setLink(false);
            assertEquals(20f, button.minWidth());
        }
    }
}
