package org.inspirenxe.strata.api.event.input;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * A drag: the pointer moving while a button that went down on the target is held. Used by
 * {@link InputEvents#DRAG_START}, {@link InputEvents#DRAG} and {@link InputEvents#DRAG_END}.
 *
 * <p>A press only becomes a drag once the pointer moves more than 4 units from where the button went down. From
 * then on the target receives the drag events even while the pointer is outside it, and the release that ends the
 * drag does not also produce a {@link InputEvents#CLICK}. Each button drags independently.</p>
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
}
