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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.IconPlacement;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.window.Window;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The new parts of section 4.57 mirrored while their control runs right to left (design 4.49).
 */
class GapsRightToLeftTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private List<String> render() {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log.lines();
    }

    @Test
    void anIconPlacedAtTheStartSitsAtTheRightEnd() {
        final Button button = this.ui.button("Next").setIcon(SpriteId.of("example", "arrow"))
                .setIconPlacement(IconPlacement.START).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        this.test.show(this.ui.absolutePanel().add(button, 10, 10));
        this.test.host().layout();
        final List<String> lines = this.render();
        final String sprite = lines.stream().filter(line -> line.startsWith("sprite")).findFirst().orElseThrow();
        final String text = lines.stream().filter(line -> line.startsWith("text")).findFirst().orElseThrow();
        final float iconX = Float.parseFloat(sprite.split(" ")[2].split(",")[0]);
        final float textX = Float.parseFloat(text.split(" ")[2].split(",")[0]);
        assertTrue(iconX > textX, lines.toString());
    }

    @Test
    void aDropDownBoxDrawsAndTakesItsArrowAtTheLeft() {
        final TextBox box = this.ui.textBox().setDropDown(true)
                .setSuggestionProvider(TextBox.SuggestionProvider.words(List.of("Ana", "Ben")))
                .setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        box.setWidth(120);
        this.test.show(this.ui.absolutePanel().add(box, 10, 10));
        this.test.host().layout();
        // The arrow's widest line is 7 units at the left, inside the padding of 4
        assertTrue(this.render().stream().anyMatch(line -> line.startsWith("fill 14,") && line.contains(" 7x1 ")),
                this.render().toString());
        final float y = box.bounds().y() + box.bounds().height() / 2f;
        this.test.host().pointerButton(16, y, MouseButton.LEFT, true, Modifiers.NONE);
        this.test.host().pointerButton(16, y, MouseButton.LEFT, false, Modifiers.NONE);
        assertTrue(this.render().stream().anyMatch(line -> line.startsWith("text Ana")), this.render().toString());
    }

    @Test
    void aSlidersTicksRunFromTheRight() {
        final Slider slider = this.ui.slider().setRange(0, 100).setTicks(100, 0)
                .setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        slider.setSize(108, 20);
        this.test.show(this.ui.absolutePanel().add(slider, 10, 10));
        this.test.host().layout();
        final List<String> lines = this.render();
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("fill 113,26 1x4")), lines.toString());
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("fill 13,26 1x4")), lines.toString());
    }

    @Test
    void aWindowsButtonsSitRightOfItsCloseButton() {
        final Window window = this.ui.window("Plan").setMaximizable(true)
                .setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        window.setSize(120, 80);
        this.test.show(this.ui.absolutePanel().add(window, 10, 10));
        this.test.host().layout();
        // The close button is the leftmost, and the maximize button's outline the next one in
        final List<String> lines = this.render();
        assertTrue(lines.stream().anyMatch(line -> line.startsWith("fill ") && line.contains(" 7x1 ")
                && Float.parseFloat(line.split(" ")[1].split(",")[0]) < 40f), lines.toString());
    }
}
