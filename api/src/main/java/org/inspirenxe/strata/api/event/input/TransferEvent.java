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
package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Optional;
import java.util.Set;

/**
 * A drag that carries a {@link Transfer}, as a component under the pointer or the drag's source hears it. Used by
 * {@link InputEvents#DRAG_ENTER}, {@link InputEvents#DRAG_OVER}, {@link InputEvents#DRAG_EXIT},
 * {@link InputEvents#DROP} and {@link InputEvents#TRANSFER_DONE}.
 *
 * <p>A drag carries a transfer once a {@link InputEvents#DRAG_START} handler calls {@link DragEvent#startTransfer},
 * or when the adapter reports a drag from another application. On each move of the pointer the components under it
 * hear {@code DRAG_OVER}, from the deepest up, until one calls {@link #accept}: that component would take the drop,
 * with that effect. The release fires {@code DROP} at the component that accepted last, and the source then hears
 * {@code TRANSFER_DONE} with the effect the target took, so it can remove what it gave away after a
 * {@link DropEffect#MOVE}.</p>
 *
 * <p>Every one of these events runs in the input frame of the pointer event that carries it, with the user as the
 * origin, and with the source pushed on the cause, so a target can tell a reorder inside its own list from a drag out
 * of another component.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface TransferEvent extends PointerEvent {

    /**
     * Returns what is being dragged. During a drag from another application a format's value may read as empty
     * until the drop, as {@link Transfer} describes, while {@link Transfer#has} already answers.
     *
     * @return the transfer
     */
    Transfer transfer();

    /**
     * Returns the effects the source allows. {@link #accept(DropEffect)} takes one of them.
     *
     * @return an unmodifiable set, never empty
     */
    Set<DropEffect> allowed();

    /**
     * Returns the component the drag started on: the one whose {@code DRAG_START} handler started the transfer.
     *
     * @return the source, or empty for a drag from another application
     */
    Optional<Component<?>> source();

    /**
     * Returns the effect the user asks for with the modifier keys, among the allowed ones: with Control and Shift
     * {@link DropEffect#LINK}, with Control {@link DropEffect#COPY}, with Shift or no modifier {@link DropEffect#MOVE},
     * each when allowed, and otherwise the first allowed of move, copy and link.
     *
     * @return the preferred effect
     */
    DropEffect preferred();

    /**
     * Returns the effect so far. In {@code DRAG_OVER} it is empty until a component accepts. In {@code DROP} it
     * starts as the effect the target accepted on the last move, and is what the target takes unless a handler
     * changes it. In {@code TRANSFER_DONE} it is the effect the target took. In {@code DRAG_ENTER} and
     * {@code DRAG_EXIT} it is empty.
     *
     * @return the effect, or empty
     */
    Optional<DropEffect> effect();

    /**
     * Takes the drag here with an effect. In {@code DRAG_OVER} the {@linkplain #current() current} component
     * becomes the one that would take the drop, and the event is {@linkplain #consume() consumed}, so it stops
     * climbing and no default behavior accepts it again. In {@code DROP} the effect becomes the one taken.
     *
     * @param effect the effect, one of {@link #allowed()}
     * @throws IllegalArgumentException if the source does not allow the effect
     * @throws IllegalStateException in an event other than {@code DRAG_OVER} and {@code DROP}
     */
    void accept(DropEffect effect);

    /**
     * Takes the drag here with the {@linkplain #preferred() preferred} effect, as {@link #accept(DropEffect)} does.
     *
     * @throws IllegalStateException in an event other than {@code DRAG_OVER} and {@code DROP}
     */
    default void accept() {
        this.accept(this.preferred());
    }

    /**
     * Refuses the drag here. In {@code DRAG_OVER} nothing takes the drop on this move, and the event is consumed,
     * so no ancestor takes it either. In {@code DROP} the drop ends with no effect. Canceling a
     * {@code DROP} only skips the default behavior of the components it reaches, as for any input event, so a
     * handler that took the drop itself may consume it.
     *
     * @throws IllegalStateException in an event other than {@code DRAG_OVER} and {@code DROP}
     */
    void reject();
}
