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

import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CheckState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A check box's three states, the user's changes between them, and the events each change fires.
 */
class CheckStateTest {
    private final List<String> heard = new ArrayList<>();
    private TestHost test;
    private CheckBox box;

    @BeforeEach
    void show() {
        this.test = new TestHost(200, 100);
        this.box = this.test.factory().checkBox("All")
                .onEvent(CheckBox.CHANGE, event -> this.heard.add("change " + event.value()))
                .onEvent(Toggle.TOGGLE, event -> this.heard.add("toggle " + event.value()))
                .onEvent(CheckBox.CHANGED, event -> this.heard.add(event.previous() + "->" + event.value()))
                .onEvent(Toggle.TOGGLED, event -> this.heard.add("toggled " + event.value()));
        this.test.show(this.test.factory().absolutePanel().add(this.box, 0, 0));
    }

    private void click() {
        this.test.click(this.box.bounds().x() + 2, this.box.bounds().y() + 2);
    }

    @Test
    void codeSetsMixedAndOnlyCheckingTogglesTheSelection() {
        this.box.setState(CheckState.MIXED);
        assertFalse(this.box.isSelected());
        this.box.setSelected(true);
        assertEquals(List.of("UNCHECKED->MIXED", "MIXED->CHECKED", "toggled true"), this.heard);
    }

    @Test
    void aClickChecksAMixedBox() {
        this.box.setState(CheckState.MIXED);
        this.heard.clear();
        this.click();
        assertEquals(CheckState.CHECKED, this.box.state());
        assertEquals(List.of("change CHECKED", "toggle true", "MIXED->CHECKED", "toggled true"), this.heard);
    }

    @Test
    void withMixedAllowedClicksCycleThroughAllThree() {
        this.box.setAllowsMixed(true);
        this.click();
        this.click();
        assertEquals(CheckState.MIXED, this.box.state());
        this.heard.clear();
        this.click();
        assertEquals(CheckState.UNCHECKED, this.box.state());
        assertEquals(List.of("change UNCHECKED", "MIXED->UNCHECKED"), this.heard,
                "going from mixed to empty does not change the selection, so no toggle fires");
    }

    @Test
    void aCanceledChangeKeepsTheBox() {
        this.box.onEvent(CheckBox.CHANGE, event -> event.cancel());
        this.click();
        assertEquals(CheckState.UNCHECKED, this.box.state());
        assertTrue(this.heard.stream().noneMatch(line -> line.startsWith("toggle")));
    }

    @Test
    void aChangeCanBeRewrittenToMixed() {
        this.box.onEvent(CheckBox.CHANGE, event -> event.setValue(CheckState.MIXED));
        this.click();
        assertEquals(CheckState.MIXED, this.box.state());
    }
}
