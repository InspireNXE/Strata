package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;

/**
 * A container that places each child at a fixed position, measured from the top-left corner of its content area
 * (its bounds minus padding). Each child gets a slot of exactly its desired size plus margin at that position, so
 * alignment has no effect here. Children may overlap; later ones are drawn on top.
 *
 * <p>Its desired size is just large enough to cover every child. Use it for free-form placement, such as a canvas
 * of draggable items; prefer the other layouts for forms and menus, which adapt to text and screen size.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#absolutePanel()}. Do not implement this
 * interface.</p>
 */
public interface AbsolutePanel extends Container<AbsolutePanel> {

    /**
     * Adds a child at a position. {@link Container#add(Component)} adds at (0, 0).
     *
     * @param child the component to add
     * @param x the left edge of the child's slot, relative to the content area
     * @param y the top edge of the child's slot, relative to the content area
     * @return this panel
     * @throws IllegalArgumentException as for {@link Container#add(Component)}
     */
    AbsolutePanel add(Component<?> child, float x, float y);

    /**
     * Moves a child. Does nothing if the component is not a child of this panel.
     *
     * @param child the child to move
     * @param x the new left edge, relative to the content area
     * @param y the new top edge, relative to the content area
     * @return this panel
     */
    AbsolutePanel move(Component<?> child, float x, float y);
}
