package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.layout.Orientation;

import java.util.function.Consumer;

/**
 * Two components side by side, or one above the other, with a divider between them that the user drags to share
 * the space.
 *
 * <p>A {@link Orientation#HORIZONTAL} pane puts the first component on the left and the second on the right, with a
 * vertical divider; a {@link Orientation#VERTICAL} one puts the first on top. The divider is {@value #DIVIDER_SIZE}
 * units thick and shows a resize cursor. Its position is the fraction of the space, after the divider, that the
 * first component gets. Dragging moves it, kept so neither side is smaller than its {@linkplain #setMinSizes minimum}
 * when there is room for both. Each side is placed in its part by its own alignment, so give it
 * {@link org.inspirenxe.strata.api.layout.Anchor#FILL} to fill the part.</p>
 *
 * <p>The desired size is the two components' desired sizes and the divider along the split, and the larger of the
 * two across it. A missing side counts as empty.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_DIVIDER} (paint, optional): the divider. Falls back to {@value Separator#STYLE_LINE}.</li>
 *     <li>{@value #STYLE_DIVIDER_HOVERED} (paint, optional): the divider under the pointer or being dragged. Falls
 *     back to {@value #STYLE_DIVIDER}.</li>
 * </ul>
 *
 * <pre>{@code
 * SplitPane split = factory.splitPane(Orientation.HORIZONTAL)
 *         .setFirst(categories).setSecond(offers).setPosition(0.3f);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#splitPane(Orientation)}. Do not implement this
 * interface.</p>
 */
public interface SplitPane extends Component<SplitPane> {
    /**
     * Style key for the divider (paint, optional).
     */
    String STYLE_DIVIDER = "split.divider";
    /**
     * Style key for the divider under the pointer or being dragged (paint, optional).
     */
    String STYLE_DIVIDER_HOVERED = "split.divider.hovered";
    /**
     * How thick the divider is, in UI units.
     */
    float DIVIDER_SIZE = 4f;
    /**
     * The smallest each side may be dragged to by default, in UI units.
     */
    float DEFAULT_MIN_SIZE = 20f;

    /**
     * Returns the direction of the split.
     *
     * @return {@link Orientation#HORIZONTAL} for side by side, {@link Orientation#VERTICAL} for one above the other
     */
    Orientation orientation();

    /**
     * Returns the left or top component.
     *
     * @return the first component, or null if none was set
     */
    Component<?> first();

    /**
     * Replaces the left or top component. The old one is removed from the tree.
     *
     * @param first the new component, or null for none; it must not have a parent
     * @return this pane
     * @throws IllegalArgumentException if {@code first} already has a parent
     */
    SplitPane setFirst(Component<?> first);

    /**
     * Returns the right or bottom component.
     *
     * @return the second component, or null if none was set
     */
    Component<?> second();

    /**
     * Replaces the right or bottom component. The old one is removed from the tree.
     *
     * @param second the new component, or null for none; it must not have a parent
     * @return this pane
     * @throws IllegalArgumentException if {@code second} already has a parent
     */
    SplitPane setSecond(Component<?> second);

    /**
     * Returns the divider's position.
     *
     * @return the fraction of the space the first component gets, from 0 to 1; 0.5 by default
     */
    float position();

    /**
     * Moves the divider. Listeners run if the position changed. Unlike a drag, it is not limited by the minimum
     * sizes.
     *
     * @param position the fraction of the space the first component gets
     * @return this pane
     * @throws IllegalArgumentException if {@code position} is outside 0 to 1, or NaN
     */
    SplitPane setPosition(float position);

    /**
     * Sets the smallest each side may be dragged to.
     *
     * @param first the first component's minimum along the split, in UI units
     * @param second the second component's minimum, in UI units
     * @return this pane
     * @throws IllegalArgumentException if either is negative or infinite
     */
    SplitPane setMinSizes(float first, float second);

    /**
     * Adds a listener told the new position after every change, by dragging or by code.
     *
     * @param listener receives the new position
     * @return this pane
     */
    SplitPane onPositionChanged(Consumer<Float> listener);
}
