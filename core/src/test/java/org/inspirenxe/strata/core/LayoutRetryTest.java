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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.state.State;
import org.inspirenxe.strata.api.text.Text;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Layout tries again after a hook throws, and follows handlers and content added after the first layout.
 */
class LayoutRetryTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aMeasureThatThrowsIsTriedAgainOnTheNextLayout() {
        final Flaky flaky = new Flaky(this.ui);
        this.test.show(this.ui.absolutePanel().add(flaky, 0, 0));
        flaky.failing = true;
        flaky.grow(60);

        assertThrows(IllegalStateException.class, () -> this.test.host().layout());
        flaky.failing = false;
        this.test.host().layout();

        assertEquals(60, flaky.bounds().width());
    }

    @Test
    void aMeasuredHandlerAddedAfterLayoutTakesEffect() {
        final Box box = new Box(this.ui, 30, 10);
        this.test.show(this.ui.absolutePanel().add(box, 0, 0));

        box.onEvent(Component.MEASURED, event -> event.setWidth(77));
        this.test.host().layout();

        assertEquals(77, box.bounds().width());
    }

    @Test
    void contentAddedToAnAttachedCompositeReportsPastItWhileItAttaches() {
        final Frame frame = new Frame(this.ui);
        final AbsolutePanel outer = this.ui.absolutePanel().add(frame, 0, 0);
        this.test.show(outer);
        final List<Boolean> heard = new ArrayList<>();
        outer.onEvent(Toggle.TOGGLED, event -> heard.add(event.value()));
        final CheckBox box = this.ui.checkBox(Text.of("Agree"));
        box.bind(Toggle.SELECTED, State.of(true));

        frame.put(box);

        assertEquals(List.of(true), heard);
    }
}
