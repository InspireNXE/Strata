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
package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStylePatch;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;

import java.util.List;
import java.util.Optional;

/**
 * A component with children. The container decides where its children go (see the layouts in
 * {@link org.inspirenxe.strata.api.layout}).
 *
 * <h2>Two orders</h2>
 * <p>The children have a layout order, the order of {@link #children()}, which layout, focus and Tab follow, and
 * which {@link #insert} and {@link #move} change. They also have a drawing order, {@link #paintOrder()}: by
 * {@linkplain Component#zIndex() z index}, lowest first, and among children with the same z index in the order they
 * were added, {@linkplain #raise raised} or {@linkplain #lower lowered}. Later children in the drawing order appear
 * on top, and the pointer hits them first. While children are only added at the end and no z index is set, the
 * drawing order is the layout order, so the child added last is on top.</p>
 *
 * <pre>{@code
 * // Bring the window the user pressed to the front without moving it in the layout
 * panel.raise(window);
 * // Keep a toolbar over every window, whatever is raised
 * toolbar.setZIndex(1);
 * }</pre>
 *
 * <p>Do not implement this interface. Get containers from a {@link org.inspirenxe.strata.api.ComponentFactory}, or
 * extend {@link org.inspirenxe.strata.api.extension.CustomContainer} to write a new layout.</p>
 *
 * @param <T> the type callers see the container as, so setters can return it for chaining
 */
public interface Container<T extends Container<T>> extends Component<T>, HasCursor<T>, HasTooltip<T> {

    /**
     * Returns the children in layout order, the order layout, focus and Tab follow. The list is a live, unmodifiable
     * view: it reflects later changes, and throws {@link UnsupportedOperationException} if changed directly. For the
     * order children are drawn in, see {@link #paintOrder()}.
     *
     * @return the children
     */
    List<Component<?>> children();

    /**
     * Adds a child at the end of the list. It is drawn over the existing children with the same z index. If this
     * container is attached, the child is attached too and gets the host's theme.
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
     * Adds a child at a place in the layout order. A child inserted anywhere is drawn over the existing children
     * with the same z index, as one added at the end is. If this container is attached, the child is attached too
     * and gets the host's theme.
     *
     * @param index where the child goes in {@link #children()}: 0 for the first, {@code children().size()} for the
     *     last
     * @param child the component to add
     * @return this container
     * @throws IndexOutOfBoundsException if the index is negative or greater than the number of children
     * @throws IllegalArgumentException as for {@link #add(Component)}, including when the child is already a child
     *                                  of this container
     * @throws org.inspirenxe.strata.api.theme.ThemeException as for {@link #add(Component)}
     */
    T insert(int index, Component<?> child);

    /**
     * Moves a child to another place in the layout order. The child stays attached: it keeps focus, hover and press
     * state, and fires no events of leaving or joining the tree. Layout runs again and Tab follows the new order.
     * Where the child is drawn among its siblings does not change.
     *
     * @param child the child to move
     * @param index the place it ends up at in {@link #children()}, from 0 to {@code children().size() - 1}
     * @return this container
     * @throws IllegalArgumentException if the component is not a child of this container
     * @throws IndexOutOfBoundsException if the index is negative or not less than the number of children
     */
    T move(Component<?> child, int index);

    /**
     * Returns where a child is in the layout order.
     *
     * @param child the component to look for
     * @return its index in {@link #children()}, or -1 when it is not a child of this container
     */
    int indexOf(Component<?> child);

    /**
     * Draws a child over its siblings with the same z index, and lets the pointer hit it first among them. Siblings
     * with a higher z index stay over it. The layout order does not change, and nothing is laid out again.
     *
     * @param child the child to raise
     * @return this container
     * @throws IllegalArgumentException if the component is not a child of this container
     */
    T raise(Component<?> child);

    /**
     * Draws a child under its siblings with the same z index, so the pointer hits it last among them. Siblings with
     * a lower z index stay under it. The layout order does not change, and nothing is laid out again.
     *
     * @param child the child to lower
     * @return this container
     * @throws IllegalArgumentException if the component is not a child of this container
     */
    T lower(Component<?> child);

    /**
     * Returns the children in the order they are drawn, bottom first. The pointer hits them from the end of the
     * list. The list is a copy that later changes do not reach.
     *
     * @return the children, bottom first
     */
    List<Component<?>> paintOrder();

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
     * @return the space between the container's edges and its children; {@link Insets#NONE} by default, and the
     *     theme's for a window
     */
    Insets padding();

    /**
     * Sets the space between the container's edges and its children. Padding is inside the bounds, so the
     * container's background covers it. Padding set here stays through theme changes.
     *
     * @param padding the padding; {@link #clearPadding()} goes back to the default
     * @return this container
     */
    T setPadding(Insets padding);

    /**
     * Returns the patch this container applies to the text drawn inside it.
     *
     * @return the patch, or empty for none, the default
     */
    Optional<TextStylePatch> textStyle();

    /**
     * Sets a patch applied over the text styles of every control inside this container, its children and theirs, so
     * a panel can make the text inside it gray without restyling each control:
     * {@code panel.setTextStyle(TextStylePatch.of().color(0xFF808080))}. Each control's theme style comes first,
     * then the patches of its containers, outermost first, so an inner container wins where both set a property. A
     * style set in code on the control, such as with {@code Label.setStyle}, wins over them all. The patches apply to
     * the text style keys the theme defines; a control reading a key the theme lacks keeps its own fallback. They do
     * not reach popups and other overlays, which are not inside the container, nor text the container draws itself.
     * Strata works the styles out when the theme is applied or the tree changes, never while painting.
     *
     * @param patch the patch, or {@code null} for none
     * @return this container
     */
    T setTextStyle(TextStylePatch patch);
    /**
     * Goes back to the container's default padding: none, or the theme's for a container that reads one, such as a
     * window. The default then follows theme changes again.
     *
     * @return this container
     */
    T clearPadding();

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
     * Sets one padding for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the padding on the left and right
     * @param vertical the padding on the top and bottom
     * @return this container
     */
    default T setPadding(final float horizontal, final float vertical) {
        return this.setPadding(Insets.of(horizontal, vertical));
    }

    /**
     * Sets the padding on each side.
     *
     * @param left the padding on the left
     * @param top the padding on the top
     * @param right the padding on the right
     * @param bottom the padding on the bottom
     * @return this container
     */
    default T setPadding(final float left, final float top, final float right, final float bottom) {
        return this.setPadding(new Insets(left, top, right, bottom));
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
