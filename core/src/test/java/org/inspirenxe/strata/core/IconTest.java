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
import org.inspirenxe.strata.api.control.attribute.text.HasIcon;
import org.inspirenxe.strata.api.control.pane.NavigationItem;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The icon attribute: one set of methods on buttons and navigation items, and how an item's icon fits its slot. Text
 * in the test platform is 9 units per line, so an item's icon slot is 9 by 9.
 */
class IconTest {
    private static final SpriteId ICON = SpriteId.of("example", "icon/market");

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void oneHelperWorksForButtonsAndEntries() {
        final List<HasIcon<?>> all = List.of(this.ui.button("Ana"), this.ui.dropDownButton("Ben"),
                this.ui.navigationItem("Cleo"));

        for (final HasIcon<?> each : all) {
            assertTrue(each.iconSprite().isEmpty(), each + " starts without an icon");
            assertEquals(16f, each.iconWidth());
            assertEquals(16f, each.iconHeight());
            each.setIcon(ICON).setIconSize(12f, 10f);
            assertEquals(ICON, each.iconSprite().orElseThrow());
            assertTrue(each.iconVisual().isEmpty());
            assertEquals(12f, each.iconWidth());
            each.clearIcon();
            assertTrue(each.iconSprite().isEmpty());
            assertEquals(10f, each.iconHeight(), "the size stays for the next icon");
            assertThrows(IllegalArgumentException.class, () -> each.setIconSize(-1f, 4f));
        }
    }

    @Test
    void anItemIconLargerThanItsSlotIsScaledDownAndASmallerOneIsCentered() {
        final NavigationItem item = this.ui.navigationItem("Market").setIcon(ICON);
        this.test.show(this.ui.navigationView().addItem(item));

        final String large = this.iconLine();
        assertTrue(large.endsWith(" 9x9 #FFFFFFFF"), "16 by 16 is scaled down to the line: " + large);

        item.setIconSize(4f, 6f);
        this.test.host().layout();
        final String small = this.iconLine();
        assertTrue(small.contains(" 4x6 "), "a size that fits is kept: " + small);
        final float largeX = this.x(large);
        final float smallX = this.x(small);
        assertEquals(largeX + 2.5f, smallX, "centered in the slot");
    }

    private String iconLine() {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final List<String> lines = log.lines("sprite " + ICON + " ");
        assertEquals(1, lines.size(), log.lines().toString());
        return lines.getFirst();
    }

    private float x(final String line) {
        return Float.parseFloat(line.split(" ")[2].split(",")[0]);
    }
}
