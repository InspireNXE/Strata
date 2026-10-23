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
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A radio group as one tab stop: the first enabled button while none is selected, and a button whose tabbable flag
 * code set keeps it (design 4.41).
 */
class RadioTabStopsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final RadioGroup<String> group = this.ui.radioGroup();
    private final RadioButton ana = this.group.add("Ana", "ana");
    private final RadioButton ben = this.group.add("Ben", "ben");
    private final RadioButton cleo = this.group.add("Cleo", "cleo");

    private void show() {
        this.test.show(this.ui.stackPanel().add(this.ana).add(this.ben).add(this.cleo).setAnchor(Anchor.TOP_LEFT));
    }

    @Test
    void theFirstEnabledButtonIsTheStopWhileNoneIsSelected() {
        this.ana.setEnabled(false);
        this.show();
        assertEquals(List.of(this.ben), this.test.tabOrder());
        this.ana.setEnabled(true);
        assertEquals(List.of(this.ana), this.test.tabOrder());
    }

    @Test
    void theSelectedButtonIsTheStop() {
        this.show();
        this.group.select(2);
        assertEquals(List.of(this.cleo), this.test.tabOrder());
    }

    @Test
    void aButtonWhoseFlagCodeSetKeepsIt() {
        this.show();
        this.cleo.setTabbable(true);
        this.group.select(1);
        assertEquals(List.of(this.ben, this.cleo), this.test.tabOrder());
        this.group.select(0);
        assertEquals(List.of(this.ana, this.cleo), this.test.tabOrder());
    }
}
