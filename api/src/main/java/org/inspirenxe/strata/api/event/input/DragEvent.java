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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.transfer.DropEffect;
import org.inspirenxe.strata.api.transfer.Transfer;

import java.util.Set;
import java.util.function.Function;

/**
 * A drag: the pointer moving while a button that went down on the target is held. Used by
 * {@link InputEvents#DRAG_START}, {@link InputEvents#DRAG} and {@link InputEvents#DRAG_END}.
 *
 * <p>A press only becomes a drag once the pointer moves more than 4 units from where the button went down. From
 * then on the target receives the drag events even while the pointer is outside it, and the release that ends the
 * drag does not also produce a {@link InputEvents#CLICK}. Each button drags independently.</p>
 *
 * <p>A {@link InputEvents#DRAG_START} handler can make the drag carry a payload with {@link #startTransfer}. The
 * components under the pointer then hear the drag as {@link TransferEvent}s instead, and the source hears no more
 * {@link InputEvents#DRAG}; it still hears the release and {@link InputEvents#DRAG_END}.</p>
 *
 * <p>Only Strata creates events. Do not implement this interface.</p>
 */
public interface DragEvent extends PointerEvent {

    /**
     * Returns the button being held.
     *
     * @return the button
     */
    MouseButton button();

    /**
     * Returns the click count of the press that started the drag, so a drag after a double click can select by
     * word. See {@link PointerButtonEvent#clicks()}.
     *
     * @return 1, 2 or 3
     */
    int clicks();

    /**
     * Returns the x where the button went down, in the host's coordinates.
     *
     * @return the start x
     */
    float startX();

    /**
     * Returns the y where the button went down, in the host's coordinates.
     *
     * @return the start y
     */
    float startY();

    /**
     * Returns how far the pointer moved horizontally since the previous drag event. Zero for
     * {@link InputEvents#DRAG_START}.
     *
     * @return the horizontal movement
     */
    float deltaX();

    /**
     * Returns how far the pointer moved vertically since the previous drag event. Zero for
     * {@link InputEvents#DRAG_START}.
     *
     * @return the vertical movement
     */
    float deltaY();

    /**
     * Makes this drag carry a payload, with no drag image. See {@link #startTransfer(Transfer, Set, Function)}.
     *
     * @param transfer what the drag carries
     * @param allowed the effects a target may take
     * @throws IllegalArgumentException if the transfer or the set of effects is empty
     * @throws IllegalStateException outside a {@link InputEvents#DRAG_START} event, after a transfer was started
     *     for this drag, or while another drag of the host carries a transfer
     */
    void startTransfer(Transfer transfer, Set<DropEffect> allowed);

    /**
     * Makes this drag carry a payload. Call it from a {@link InputEvents#DRAG_START} handler or a component's
     * {@code onDragStart} hook. The {@linkplain #current() current} component becomes the drag's source, and the
     * event is {@linkplain #consume() consumed}, so an ancestor does not start a transfer of its own.
     *
     * <p>From then on each move goes to the components under the pointer as {@link InputEvents#DRAG_OVER}, and the
     * release drops the transfer on the one that accepted last, as {@link TransferEvent} describes. Escape, or a
     * release where nothing accepted, ends the drag with no effect. The source then hears
     * {@link InputEvents#TRANSFER_DONE}.</p>
     *
     * @param transfer what the drag carries
     * @param allowed the effects a target may take
     * @param image builds the component drawn at the pointer while the drag lasts, once, with the host's factory;
     *     it must return a new component
     * @throws IllegalArgumentException if the transfer or the set of effects is empty
     * @throws IllegalStateException outside a {@link InputEvents#DRAG_START} event, after a transfer was started
     *     for this drag, or while another drag of the host carries a transfer
     */
    void startTransfer(Transfer transfer, Set<DropEffect> allowed,
                       Function<ComponentFactory, ? extends Component<?>> image);
}
