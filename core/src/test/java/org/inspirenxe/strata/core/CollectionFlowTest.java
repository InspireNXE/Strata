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
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.pane.ScrollBar;
import org.inspirenxe.strata.api.control.pane.SplitPane;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Lists, scroll bars and split panes right to left (design 4.49).
 */
class CollectionFlowTest {
    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    private <C extends Component<C>> C show(final C component) {
        component.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        final StackPanel column = this.ui.stackPanel().add(component);
        column.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        this.test.show(column);
        return component;
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aListKeepsItsBarAtTheLeft() {
        final List<String> items = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            items.add("Item " + i);
        }
        final ListView<String> list = this.show(this.ui.<String>listView()).setItems(items);
        list.setHeight(60);
        this.test.host().layout();
        final float before = list.scrollY();
        // A press on the track below the thumb, at the left, pages down
        this.test.host().pointerButton(list.bounds().x() + 2f, list.bounds().bottom() - 3f, MouseButton.LEFT, true,
                Modifiers.NONE);
        this.test.host().pointerButton(list.bounds().x() + 2f, list.bounds().bottom() - 3f, MouseButton.LEFT, false,
                Modifiers.NONE);
        assertTrue(list.scrollY() > before, "scrolled to " + list.scrollY());
    }

    @Test
    void aHorizontalScrollBarStartsAtTheRight() {
        final ScrollBar bar = this.show(this.ui.scrollBar(Orientation.HORIZONTAL));
        bar.setRange(0, 100).setVisibleAmount(10);
        this.test.host().layout();
        // A press near the left end of the track is toward the end
        this.test.click(bar.bounds().x() + 2f, bar.bounds().y() + 2f);
        assertTrue(bar.value() > 0.0, "value " + bar.value());
        final double paged = bar.value();
        // And one at the right end, where the thumb started, goes back toward the start
        this.test.click(bar.bounds().right() - 2f, bar.bounds().y() + 2f);
        assertTrue(bar.value() < paged, "value " + bar.value());
    }

    @Test
    void aSplitPanesFirstSideIsAtTheRight() {
        final Box first = new Box(this.ui, 10, 10).setAnchor(Anchor.FILL);
        final Box second = new Box(this.ui, 10, 10).setAnchor(Anchor.FILL);
        final SplitPane split = this.show(this.ui.splitPane(Orientation.HORIZONTAL)).setFirst(first)
                .setSecond(second).setPosition(0.25f);
        split.setHeight(50);
        this.test.host().layout();
        assertEquals(split.bounds().right(), first.bounds().right());
        assertTrue(second.bounds().x() < first.bounds().x());
        split.requestFocus();
        final float position = split.position();
        this.key(Key.LEFT);
        assertTrue(split.position() > position, split.position() + " after " + position);
    }
}
