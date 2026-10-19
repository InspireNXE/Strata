package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;

import java.util.List;

/**
 * A component with children. The container decides where its children go (see the layouts in
 * {@link org.inspirenxe.strata.api.layout}); children are drawn in list order, so later ones appear on top, and
 * the pointer hits later ones first.
 *
 * <p>Do not implement this interface. Get containers from a {@link org.inspirenxe.strata.api.ComponentFactory}, or
 * extend {@link org.inspirenxe.strata.api.extension.CustomContainer} to write a new layout.</p>
 *
 * @param <T> the type callers see the container as, so setters can return it for chaining
 */
public interface Container<T extends Container<T>> extends Component<T>, HasCursor<T>, HasTooltip<T> {

    /**
     * Returns the children in drawing order. The list is a live, unmodifiable view: it reflects later adds and
     * removes, and throws {@link UnsupportedOperationException} if changed directly.
     *
     * @return the children
     */
    List<Component<?>> children();

    /**
     * Adds a child at the end of the list, on top of the existing children. If this container is attached, the
     * child is attached too and gets the host's theme.
     *
     * @param child the component to add
     * @return this container
     * @throws IllegalArgumentException if the child already has a parent, is a screen's root component, is this
     *                                  container or one of its ancestors, has no Strata implementation behind it, or
     *                                  was made by a different factory
     * @throws org.inspirenxe.strata.api.theme.ThemeException if this container is attached and the host's theme
     *                                                         lacks style keys the child needs
     */
    T add(Component<?> child);

    /**
     * Removes a child. A removed child loses focus, hover and press state and is detached from the host. Does
     * nothing if the component is not a child of this container.
     *
     * @param child the component to remove
     * @return this container
     */
    T remove(Component<?> child);

    /**
     * Removes every child, as if each were passed to {@link #remove}.
     *
     * @return this container
     */
    T clear();

    /**
     * Returns the padding.
     *
     * @return the space between the container's edges and its children; {@link Insets#NONE} by default
     */
    Insets padding();

    /**
     * Sets the space between the container's edges and its children. Padding is inside the bounds, so the
     * container's background covers it.
     *
     * @param padding the padding
     * @return this container
     */
    T setPadding(Insets padding);

    /**
     * Sets the same padding on all four sides.
     *
     * @param all the padding on every side
     * @return this container
     */
    default T setPadding(final float all) {
        return this.setPadding(Insets.of(all));
    }

    /**
     * Returns whether children are clipped to the container's bounds.
     *
     * @return true by default
     */
    boolean clipsChildren();

    /**
     * Sets whether children are clipped to the container's bounds. While clipping, any part of a child outside
     * the bounds is neither drawn nor hit by the pointer. Turn it off to let children draw outside, for example a
     * focus glow.
     *
     * @param clipChildren false to let children draw outside the bounds
     * @return this container
     */
    T setClipChildren(boolean clipChildren);

    /**
     * Returns the background paint.
     *
     * @return the background; {@link Paint#NONE} by default
     */
    Paint background();

    /**
     * Sets the paint drawn over the container's bounds before anything else of it. Changing the background never
     * changes layout.
     *
     * @param background the background, or {@link Paint#NONE} for none
     * @return this container
     */
    T setBackground(Paint background);

    /**
     * Returns the border.
     *
     * @return the border; {@link Border#NONE} by default
     */
    Border border();

    /**
     * Sets the lines drawn along the inside of the container's edges, over its background and under its children.
     * A border takes no space in layout, so give the container padding at least as wide as its border to keep
     * children off it.
     *
     * @param border the border, or {@link Border#NONE} for none
     * @return this container
     */
    T setBorder(Border border);
}
