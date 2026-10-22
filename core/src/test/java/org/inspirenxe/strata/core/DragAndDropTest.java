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
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.cause.CauseKeys;
import org.inspirenxe.strata.api.event.cause.Device;
import org.inspirenxe.strata.api.event.cause.Origin;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.event.input.TransferEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drags that carry a transfer, inside a host and from another application (design 4.48), written the way a
 * developer would write a tile that can be dragged and a bin that takes it.
 */
class DragAndDropTest {
    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final ScreenHostImpl host = this.test.host();
    private final List<String> log = new ArrayList<>();

    /**
     * A tile that drags its name, as a developer would write it.
     */
    final class Tile extends CustomComponent<Tile> {
        private final String name;
        private Set<DropEffect> allowed = EnumSet.of(DropEffect.MOVE, DropEffect.COPY);
        private boolean image;

        Tile(final String name) {
            super(DragAndDropTest.this.ui);
            this.name = name;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(40, 40);
        }

        @Override
        protected void onDragStart(final DragEvent event) {
            if (this.image) {
                event.startTransfer(Transfer.text(this.name), this.allowed,
                        factory -> new Box(factory, 10, 10));
            } else {
                event.startTransfer(Transfer.text(this.name), this.allowed);
            }
        }

        @Override
        protected void onDrag(final DragEvent event) {
            DragAndDropTest.this.log.add("drag " + this.name);
        }

        @Override
        protected void onDragEnd(final DragEvent event) {
            DragAndDropTest.this.log.add("end " + this.name);
        }

        @Override
        protected void onTransferDone(final TransferEvent event) {
            DragAndDropTest.this.log.add("done " + this.name + " " + event.effect().map(Enum::name).orElse("none"));
        }
    }

    /**
     * A bin that takes text, as a developer would write it.
     */
    final class Bin extends CustomComponent<Bin> {
        private final String name;

        Bin(final String name) {
            super(DragAndDropTest.this.ui);
            this.name = name;
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(60, 60);
        }

        @Override
        protected void onDragEnter(final TransferEvent event) {
            DragAndDropTest.this.log.add("enter " + this.name);
        }

        @Override
        protected void onDragOver(final TransferEvent event) {
            if (event.transfer().has(DataFormat.TEXT)) {
                event.accept();
            }
        }

        @Override
        protected void onDragExit(final TransferEvent event) {
            DragAndDropTest.this.log.add("exit " + this.name);
        }

        @Override
        protected void onDrop(final TransferEvent event) {
            DragAndDropTest.this.log.add("drop " + this.name + " " + event.transfer().get(DataFormat.TEXT).orElse("?")
                    + " " + event.effect().map(Enum::name).orElse("none"));
        }
    }

    private final Tile tile = new Tile("ana");
    private final Bin bin = new Bin("bin");
    private final AbsolutePanel panel = this.ui.absolutePanel().add(this.tile, 10, 10).add(this.bin, 200, 100);

    DragAndDropTest() {
        this.panel.setSize(300, 200);
        this.test.show(this.panel);
    }

    private void press(final float x, final float y) {
        this.press(x, y, Modifiers.NONE);
    }

    private void press(final float x, final float y, final Modifiers modifiers) {
        this.host.pointerMoved(x, y, modifiers);
        this.host.pointerButton(x, y, MouseButton.LEFT, true, modifiers);
    }

    private void move(final float x, final float y) {
        this.host.pointerMoved(x, y, Modifiers.NONE);
    }

    private void release(final float x, final float y) {
        this.host.pointerButton(x, y, MouseButton.LEFT, false, Modifiers.NONE);
    }

    @Test
    void aDragCarriesItsTransferToTheTargetThatAccepts() {
        this.press(20, 20);
        this.move(40, 40);
        this.move(220, 120);
        this.move(225, 125);
        this.release(225, 125);

        assertEquals(List.of("enter bin", "drop bin ana MOVE", "exit bin", "done ana MOVE", "end ana"), this.log);
    }

    @Test
    void aReleaseOverNothingThatAcceptsEndsWithNoEffect() {
        this.press(20, 20);
        this.move(150, 20);
        this.release(150, 20);

        assertEquals(List.of("done ana none", "end ana"), this.log);
    }

    @Test
    void escapeEndsTheTransferAndTheDragGoesOnWithout() {
        this.press(20, 20);
        this.move(220, 120);
        assertTrue(this.host.key(Key.ESCAPE, KeyAction.PRESS, Modifiers.NONE));
        this.move(222, 122);
        this.release(222, 122);

        assertEquals(List.of("enter bin", "exit bin", "done ana none", "drag ana", "end ana"), this.log);
    }

    @Test
    void controlAsksForACopy() {
        final Modifiers control = Modifiers.of(Modifier.CONTROL);
        this.press(20, 20, control);
        this.host.pointerMoved(220, 120, control);
        this.host.pointerButton(220, 120, MouseButton.LEFT, false, control);

        assertTrue(this.log.contains("done ana COPY"), this.log.toString());
    }

    @Test
    void aSourceThatAllowsOnlyCopyIsCopiedWithoutModifiers() {
        this.tile.allowed = EnumSet.of(DropEffect.COPY);
        this.press(20, 20);
        this.move(220, 120);
        this.release(220, 120);

        assertTrue(this.log.contains("done ana COPY"), this.log.toString());
    }

    @Test
    void anAcceptedEffectMustBeAllowed() {
        this.tile.allowed = EnumSet.of(DropEffect.COPY);
        final List<RuntimeException> thrown = new ArrayList<>();
        this.bin.onEvent(InputEvents.DRAG_OVER, event -> {
            try {
                event.accept(DropEffect.LINK);
            } catch (final IllegalArgumentException e) {
                thrown.add(e);
            }
        });
        this.press(20, 20);
        this.move(220, 120);
        assertEquals(1, thrown.size());
    }

    @Test
    void aRejectedDropTakesNothingAndACanceledOneSkipsOnlyTheDefault() {
        final Subscription rejecting = this.bin.subscribe(InputEvents.DROP, 0, TransferEvent::reject);
        this.press(20, 20);
        this.move(220, 120);
        this.release(220, 120);
        // The bin's own hook still runs, and sees no effect left
        assertEquals(List.of("enter bin", "drop bin ana none", "exit bin", "done ana none", "end ana"), this.log);
        rejecting.unsubscribe();

        this.log.clear();
        this.bin.onEvent(InputEvents.DROP, TransferEvent::consume);
        this.press(20, 20);
        this.move(220, 120);
        this.release(220, 120);
        assertEquals(List.of("enter bin", "exit bin", "done ana MOVE", "end ana"), this.log);
    }

    @Test
    void dragOverClimbsUntilAContainerAccepts() {
        final List<Component<?>> dropped = new ArrayList<>();
        final Box inner = new Box(this.ui, 30, 30);
        final AbsolutePanel shelf = this.ui.absolutePanel().add(inner, 0, 0);
        shelf.setSize(50, 50);
        this.panel.add(shelf, 100, 10);
        shelf.onEvent(InputEvents.DRAG_OVER, event -> event.accept(DropEffect.COPY));
        shelf.onEvent(InputEvents.DROP, event -> dropped.add(event.target()));
        this.test.host().layout();

        this.press(20, 20);
        this.move(110, 20);
        this.release(110, 20);

        assertEquals(List.of(shelf), dropped);
        assertTrue(this.log.contains("done ana COPY"), this.log.toString());
    }

    @Test
    void aRejectingChildStopsItsContainerFromTakingTheDrop() {
        final Box inner = new Box(this.ui, 30, 30);
        final AbsolutePanel shelf = this.ui.absolutePanel().add(inner, 0, 0);
        shelf.setSize(50, 50);
        this.panel.add(shelf, 100, 10);
        shelf.onEvent(InputEvents.DRAG_OVER, event -> event.accept(DropEffect.COPY));
        inner.onEvent(InputEvents.DRAG_OVER, TransferEvent::reject);
        this.test.host().layout();

        this.press(20, 20);
        this.move(110, 20);
        this.release(110, 20);

        assertTrue(this.log.contains("done ana none"), this.log.toString());
    }

    @Test
    void transferEventsCarryTheUserAndTheSource() {
        final List<String> seen = new ArrayList<>();
        this.bin.onEvent(InputEvents.DROP, event -> {
            seen.add(event.origin() instanceof Origin.User ? "user" : "code");
            seen.add(String.valueOf(event.cause().first(Tile.class).orElse(null) == this.tile));
            seen.add(String.valueOf(event.source().orElseThrow() == this.tile));
            seen.add(event.cause().context(CauseKeys.DEVICE).orElseThrow().name());
        });
        this.press(20, 20);
        this.move(220, 120);
        this.release(220, 120);

        assertEquals(List.of("user", "true", "true", Device.MOUSE.name()), seen);
    }

    @Test
    void aTransferStartsOnlyFromADragStartAndOnlyOnce() {
        final List<Class<?>> thrown = new ArrayList<>();
        final Box other = new Box(this.ui, 20, 20);
        this.panel.add(other, 100, 150);
        other.onEvent(InputEvents.DRAG, event -> {
            try {
                event.startTransfer(Transfer.text("x"), EnumSet.of(DropEffect.MOVE));
            } catch (final IllegalStateException e) {
                thrown.add(e.getClass());
            }
        });
        other.onEvent(InputEvents.DRAG_START, event -> {
            try {
                event.startTransfer(Transfer.text("x"), EnumSet.noneOf(DropEffect.class));
            } catch (final IllegalArgumentException e) {
                thrown.add(e.getClass());
            }
            try {
                event.startTransfer(Transfer.empty(), EnumSet.of(DropEffect.MOVE));
            } catch (final IllegalArgumentException e) {
                thrown.add(e.getClass());
            }
            event.startTransfer(Transfer.text("x"), EnumSet.of(DropEffect.MOVE));
            try {
                event.startTransfer(Transfer.text("y"), EnumSet.of(DropEffect.MOVE));
            } catch (final IllegalStateException e) {
                thrown.add(e.getClass());
            }
        });
        this.test.host().layout();
        this.press(105, 155);
        this.move(120, 170);

        assertEquals(List.of(IllegalArgumentException.class, IllegalArgumentException.class,
                IllegalStateException.class), thrown);
    }

    @Test
    void aSecondDragCannotStartATransferWhileOneIsGoing() {
        final Tile other = new Tile("ben");
        this.panel.add(other, 100, 150);
        this.test.host().layout();
        final List<Class<?>> thrown = new ArrayList<>();
        other.onEvent(InputEvents.DRAG_START, event -> {
            try {
                event.startTransfer(Transfer.text("x"), EnumSet.of(DropEffect.MOVE));
            } catch (final IllegalStateException e) {
                thrown.add(e.getClass());
            }
            event.consume();
        });
        this.press(20, 20);
        this.move(40, 40);
        this.host.pointerButton(110, 160, MouseButton.RIGHT, true, Modifiers.NONE);
        this.host.pointerMoved(130, 180, Modifiers.NONE);

        assertEquals(List.of(IllegalStateException.class), thrown);
    }

    private CursorKind cursor() {
        final NullCanvas canvas = new NullCanvas().showCursors(EnumSet.allOf(CursorKind.class));
        this.host.render(canvas);
        return canvas.cursor();
    }

    @Test
    void theCursorShowsWhatADropWouldDo() {
        this.press(20, 20);
        this.move(150, 20);
        assertEquals(CursorKind.NO_DROP, this.cursor());
        this.move(220, 120);
        assertEquals(CursorKind.GRABBING, this.cursor());
        this.host.pointerMoved(221, 121, Modifiers.of(Modifier.CONTROL));
        assertEquals(CursorKind.COPY, this.cursor());
        this.release(221, 121);
        assertEquals(CursorKind.ARROW, this.cursor());
    }

    @Test
    void theDragImageIsBuiltOnceFollowsThePointerAndGoesAtTheEnd() {
        this.tile.image = true;
        this.press(20, 20);
        this.move(100, 100);
        final Component<?> image = this.imageNode().orElseThrow();
        this.host.render(new NullCanvas());
        assertEquals(new Rect(108, 108, 10, 10), image.bounds());
        // The image is never hit, so what is under it still takes the drag
        assertSame(this.panel, this.ui.componentAt(110, 110).orElseThrow());

        this.move(295, 195);
        this.host.layout();
        assertSame(image, this.imageNode().orElseThrow());
        assertEquals(new Rect(290, 190, 10, 10), image.bounds());

        this.release(295, 195);
        assertEquals(Optional.empty(), this.imageNode());
    }

    private Optional<Component<?>> imageNode() {
        return this.host.root().dragImage();
    }

    @Test
    void thePressBelongsToTheSourceSoThePressedChildMayLeave() {
        final Box handle = new Box(this.ui, 10, 10);
        final AbsolutePanel card = this.ui.absolutePanel().add(handle, 0, 0);
        card.setSize(30, 30);
        this.panel.add(card, 100, 150);
        card.onEvent(InputEvents.DRAG_START, event -> event.startTransfer(Transfer.text("card"),
                EnumSet.of(DropEffect.MOVE)));
        final List<String> done = new ArrayList<>();
        card.onEvent(InputEvents.TRANSFER_DONE, event -> done.add(event.effect().orElseThrow().name()));
        this.test.host().layout();

        this.press(105, 155);
        this.move(150, 160);
        card.remove(handle);
        this.move(220, 120);
        this.release(220, 120);

        assertEquals(List.of("MOVE"), done);
        assertTrue(this.log.contains("drop bin card MOVE"), this.log.toString());
    }

    @Test
    void aSourceThatLeavesEndsItsDragWithoutEvents() {
        this.press(20, 20);
        this.move(220, 120);
        this.panel.remove(this.tile);
        this.move(222, 122);
        this.release(222, 122);

        assertEquals(List.of("enter bin"), this.log);
        assertEquals(Optional.empty(), this.imageNode());
    }

    @Test
    void aDisabledComponentPassesTheDragToItsEnabledAncestor() {
        final Box inner = new Box(this.ui, 30, 30);
        final AbsolutePanel shelf = this.ui.absolutePanel().add(inner, 0, 0);
        shelf.setSize(50, 50);
        this.panel.add(shelf, 100, 10);
        inner.setEnabled(false);
        final List<Component<?>> entered = new ArrayList<>();
        shelf.onEvent(InputEvents.DRAG_ENTER, event -> entered.add(event.target()));
        inner.onEvent(InputEvents.DRAG_ENTER, event -> entered.add(event.target()));
        this.test.host().layout();

        this.press(20, 20);
        this.move(110, 20);

        assertEquals(List.of(shelf), entered);
    }

    @Test
    void filesFromAnotherApplicationDropOnTheComponentThatAccepts() {
        final List<Object> dropped = new ArrayList<>();
        this.bin.onEvent(InputEvents.DROP, event -> {
            dropped.add(event.source());
            dropped.add(event.transfer().get(DataFormat.FILES).orElseThrow());
        });
        final Transfer later = Transfer.builder().supply(DataFormat.TEXT, () -> null).build();

        assertEquals(Optional.of(DropEffect.MOVE), this.host.dragEntered(220, 120, later,
                EnumSet.of(DropEffect.MOVE, DropEffect.COPY), Modifiers.NONE));
        assertEquals(Optional.empty(), this.host.dragMoved(150, 20, Modifiers.NONE));
        assertEquals(Optional.of(DropEffect.MOVE), this.host.dragMoved(225, 125, Modifiers.NONE));
        final Transfer files = Transfer.builder().add(DataFormat.TEXT, "a.txt")
                .add(DataFormat.FILES, List.of(Path.of("a.txt"))).build();
        assertEquals(Optional.of(DropEffect.MOVE), this.host.dropped(225, 125, files, Modifiers.NONE));

        assertEquals(List.of(Optional.empty(), List.of(Path.of("a.txt"))), dropped);
        assertEquals(List.of("enter bin", "exit bin", "enter bin", "drop bin a.txt MOVE", "exit bin"), this.log);
    }

    @Test
    void aDragFromAnotherApplicationThatLeavesTellsTheComponentUnderIt() {
        this.host.dragEntered(220, 120, Transfer.text("x"), EnumSet.of(DropEffect.COPY), Modifiers.NONE);
        this.host.dragExited();
        assertEquals(List.of("enter bin", "exit bin"), this.log);
        assertEquals(Optional.empty(), this.host.dragMoved(220, 120, Modifiers.NONE));
    }

    @Test
    void aDropWithoutADragBeforeItAllowsEveryEffect() {
        final List<Set<DropEffect>> allowed = new ArrayList<>();
        this.bin.onEvent(InputEvents.DROP, event -> allowed.add(event.allowed()));
        assertEquals(Optional.of(DropEffect.MOVE), this.host.dropped(220, 120, Transfer.text("b"), Modifiers.NONE));
        assertEquals(List.of(EnumSet.allOf(DropEffect.class)), allowed);

        // A handler that accepts consumes the move, so the bin's own hook does not accept again
        this.bin.onEvent(InputEvents.DRAG_OVER, event -> event.accept(DropEffect.LINK));
        assertEquals(Optional.of(DropEffect.LINK), this.host.dropped(220, 120, Transfer.text("b"), Modifiers.NONE));
    }

    @Test
    void aStackSendsDropsToItsTopScreen() {
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        stack.resize(300, 200);
        final List<String> heard = new ArrayList<>();
        stack.push(factory -> {
            final Box target = new Box(factory, 300, 200);
            target.onEvent(InputEvents.DRAG_OVER, event -> event.accept(DropEffect.COPY));
            target.onEvent(InputEvents.DROP, event -> heard.add(event.transfer().get(DataFormat.TEXT).orElseThrow()));
            return target;
        });
        stack.render(new NullCanvas());
        assertEquals(Optional.of(DropEffect.COPY), stack.dragEntered(10, 10, Transfer.text("t"),
                EnumSet.of(DropEffect.COPY), Modifiers.NONE));
        assertEquals(Optional.of(DropEffect.COPY), stack.dropped(10, 10, Transfer.text("t"), Modifiers.NONE));
        assertEquals(List.of("t"), heard);
        assertFalse(heard.isEmpty());
    }
}
