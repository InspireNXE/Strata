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
import org.inspirenxe.strata.api.control.collection.TreeView;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Menus, popups, tabs and trees right to left (design 4.49).
 */
class PaneFlowTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final StackPanel root = this.ui.stackPanel();

    PaneFlowTest() {
        this.root.setAnchor(Anchor.FILL).setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
    }

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Test
    void aPopupMeetsItsAnchorAtTheRight() {
        this.test.show(this.root);
        final Menu menu = this.ui.menu().addItem("Ana", () -> { });
        menu.open(new Rect(200, 50, 40, 10));
        this.test.host().layout();
        assertEquals(240f, menu.bounds().right());
    }

    @Test
    void aSubmenuOpensToTheLeft() {
        this.test.show(this.root);
        final Menu sub = this.ui.menu().addItem("Ben", () -> { });
        final Menu menu = this.ui.menu().addSubmenu("More", sub);
        menu.openAt(250, 20);
        this.test.host().layout();
        menu.requestFocus();
        this.key(Key.DOWN);
        // Right to left, Left opens the submenu
        this.key(Key.LEFT);
        this.test.host().layout();
        assertTrue(sub.isOpen());
        assertTrue(sub.bounds().right() <= menu.bounds().x() + 0.5f, sub.bounds() + " beside " + menu.bounds());
        this.key(Key.RIGHT);
        assertFalse(sub.isOpen());
    }

    @Test
    void aMenusTextStartsAtTheRight() {
        this.test.show(this.root);
        final Menu menu = this.ui.menu().addItem("Cleo", () -> { });
        menu.openAt(250, 20);
        this.test.host().layout();
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final String line = log.lines("text Cleo ").get(0);
        final float x = Float.parseFloat(line.split(" ")[2].split(",")[0]);
        // The text ends where the check column starts, at the right
        assertTrue(x + 24f > menu.bounds().right() - 20f, "text at " + x + " in " + menu.bounds());
    }

    @Test
    void leftSelectsTheNextTab() {
        final TabBar tabs = this.ui.tabBar().addTab("Ana").addTab("Ben").addTab("Cleo");
        this.root.add(tabs);
        this.test.show(this.root);
        assertTrue(tabs.bounds().width() > 0f);
        tabs.requestFocus();
        this.key(Key.LEFT);
        assertEquals(1, tabs.selectedIndex());
        this.key(Key.RIGHT);
        assertEquals(0, tabs.selectedIndex());
    }

    @Test
    void leftExpandsATreeItem() {
        final Map<String, List<String>> children = Map.of("Ana", List.of("Ben"));
        final TreeView<String> tree = this.ui.<String>treeView()
                .setChildren(item -> children.getOrDefault(item, List.of()));
        tree.setItems(List.of("Ana"));
        tree.setHeight(100);
        this.root.add(tree);
        this.test.show(this.root);
        tree.select(0);
        tree.requestFocus();
        this.test.host().layout();
        this.key(Key.LEFT);
        assertTrue(tree.isExpanded("Ana"));
        this.key(Key.RIGHT);
        assertFalse(tree.isExpanded("Ana"));
    }
}
