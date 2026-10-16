package org.inspirenxe.strata.api.event;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * A drag that started on the target. All positions are in root coordinates.
 */
public interface DragEvent extends PointerEvent {

    MouseButton button();

    /**
     * The click count of the press that started the drag, so a double-click drag can select by word.
     */
    int clicks();

    float startX();

    float startY();

    /**
     * Movement since the previous drag event.
     */
    float deltaX();

    float deltaY();
}
