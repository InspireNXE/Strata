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
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.RadioButton;
import org.inspirenxe.strata.api.control.button.RadioGroup;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Taking buttons out of a radio group, and what the group reports for it (design 4.57).
 */
class RadioGroupRemovalTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final RadioGroup<String> group = this.ui.radioGroup();
    private final RadioButton ana = this.group.add("Ana", "ana");
    private final RadioButton ben = this.group.add("Ben", "ben");
    private final RadioButton cleo = this.group.add("Cleo", "cleo");
    private final List<String> heard = new ArrayList<>();

    RadioGroupRemovalTest() {
        this.group.onEvent(SingleSelection.SELECTED, event -> this.heard.add(event.previous() + "->" + event.value()));
        for (final RadioButton button : this.group.buttons()) {
            button.onEvent(Toggle.TOGGLED, event -> this.heard.add(button.text().plain() + " " + event.value()));
        }
    }

    @Test
    void removingTheSelectedButtonClearsTheSelection() {
        this.group.select(1);
        this.heard.clear();
        this.group.remove(this.ben);
        assertEquals(List.of("Ben false", "1->-1"), this.heard);
        assertEquals(List.of(this.ana, this.cleo), this.group.buttons());
        assertEquals(-1, this.group.selectedIndex());
        assertFalse(this.ben.isSelected());
    }

    @Test
    void removingAButtonBeforeTheSelectedOneReportsItsNewIndex() {
        this.group.select(2);
        this.heard.clear();
        this.group.remove(this.ana);
        assertEquals(List.of("2->1"), this.heard);
        assertEquals(1, this.group.selectedIndex());
        assertEquals(Optional.of("cleo"), this.group.selected());
        this.heard.clear();
        this.group.remove(this.ben);
        assertEquals(List.of("1->0"), this.heard);
        assertEquals("cleo", this.group.value(0));
    }

    @Test
    void removingAButtonAfterTheSelectedOneReportsNothing() {
        this.group.select(0);
        this.heard.clear();
        this.group.remove(this.cleo);
        assertEquals(List.of(), this.heard);
        assertEquals(0, this.group.selectedIndex());
    }

    @Test
    void aRemovedButtonCanNoLongerBeSelected() {
        this.test.show(this.ui.stackPanel().add(this.ana).add(this.ben).add(this.cleo).setAnchor(Anchor.TOP_LEFT));
        this.group.remove(this.ben);
        this.ben.setSelected(true);
        assertFalse(this.ben.isSelected());
        assertEquals(-1, this.group.selectedIndex());
        this.test.click(this.ben.bounds().x() + 4f, this.ben.bounds().y() + 4f);
        assertFalse(this.ben.isSelected());
        this.ana.requestFocus();
        this.group.select(0);
        this.test.host().key(Key.DOWN, KeyAction.PRESS, Modifiers.NONE);
        assertTrue(this.cleo.isSelected());
    }

    @Test
    void clearTakesEveryButtonOutAndReportsOnce() {
        this.group.select(2);
        this.heard.clear();
        this.group.clear();
        assertEquals(List.of("Cleo false", "2->-1"), this.heard);
        assertEquals(List.of(), this.group.buttons());
        this.heard.clear();
        this.group.clear();
        assertEquals(List.of(), this.heard);
        final RadioButton dev = this.group.add("Dev", "dev");
        this.group.select(0);
        assertTrue(dev.isSelected());
    }
}
