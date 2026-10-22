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
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Small additions that complete existing API: a way to clear a tooltip without a cast, the no-argument cell label,
 * and a custom component whose every {@link Component} method is final, as its Javadoc says.
 */
class ApiCompletionTest {
    private final TestHost test = new TestHost(200, 200);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void clearTooltipRemovesTheTooltip() {
        final Box box = new Box(this.ui, 10, 10).setTooltip("Hint");
        assertTrue(box.tooltip().isPresent());

        assertSame(box, box.clearTooltip());

        assertEquals(Optional.empty(), box.tooltip());
    }

    @Test
    void cellLabelWithNoArgumentsHasNoText() {
        final Label label = this.ui.cellLabel();

        assertTrue(label.text().isEmpty());
        assertEquals(this.ui.cellLabel("").margin(), label.margin());
    }

    @Test
    void everyComponentMethodIsFinalOnACustomComponent() throws NoSuchMethodException {
        final List<String> overridable = new ArrayList<>();
        for (final Method method : Component.class.getMethods()) {
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            final Method own = CustomComponent.class.getMethod(method.getName(), method.getParameterTypes());
            if (!Modifier.isFinal(own.getModifiers())) {
                overridable.add(method.getName());
            }
        }

        assertEquals(List.of(), overridable);
    }
}
