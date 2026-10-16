package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

import java.util.Objects;

/**
 * A container that lays its children out one after another, in a column ({@link Orientation#VERTICAL}, the
 * default) or a row ({@link Orientation#HORIZONTAL}), with a fixed gap between them.
 *
 * <p>Along the orientation, each child gets exactly the space it asks for. Across it, each child gets the full
 * content size and aligns itself in it: in a column, a child with {@link HorizontalAlignment#STRETCH} spans the
 * width and one with {@link HorizontalAlignment#CENTER} is centered. Hidden children take no space and no gap.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#stackPanel()}. Do not implement this
 * interface.</p>
 */
public interface StackPanel extends Container<StackPanel> {

    /**
     * Returns the direction children are laid out in.
     *
     * @return the orientation; {@link Orientation#VERTICAL} by default
     */
    Orientation orientation();

    /**
     * Sets the direction children are laid out in.
     *
     * @param orientation the orientation
     * @return this panel
     * @throws NullPointerException if {@code orientation} is null
     */
    StackPanel setOrientation(Orientation orientation);

    /**
     * Returns the gap between visible children.
     *
     * @return the spacing in UI units; 0 by default
     */
    float spacing();

    /**
     * Sets the gap between visible children. The gap is added between children only, not before the first or
     * after the last.
     *
     * @param spacing the spacing in UI units
     * @return this panel
     */
    StackPanel setSpacing(float spacing);

    /**
     * A stack panel's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}. It says
     * nothing about the children.
     *
     * @param orientation the {@linkplain #orientation() orientation}; never null
     * @param spacing the {@linkplain #spacing() gap} between children
     */
    record State(Orientation orientation, float spacing) {

        /**
         * Checks the orientation.
         *
         * @throws NullPointerException if {@code orientation} is null
         */
        public State {
            Objects.requireNonNull(orientation, "orientation");
        }
    }
}
