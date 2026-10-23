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
package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.DragEvent;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * The core {@link DragEvent}. The host sends one when a drag starts, one for each pointer move during it, and one
 * when the button that started it is released. All of them go to the component the button was pressed on.
 */
public final class DragEventImpl extends PointerEventImpl implements DragEvent {
    private final MouseButton button;
    private final int clicks;
    private final float startX;
    private final float startY;
    private final float deltaX;
    private final float deltaY;
    // Whether startTransfer may be called, and what it asked for
    private boolean transferOpen;
    private TransferRequest request;

    /**
     * Creates the event.
     *
     * @param target the component the button was pressed on
     * @param x the pointer's x, in the host's coordinates; the press position for the start event
     * @param y the pointer's y, in the host's coordinates; the press position for the start event
     * @param modifiers the modifiers held, or {@link Modifiers#NONE} when the engine does not report them
     * @param button the button that started the drag
     * @param clicks the click count of the press that started the drag
     * @param startX the x of that press, in the host's coordinates
     * @param startY the y of that press, in the host's coordinates
     * @param deltaX the horizontal movement since the previous event of this drag, 0 for the start event
     * @param deltaY the vertical movement since the previous event of this drag, 0 for the start event
     */
    public DragEventImpl(final Component<?> target, final float x, final float y, final Modifiers modifiers,
                         final MouseButton button, final int clicks, final float startX, final float startY,
                         final float deltaX, final float deltaY) {
        super(target, x, y, modifiers);
        this.button = button;
        this.clicks = clicks;
        this.startX = startX;
        this.startY = startY;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    @Override
    public MouseButton button() {
        return this.button;
    }

    @Override
    public int clicks() {
        return this.clicks;
    }

    @Override
    public float startX() {
        return this.startX;
    }

    @Override
    public float startY() {
        return this.startY;
    }

    @Override
    public float deltaX() {
        return this.deltaX;
    }

    @Override
    public float deltaY() {
        return this.deltaY;
    }

    /**
     * Lets handlers of this event start a transfer. The root calls it on a drag start event while no other drag of
     * its host carries a transfer.
     *
     * @return this event
     */
    public DragEventImpl allowTransfer() {
        this.transferOpen = true;
        return this;
    }

    @Override
    public void startTransfer(final Transfer transfer, final Set<DropEffect> allowed) {
        this.start(transfer, allowed, null);
    }

    @Override
    public void startTransfer(final Transfer transfer, final Set<DropEffect> allowed,
                              final Function<ComponentFactory, ? extends Component<?>> image) {
        this.start(transfer, allowed, Objects.requireNonNull(image, "image"));
    }

    private void start(final Transfer transfer, final Set<DropEffect> allowed,
                       final Function<ComponentFactory, ? extends Component<?>> image) {
        Objects.requireNonNull(transfer, "transfer");
        Objects.requireNonNull(allowed, "allowed");
        if (!this.transferOpen) {
            throw new IllegalStateException(this.request != null ? "This drag already carries a transfer"
                    : "A transfer starts only from a drag start event, while no other drag carries one");
        }
        if (transfer.isEmpty()) {
            throw new IllegalArgumentException("A drag cannot carry an empty transfer");
        }
        if (allowed.isEmpty()) {
            throw new IllegalArgumentException("A drag must allow at least one drop effect");
        }
        this.transferOpen = false;
        this.request = new TransferRequest(this.current(), transfer, Collections.unmodifiableSet(EnumSet.copyOf(allowed)),
                image);
        this.consume();
    }

    /**
     * Returns the transfer a handler started.
     *
     * @return the request, or {@code null} if no handler started one
     */
    public TransferRequest transferRequest() {
        return this.request;
    }

    /**
     * A transfer a drag start handler asked for.
     *
     * @param source the component that was current when the handler asked
     * @param transfer what the drag carries
     * @param allowed the effects a target may take, never empty
     * @param image builds the drag image, or {@code null} for none
     */
    public record TransferRequest(Component<?> source, Transfer transfer, Set<DropEffect> allowed,
                                  Function<ComponentFactory, ? extends Component<?>> image) {
    }
}
