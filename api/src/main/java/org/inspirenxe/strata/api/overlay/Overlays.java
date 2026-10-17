package org.inspirenxe.strata.api.overlay;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.geom.Rect;

import java.util.List;
import java.util.Optional;

/**
 * Shows components above a host's screen: popups such as a drop-down list, and dialogs such as a confirmation
 * box. Each host has one; get it from {@link org.inspirenxe.strata.api.ComponentFactory#overlays()}.
 *
 * <p>Overlays stack in the order they open, each drawn above the screen and the overlays before it, outside every
 * clip. While any overlay is open:</p>
 * <ul>
 *     <li>The pointer reaches the topmost overlay under it, and the screen only where no overlay is.</li>
 *     <li>A press outside a popup closes it and goes no further. A press on a popup below the top one closes the
 *     popups above it.</li>
 *     <li>A dialog is modal: nothing below it gets pointer input, focus, or Tab, and keys go to it when nothing in it
 *     has focus.</li>
 *     <li>An Escape press that nothing consumed closes the topmost overlay.</li>
 * </ul>
 *
 * <p>Opening a dialog moves focus to the first tabbable component in it, and closing it puts focus back where it
 * was if that component can still take it. Closing the screen closes every overlay.</p>
 *
 * <p>Strata provides this object. Do not implement this interface. Call it on the UI thread only.</p>
 */
public interface Overlays {
    /**
     * Style key for the shade drawn over everything below an open dialog (paint, optional). Nothing is drawn if it
     * is missing.
     */
    String STYLE_BACKDROP = "overlay.backdrop";

    /**
     * Opens a popup next to an area, usually the bounds of the component that opened it. The popup takes its
     * desired size. It sits below the area, left edges lined up, or above it when there is more room above. It is
     * moved left as needed to stay inside the host.
     *
     * @param content what to show; it must not be in a tree already
     * @param anchor the area to show it next to, in the host's coordinates
     * @return the open popup
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    Overlay popup(Component<?> content, Rect anchor);

    /**
     * Opens a modal dialog. The content is laid out in the whole host, where its own alignment places it: a
     * centered window stays centered when the host resizes.
     *
     * @param content what to show; it must not be in a tree already
     * @return the open dialog
     * @throws IllegalArgumentException if the content is already in a tree or was made by another factory
     */
    Overlay dialog(Component<?> content);

    /**
     * Finds the overlay a component is shown in.
     *
     * @param component a component, or anything inside an overlay's content
     * @return the overlay whose content is the component or one of its ancestors; empty if the component is not
     *     in an open overlay
     */
    Optional<Overlay> of(Component<?> component);

    /**
     * Returns the open overlays.
     *
     * @return a copy, bottom first
     */
    List<Overlay> open();

    /**
     * Closes every open overlay, top first.
     */
    void closeAll();
}
