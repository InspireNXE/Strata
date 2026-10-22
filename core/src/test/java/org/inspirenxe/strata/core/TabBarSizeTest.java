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
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The size a tab bar asks for. Text in the test platform is 6 units per code point, and a tab adds 16 units of
 * padding at the sides.
 */
class TabBarSizeTest {
    private final TestHost test = new TestHost(400, 100);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aBarWithNoWidthGivesEveryTabRoomForTheWidestCaption() {
        // "Three" needs 30 + 16; tabs share the bar equally, so each of the three needs 46
        final TabBar tabs = this.ui.tabBar().addTab("One").addTab("Two").addTab("Three");
        this.test.show(this.ui.absolutePanel().add(tabs, 0, 0));

        assertEquals(46 * 3, tabs.bounds().width());
    }

    @Test
    void spacingIsAddedBetweenTheShares() {
        final TabBar tabs = this.ui.tabBar().addTab("One").addTab("Three").setSpacing(4);
        this.test.show(this.ui.absolutePanel().add(tabs, 0, 0));

        assertEquals(46 * 2 + 4, tabs.bounds().width());
    }
}
