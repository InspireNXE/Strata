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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screens in a stack hand the engine's text input and the pointer to whichever screen is on top.
 */
class StackedScreensTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final TestPlatform platform = new TestPlatform();
    private final ScreenStack stack = StrataCore.createScreenStack(this.platform, Themes.flat());

    {
        this.stack.resize(300, 300);
    }

    @Test
    void aCoveredFieldGetsTextInputBackWhenTheScreenAboveCloses() {
        final TextBox[] below = new TextBox[1];
        this.stack.push(ui -> {
            below[0] = ui.textBox();
            return ui.stackPanel().add(below[0]);
        });
        below[0].requestFocus();
        assertNotNull(this.platform.textInputArea());

        final TextBox[] above = new TextBox[1];
        this.stack.push(ui -> {
            above[0] = ui.textBox();
            return ui.stackPanel().add(above[0]);
        });
        above[0].requestFocus();
        this.stack.pop();

        assertNotNull(this.platform.textInputArea(), "the field below has the engine's text input again");
    }

    @Test
    void aScreenWithoutAFieldTakesTextInputFromTheOneItCovers() {
        final TextBox[] below = new TextBox[1];
        this.stack.push(ui -> {
            below[0] = ui.textBox();
            return ui.stackPanel().add(below[0]);
        });
        below[0].requestFocus();

        this.stack.push(ui -> ui.stackPanel().add(ui.button("OK")));

        assertNull(this.platform.textInputArea());
    }

    @Test
    void aCoveredScreenDropsItsHover() {
        final List<String> heard = new ArrayList<>();
        this.stack.push(ui -> {
            final Box box = new Box(ui, 50, 50);
            box.onEvent(InputEvents.POINTER_EXIT, event -> heard.add("exit"));
            return ui.absolutePanel().add(box, 0, 0);
        });
        this.stack.pointerMoved(10, 10, Modifiers.NONE);

        this.stack.push(ui -> ui.absolutePanel());

        assertEquals(List.of("exit"), heard);
    }

    @Test
    void closedThatThrowsStillEmptiesTheHost() {
        this.test.host().open(new Screen() {
            @Override
            public Component<?> build(final ComponentFactory ui) {
                return ui.absolutePanel();
            }

            @Override
            public void closed() {
                throw new IllegalStateException("closed failed on purpose");
            }
        });

        assertThrows(IllegalStateException.class, () -> this.test.host().close());

        assertNull(this.test.host().root().content());
        assertTrue(this.test.host().screen().isEmpty());
    }
}
