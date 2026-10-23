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
import org.inspirenxe.strata.api.control.attribute.item.ItemDragging;
import org.inspirenxe.strata.api.control.attribute.item.ItemDragging.ItemMove;
import org.inspirenxe.strata.api.control.attribute.item.SelectionMode;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dragging a list's rows to reorder them or drop them elsewhere (design 4.48).
 */
class ListDragTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final ScreenHostImpl host = this.test.host();
    private final ListView<String> list = this.ui.<String>listView().setItems(List.of("ana", "ben", "cleo", "dev", "eve"))
            .setDragTransfer(rows -> Transfer.text(String.join("\n", rows)));
    private final Box bin = new Box(this.ui, 60, 60);
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.list, 0, 0).add(this.bin, 200, 100);
    private final List<ItemMove> moves = new ArrayList<>();

    ListDragTest() {
        this.list.setSize(100, 200);
        this.panel.setSize(300, 200);
        this.list.onEvent(ItemDragging.MOVE_ITEMS, event -> this.moves.add(event.value()));
        this.test.show(this.panel);
    }

    private void drag(final float fromY, final float toX, final float toY) {
        this.drag(fromY, toX, toY, Modifiers.NONE);
    }

    private void drag(final float fromY, final float toX, final float toY, final Modifiers modifiers) {
        this.host.pointerMoved(50, fromY, modifiers);
        this.host.pointerButton(50, fromY, MouseButton.LEFT, true, modifiers);
        this.host.pointerMoved(50, fromY + 8, modifiers);
        this.host.pointerMoved(toX, toY, modifiers);
        this.host.pointerButton(toX, toY, MouseButton.LEFT, false, modifiers);
        this.host.layout();
    }

    @Test
    void aMoveTakesTheRowsOutAndPutsThemInFrontOfARow() {
        assertEquals(List.of("ben", "cleo", "ana", "dev"), new ItemMove(List.of(0), 3).apply(List.of("ana", "ben",
                "cleo", "dev")));
        assertEquals(List.of("ben", "dev", "ana", "cleo"), new ItemMove(List.of(0, 2), 4).apply(List.of("ana", "ben",
                "cleo", "dev")));
        assertEquals(List.of("dev", "ana", "ben", "cleo"), new ItemMove(List.of(3), 0).apply(List.of("ana", "ben",
                "cleo", "dev")));
        assertThrows(IllegalArgumentException.class, () -> new ItemMove(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemMove(List.of(2, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemMove(List.of(1, 1), 0));
        assertThrows(IllegalArgumentException.class, () -> new ItemMove(List.of(1), -1));
        assertThrows(IndexOutOfBoundsException.class, () -> new ItemMove(List.of(4), 0).apply(List.of("a")));
    }

    @Test
    void draggingARowWithinTheListReordersIt() {
        this.drag(10, 50, 61);

        assertEquals(List.of(new ItemMove(List.of(0), 3)), this.moves);
        assertEquals(List.of("ben", "cleo", "ana", "dev", "eve"), this.list.items());
        assertEquals(Optional.of("ana"), this.list.selected());
        assertEquals(2, this.list.selectedIndex());
    }

    @Test
    void aCanceledOrRewrittenMoveIsWhatHappens() {
        this.list.onEvent(ItemDragging.MOVE_ITEMS, event -> event.cancel());
        this.drag(10, 50, 61);
        assertEquals(List.of("ana", "ben", "cleo", "dev", "eve"), this.list.items());

        final ListView<String> other = this.ui.<String>listView().setItems(List.of("a", "b", "c"))
                .setDragTransfer(rows -> Transfer.text(rows.get(0)));
        other.onEvent(ItemDragging.MOVE_ITEMS, event -> event.setValue(new ItemMove(List.of(2), 0)));
        other.setSize(100, 100);
        this.panel.remove(this.list);
        this.panel.add(other, 0, 0);
        this.host.layout();
        this.drag(10, 50, 41);
        assertEquals(List.of("c", "a", "b"), other.items());
    }

    @Test
    void aDropWhereTheRowsAlreadyAreAsksNothing() {
        this.drag(30, 50, 33);
        this.drag(30, 50, 41);
        assertEquals(List.of(), this.moves);
        assertEquals(List.of("ana", "ben", "cleo", "dev", "eve"), this.list.items());
    }

    @Test
    void theSelectedRowsMoveTogether() {
        this.list.setSelectionMode(SelectionMode.MULTIPLE);
        this.host.pointerButton(50, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerButton(50, 10, MouseButton.LEFT, false, Modifiers.NONE);
        final Modifiers control = Modifiers.of(Modifier.CONTROL);
        this.host.pointerButton(50, 50, MouseButton.LEFT, true, control);
        this.host.pointerButton(50, 50, MouseButton.LEFT, false, control);
        assertEquals(List.of(0, 2), this.list.selectedIndexes());

        // A plain press on one of them keeps both selected, so the drag carries both
        this.test.platform().advanceMillis(1000);
        this.drag(50, 50, 99);
        assertEquals(List.of(new ItemMove(List.of(0, 2), 5)), this.moves);
        assertEquals(List.of("ben", "dev", "eve", "ana", "cleo"), this.list.items());
        assertEquals(List.of(3, 4), this.list.selectedIndexes());

        // A plain click on one of them, with no drag, selects it alone
        this.test.platform().advanceMillis(1000);
        this.host.pointerButton(50, 70, MouseButton.LEFT, true, Modifiers.NONE);
        assertEquals(List.of(3, 4), this.list.selectedIndexes());
        this.host.pointerButton(50, 70, MouseButton.LEFT, false, Modifiers.NONE);
        assertEquals(List.of(3), this.list.selectedIndexes());
    }

    @Test
    void theLineShowsWhereTheRowsWouldLand() {
        this.host.pointerMoved(50, 10, Modifiers.NONE);
        this.host.pointerButton(50, 10, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(50, 20, Modifiers.NONE);
        this.host.pointerMoved(50, 61, Modifiers.NONE);
        final DrawLog log = new DrawLog();
        this.host.render(log);

        assertTrue(log.lines().contains("fill 0,59 100x2 #FF8FB4F0"), log.lines("fill").toString());
        this.host.pointerButton(50, 61, MouseButton.LEFT, false, Modifiers.NONE);
        final DrawLog after = new DrawLog();
        this.host.render(after);
        assertFalse(after.lines().contains("fill 0,59 100x2 #FF8FB4F0"));
    }

    @Test
    void theDragImageIsTheFirstRowsCell() {
        this.host.pointerMoved(50, 30, Modifiers.NONE);
        this.host.pointerButton(50, 30, MouseButton.LEFT, true, Modifiers.NONE);
        this.host.pointerMoved(50, 50, Modifiers.NONE);
        final DrawLog log = new DrawLog();
        this.host.render(log);
        assertTrue(this.host.root().dragImage().isPresent());
        assertEquals(0.75f, this.host.root().dragImage().orElseThrow().opacity());
        assertEquals(2, log.lines("text ben").size(), log.lines("text").toString());
    }

    @Test
    void rowsDroppedElsewhereLeaveTheItemsToTheProject() {
        final List<String> dropped = new ArrayList<>();
        final List<String> done = new ArrayList<>();
        this.bin.onEvent(InputEvents.DRAG_OVER, event -> event.accept());
        this.bin.onEvent(InputEvents.DROP, event -> dropped.add(event.transfer().get(DataFormat.TEXT).orElseThrow()));
        this.list.onEvent(InputEvents.TRANSFER_DONE, event -> done.add(event.effect().orElseThrow().name()));

        this.drag(30, 220, 120);

        assertEquals(List.of("ben"), dropped);
        assertEquals(List.of("MOVE"), done);
        assertEquals(List.of(), this.moves);
        assertEquals(5, this.list.items().size());
    }

    @Test
    void aListWithoutADragTransferDragsNothing() {
        this.list.setDragTransfer(null);
        final List<String> over = new ArrayList<>();
        this.bin.onEvent(InputEvents.DRAG_OVER, event -> over.add("over"));
        this.drag(30, 220, 120);
        assertEquals(List.of(), over);
        assertEquals(Optional.empty(), this.list.dragTransfer());
    }

    @Test
    void aTransferThatIsEmptyStartsNoDrag() {
        this.list.setDragTransfer(rows -> Transfer.empty());
        this.drag(30, 50, 81);
        assertEquals(List.of(), this.moves);
        assertTrue(this.host.root().dragImage().isEmpty());
        assertEquals(DropEffect.MOVE, DropEffect.valueOf("MOVE"));
    }
}
