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
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.collection.TableView;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.GridLength;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Edge cases of the collection controls: offsets set before a layout, selections across changed items, handlers that
 * change a control while it reports, and callbacks that throw.
 */
class CollectionEdgeCaseTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();
    private final AbsolutePanel panel = this.ui.absolutePanel();

    private static List<Integer> numbers(final int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @Nested
    class ScrollOffsets {
        private ListView<Integer> list(final int count) {
            final ListView<Integer> list = CollectionEdgeCaseTest.this.ui.<Integer>listView().setItems(numbers(count));
            list.setSize(100, 100);
            return list;
        }

        @Test
        void anOffsetSetBeforeTheFirstLayoutIsKept() {
            final ListView<Integer> list = this.list(50);
            list.setScrollY(300);
            assertEquals(300f, list.scrollY());
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            assertEquals(300f, list.scrollY());
        }

        @Test
        void anOffsetSetRightAfterNewItemsIsKept() {
            final ListView<Integer> list = this.list(10);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            list.setItems(numbers(50));
            list.setScrollY(500);
            assertEquals(500f, list.scrollY());
            CollectionEdgeCaseTest.this.test.host().layout();
            assertEquals(500f, list.scrollY());
        }

        @Test
        void anOffsetIsClampedToTheRowsAtOnceOnceLaidOut() {
            final ListView<Integer> list = this.list(10);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(list, 0, 0));
            list.setScrollY(1000);
            assertEquals(100f, list.scrollY());
            list.setScrollY(-5);
            assertEquals(0f, list.scrollY());
        }

        @Test
        void aTableKeepsAnOffsetSetBeforeItsFirstLayout() {
            final TableView<Integer> table = CollectionEdgeCaseTest.this.ui.<Integer>tableView()
                    .addTextColumn("N", GridLength.weighted(1f), n -> n)
                    .setItems(numbers(50));
            table.setSize(100, 120);
            table.setScrollY(240);
            CollectionEdgeCaseTest.this.test.show(CollectionEdgeCaseTest.this.panel.add(table, 0, 0));
            assertEquals(240f, table.scrollY());
        }
    }
}
