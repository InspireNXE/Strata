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
import org.inspirenxe.strata.api.command.Command;
import org.inspirenxe.strata.api.component.registry.ComponentId;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.IconPlacement;
import org.inspirenxe.strata.api.control.button.ToggleButton;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The toggle button, and where buttons place their icons (design 4.57).
 */
class ToggleButtonTest {
    private static final SpriteId ICON = SpriteId.of("example", "bold");

    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private DrawLog render() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas;
    }

    private void clickCenter(final ToggleButton button) {
        final Rect bounds = button.bounds();
        this.test.click(bounds.x() + bounds.width() / 2f, bounds.y() + bounds.height() / 2f);
    }

    private static String fill(final Rect bounds, final int argb) {
        return "fill " + DrawLog.rect(bounds) + " " + String.format("#%08X", argb);
    }

    @Nested
    class Toggling {
        @Test
        void aClickFlipsItThroughTheToggleRequest() {
            final ToggleButton bold = ToggleButtonTest.this.ui.toggleButton("Bold")
                    .onEvent(Toggle.TOGGLE, event -> ToggleButtonTest.this.heard.add("ask " + event.value()))
                    .onEvent(Toggle.TOGGLED, event -> ToggleButtonTest.this.heard.add("now " + event.value()));
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(bold, 10, 10));
            ToggleButtonTest.this.clickCenter(bold);
            assertTrue(bold.isSelected());
            ToggleButtonTest.this.clickCenter(bold);
            assertFalse(bold.isSelected());
            assertEquals(List.of("ask true", "now true", "ask false", "now false"), ToggleButtonTest.this.heard);
        }

        @Test
        void aCanceledRequestKeepsItsState() {
            final ToggleButton bold = ToggleButtonTest.this.ui.toggleButton("Bold")
                    .onEvent(Toggle.TOGGLE, event -> event.cancel());
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(bold, 10, 10));
            ToggleButtonTest.this.clickCenter(bold);
            assertFalse(bold.isSelected());
        }

        @Test
        void spaceFlipsItWhileFocusedAndCodeReportsWithoutAsking() {
            final ToggleButton grid = ToggleButtonTest.this.ui.toggleButton("Grid")
                    .onEvent(Toggle.TOGGLE, event -> ToggleButtonTest.this.heard.add("ask"))
                    .onEvent(Toggle.TOGGLED, event -> ToggleButtonTest.this.heard.add("now " + event.value()));
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(grid, 10, 10));
            grid.requestFocus();
            ToggleButtonTest.this.test.host().key(Key.SPACE, KeyAction.PRESS, Modifiers.NONE);
            assertTrue(grid.isSelected());
            grid.setSelected(false);
            grid.toggle();
            assertEquals(List.of("ask", "now true", "now false", "now true"), ToggleButtonTest.this.heard);
        }

        @Test
        void aDisabledButtonIgnoresTheUser() {
            final ToggleButton bold = ToggleButtonTest.this.ui.toggleButton("Bold");
            bold.setEnabled(false);
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(bold, 10, 10));
            ToggleButtonTest.this.clickCenter(bold);
            bold.toggle();
            assertFalse(bold.isSelected());
            bold.setSelected(true);
            assertTrue(bold.isSelected());
        }

        @Test
        void aToggleCommandDrivesItBothWays() {
            final State<Boolean> on = State.of(false);
            final State<Boolean> enabled = State.of(true);
            final Command.Toggle command = Command.toggle("Bold", on).withEnabled(enabled);
            final ToggleButton bold = ToggleButtonTest.this.ui.toggleButton().setCommand(command);
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(bold, 10, 10));
            assertEquals("Bold", bold.text().plain());
            on.set(true);
            ToggleButtonTest.this.test.host().flush();
            assertTrue(bold.isSelected());
            ToggleButtonTest.this.clickCenter(bold);
            assertFalse(on.get());
            enabled.set(false);
            ToggleButtonTest.this.test.host().flush();
            assertFalse(bold.isEnabled());
            bold.setCommand(null);
            on.set(true);
            ToggleButtonTest.this.test.host().flush();
            assertFalse(bold.isSelected());
        }

        @Test
        void itIsRegisteredAsStrataToggleButton() {
            assertInstanceOf(ToggleButton.class,
                    ToggleButtonTest.this.ui.create(ComponentId.of("strata", "toggle_button")));
        }
    }

    @Nested
    class Faces {
        @Test
        void anOnButtonDrawsTheSelectedFace() {
            final ToggleButton bold = ToggleButtonTest.this.ui.toggleButton("Bold");
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(bold, 10, 10));
            ToggleButtonTest.this.render();
            final Paint selected = Themes.flat().paint(ToggleButton.STYLE_SELECTED).orElseThrow();
            final Paint normal = Themes.flat().paint(Button.STYLE_NORMAL).orElseThrow();
            assertTrue(ToggleButtonTest.this.render().lines().contains(fill(bold.bounds(), color(normal))));
            bold.setSelected(true);
            assertTrue(ToggleButtonTest.this.render().lines().contains(fill(bold.bounds(), color(selected))));
        }

        @Test
        void withoutSelectedKeysTheFaceFallsBackToPressed() {
            final Theme theme = Theme.builder("bare")
                    .paint(Button.STYLE_NORMAL, Paint.solid(0xFF101010))
                    .paint(Button.STYLE_HOVERED, Paint.solid(0xFF202020))
                    .paint(Button.STYLE_PRESSED, Paint.solid(0xFF303030))
                    .paint(Button.STYLE_DISABLED, Paint.solid(0xFF404040))
                    .build();
            final TestHost bare = new TestHost(new TestPlatform(), theme, 200, 200);
            final ToggleButton bold = bare.factory().toggleButton("Bold").setSelected(true);
            bare.show(bare.factory().absolutePanel().add(bold, 10, 10));
            final DrawLog log = new DrawLog();
            bare.host().render(log);
            assertTrue(log.lines().contains(fill(bold.bounds(), 0xFF303030)));
        }

        private static int color(final Paint paint) {
            return ((Paint.Solid) paint).argb();
        }
    }

    @Nested
    class IconPlacements {
        private Button show(final IconPlacement placement) {
            final Button button = ToggleButtonTest.this.ui.button("Next").setIcon(ICON).setIconPlacement(placement);
            ToggleButtonTest.this.test.show(ToggleButtonTest.this.ui.absolutePanel().add(button, 10, 10));
            ToggleButtonTest.this.test.host().layout();
            return button;
        }

        private float[] iconAndText() {
            final DrawLog log = ToggleButtonTest.this.render();
            final String sprite = log.lines("sprite").getFirst().split(" ")[2];
            final String text = log.lines("text").getFirst().split(" ")[2];
            final String[] at = sprite.split(",");
            final String[] textAt = text.split(",");
            return new float[] {Float.parseFloat(at[0]), Float.parseFloat(at[1]), Float.parseFloat(textAt[0]),
                    Float.parseFloat(textAt[1])};
        }

        @Test
        void startPutsTheIconBeforeTheCaption() {
            final Button button = this.show(IconPlacement.START);
            assertEquals(IconPlacement.START, ToggleButtonTest.this.ui.button("x").iconPlacement());
            final float[] at = this.iconAndText();
            assertEquals(at[0] + 16f + 4f, at[2], 0.01f);
            assertEquals(button.bounds().y() + (button.bounds().height() - 16f) / 2f, at[1], 0.01f);
        }

        @Test
        void endPutsTheIconAfterTheCaptionAndKeepsTheWidth() {
            final float width = this.show(IconPlacement.START).desiredWidth();
            final Button button = this.show(IconPlacement.END);
            assertEquals(width, button.desiredWidth(), 0.01f);
            final float[] at = this.iconAndText();
            assertTrue(at[0] > at[2]);
            assertEquals(button.bounds().right() - 8f - 16f, at[0], 0.01f);
            assertEquals(button.bounds().x() + 8f, at[2], 0.01f);
        }

        @Test
        void topStacksTheIconAboveTheCaption() {
            final float flatHeight = this.show(IconPlacement.START).desiredHeight();
            final Button button = this.show(IconPlacement.TOP);
            assertTrue(button.desiredHeight() > flatHeight);
            final float[] at = this.iconAndText();
            assertEquals(button.bounds().y() + 5f, at[1], 0.01f);
            assertTrue(at[3] > at[1] + 16f);
            assertEquals(button.bounds().x() + (button.bounds().width() - 16f) / 2f, at[0], 0.01f);
        }

        @Test
        void bottomStacksTheIconBelowTheCaption() {
            final Button button = this.show(IconPlacement.BOTTOM);
            final float[] at = this.iconAndText();
            assertTrue(at[1] > at[3]);
            assertEquals(button.bounds().bottom() - 5f - 16f, at[1], 0.01f);
        }

        @Test
        void aToggleButtonAndADropDownButtonTakeThePlacementToo() {
            assertEquals(IconPlacement.TOP,
                    ToggleButtonTest.this.ui.toggleButton("Bold").setIconPlacement(IconPlacement.TOP).iconPlacement());
            assertEquals(IconPlacement.END,
                    ToggleButtonTest.this.ui.dropDownButton("Open").setIconPlacement(IconPlacement.END).iconPlacement());
        }
    }
}
