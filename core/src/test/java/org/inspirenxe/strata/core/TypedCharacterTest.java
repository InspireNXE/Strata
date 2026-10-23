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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Modifiers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Typed characters the engine sends that are not code points.
 */
class TypedCharacterTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void invalidCodePointsAreIgnored() {
        final TextBox field = this.ui.textBox();
        this.test.show(this.ui.stackPanel().add(field));
        field.requestFocus();

        assertFalse(this.test.host().charTyped(-1, Modifiers.NONE));
        assertFalse(this.test.host().charTyped(0xD800, Modifiers.NONE));
        assertFalse(this.test.host().charTyped(0x110000, Modifiers.NONE));

        assertEquals("", field.text());
    }
}
