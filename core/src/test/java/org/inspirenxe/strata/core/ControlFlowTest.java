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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.collection.ComboBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Controls whose parts sit at a side, right to left (design 4.49). With the test platform every code point is 6
 * units wide.
 */
class ControlFlowTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    /**
     * Shows a component stretched across the top of a right-to-left column.
     */
    private <C extends Component<C>> C show(final C component) {
        component.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final StackPanel column = this.ui.stackPanel().add(component);
        column.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        this.test.show(column);
        return component;
    }

    private DrawLog draw() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas;
    }

    /**
     * The x a text was drawn at.
     */
    private static float textX(final DrawLog log, final String text) {
        final String line = log.lines("text " + text + " ").get(0);
        final String at = line.substring(("text " + text + " ").length());
        return Float.parseFloat(at.substring(0, at.indexOf(',')));
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aLabelPutsItsLinesAtTheStart() {
        this.show(this.ui.label("Ana"));
        assertEquals(200f - 18f, textX(this.draw(), "Ana"));
    }

    @Test
    void aLabelAlignedRightPutsItsLinesAtTheEnd() {
        this.show(this.ui.label("Ana")).setTextAlignment(HorizontalAlignment.RIGHT);
        assertEquals(0f, textX(this.draw(), "Ana"));
    }

    @Test
    void aCheckBoxHasItsBoxAtTheRight() {
        final CheckBox box = this.show(this.ui.checkBox("Ben"));
        final float x = textX(this.draw(), "Ben");
        // The caption ends a gap before the box, which ends at the right edge
        assertTrue(x + 18f < box.bounds().right(), "caption at " + x);
        assertTrue(x > box.bounds().right() - 18f - 40f, "caption at " + x);
    }

    @Test
    void aButtonPutsItsIconAfterTheCaption() {
        final Button button = this.show(this.ui.button("Cleo")).setIcon(SpriteId.of("test", "icon"));
        final DrawLog log = this.draw();
        final float text = textX(log, "Cleo");
        final String sprite = log.lines("sprite test:icon ").get(0);
        final float icon = Float.parseFloat(sprite.split(" ")[2].split(",")[0]);
        assertTrue(icon > text + 24f, "icon at " + icon + ", caption at " + text);
        assertTrue(button.bounds().contains(icon, button.bounds().y() + 1f));
    }

    @Test
    void aComboBoxPutsItsTextAtTheRightAndItsArrowAtTheLeft() {
        final ComboBox<String> combo = this.show(this.ui.<String>comboBox()).setItems(List.of("Dev"))
                .select(0);
        final float x = textX(this.draw(), "Dev");
        assertTrue(x > combo.bounds().right() - 18f - 12f, "text at " + x);
    }

    @Test
    void aSliderRisesTowardTheLeft() {
        final Slider slider = this.show(this.ui.slider());
        slider.setRange(0, 100).setValue(50);
        this.test.click(slider.bounds().right() - 2f, slider.bounds().y() + 2f);
        assertEquals(0.0, slider.value());
        this.key(Key.LEFT);
        assertTrue(slider.value() > 0.0, "value " + slider.value());
        this.key(Key.RIGHT);
        assertEquals(0.0, slider.value());
    }

    @Test
    void aProgressBarFillsFromTheRight() {
        final ProgressBar bar = this.show(this.ui.progressBar()).setValue(0.25);
        final DrawLog log = this.draw();
        final float width = bar.bounds().width() * 0.25f;
        assertTrue(log.lines().stream().anyMatch(line -> line.startsWith("fill " + DrawLog.rect(
                new Rect(bar.bounds().right() - width, bar.bounds().y(), width, bar.bounds().height())))),
                String.join("\n", log.lines()));
    }

    @Test
    void aSpinnersButtonsSitAtTheLeft() {
        final Spinner spinner = this.show(this.ui.spinner());
        this.test.click(spinner.bounds().x() + 4f, spinner.bounds().y() + 3f);
        assertEquals(1.0, spinner.value());
    }

    @Test
    void aTextBoxStartsAtTheRightAndLeftMovesForward() {
        final TextBox box = this.show(this.ui.textBox()).setText("abc");
        final float x = textX(this.draw(), "abc");
        assertTrue(x > box.bounds().right() - 18f - 10f, "text at " + x);
        box.requestFocus();
        box.select(0, 0);
        this.key(Key.LEFT);
        assertEquals(1, box.caret());
        this.key(Key.RIGHT);
        assertEquals(0, box.caret());
    }
}
