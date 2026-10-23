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
package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.state.PropertyKey;

import java.util.Optional;

/**
 * Two components side by side, or one above the other, with a divider between them that the user drags to share
 * the space.
 *
 * <p>A {@link Orientation#HORIZONTAL} pane puts the first component on the left and the second on the right, with a
 * vertical divider; a {@link Orientation#VERTICAL} one puts the first on top. The divider is {@value #DIVIDER_SIZE}
 * units thick and shows a resize cursor. Its position is the fraction of the space left beside the divider that the
 * first component gets. Dragging moves it. Each move by a drag fires {@link #MOVE}, and every change fires
 * {@link #MOVED}. Each side is placed in its part by its own alignment, so give it
 * {@link org.inspirenxe.strata.api.layout.Anchor#FILL} to fill the part.</p>
 *
 * <p>Neither side is ever laid out smaller than its {@linkplain #setMinSizes minimum} when the pane has room for
 * both, whatever the position, so shrinking the pane does not push a sidebar below its minimum. When there is not
 * room for both, the side the {@linkplain #setResizePolicy resize policy} keeps gets its minimum, or all the space if
 * that is less, and the other side gives way: the second under {@link ResizePolicy#PROPORTIONAL}. The position is
 * not changed by this; it comes back into force once the pane is large enough.</p>
 *
 * <p>When the pane's size changes, {@link ResizePolicy#PROPORTIONAL} keeps the position, so both sides grow and
 * shrink together, while {@link ResizePolicy#KEEP_FIRST} keeps the first side's length in units and
 * {@link ResizePolicy#KEEP_SECOND} the second's, so a sidebar keeps its width as a window grows. Under those two the
 * position follows the kept length, and changes without {@link #MOVED} as the pane is resized, since the divider
 * stays where it was.</p>
 *
 * <p>A side made {@linkplain #setCollapsible collapsible} collapses with a double click on the divider, or Enter
 * while the divider has focus, through {@link #COLLAPSE}, and the same again restores it to where it was. A
 * collapsed side is taken out of the tree until it is restored, and the other side gets all the space beside the
 * divider. Dragging the divider of a collapsed pane restores it first, through {@link #COLLAPSE} with null, and then
 * moves it. The arrows, Home and End do nothing while a side is collapsed.</p>
 *
 * <p>The divider is a tab stop between the two sides, and a press on it focuses it: {@link #requestFocus()} focuses
 * it, and {@link #isFocused()} is true while it has focus. While focused, the arrow keys along the split, Left and
 * Right for a pane side by side and Up and Down for one above the other, move it by {@value #KEY_STEP} units, or by 1
 * with Shift, and Home and End move it until the first or the second side is at its minimum, each through
 * {@link #MOVE}. It draws a focus ring while focused.</p>
 *
 * <p>The desired size is the two components' desired sizes and the divider along the split, and the larger of the
 * two across it. A missing side counts as empty.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_DIVIDER} (paint, optional): the divider. Falls back to {@value Separator#STYLE_LINE}.</li>
 *     <li>{@value #STYLE_DIVIDER_HOVERED} (paint, optional): the divider under the pointer or being dragged. Falls
 *     back to {@value #STYLE_DIVIDER}.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): a one-unit ring drawn inside the divider while it has focus. Falls
 *     back to {@value org.inspirenxe.strata.api.control.button.Button#STYLE_FOCUS}; no ring if both are missing.</li>
 * </ul>
 *
 * <pre>{@code
 * SplitPane split = factory.splitPane(Orientation.HORIZONTAL)
 *         .setFirst(categories).setSecond(offers).setPosition(0.3f);
 * }</pre>
 *
 * <p>It fires no events of its own beyond {@link #MOVE} and {@link #MOVED}; the events of the two sides are content
 * and pass the pane on to its ancestors.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#splitPane()}, which is side by side, or
 * {@link org.inspirenxe.strata.api.ComponentFactory#splitPane(Orientation)}. Do not implement this interface.</p>
 */
public interface SplitPane extends Component<SplitPane>, Focusable<SplitPane>, HasCursor<SplitPane>,
        HasTooltip<SplitPane>, HasContextMenu<SplitPane> {
    /**
     * Style key for the divider (paint, optional).
     */
    String STYLE_DIVIDER = "split.divider";
    /**
     * Style key for the divider under the pointer or being dragged (paint, optional).
     */
    String STYLE_DIVIDER_HOVERED = "split.divider.hovered";
    /**
     * Style key for the ring drawn inside the divider while it has focus (paint, optional).
     */
    String STYLE_FOCUS = "splitpane.focus";
    /**
     * How far an arrow key moves the divider, in UI units; with Shift held it moves 1.
     */
    float KEY_STEP = 10f;
    /**
     * How thick the divider is, in UI units.
     */
    float DIVIDER_SIZE = 4f;
    /**
     * The smallest each side may be dragged to by default, in UI units.
     */
    float DEFAULT_MIN_SIZE = 20f;

    /**
     * Fired each time a drag with the left button or a key is about to move the divider to a new position, before
     * it moves.
     * The value is the fraction of the space the first side is about to get, already kept within the minimum sizes.
     * Not fired while the divider would stay where it is. Canceling it keeps the divider in place. A handler may set
     * another value, which the pane clamps to 0 to 1 and keeps within the minimum sizes when both fit; NaN and null
     * are ignored. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<SplitPane, Float>> MOVE = EventType.valueRequest("split_pane.move");
    /**
     * Fired after the divider moved. The value is the new position and the previous value the old one, each the
     * fraction of the space the first side gets. Fires for changes made from code too.
     */
    EventType<ValueEvent<SplitPane, Float>> MOVED = EventType.valueReport("split_pane.moved");
    /**
     * Where the divider is, from 0 to 1, as {@link #position()} and {@link #setPosition} say. Binds two ways through
     * {@link #MOVED}. A value outside 0 to 1 throws as {@code setPosition} does.
     */
    PropertyKey<SplitPane, Float> POSITION = PropertyKey.of("split_pane.position", SplitPane::position,
            SplitPane::setPosition, MOVED);

    /**
     * Asked before the user collapses a side or restores it, by a double click on the divider, Enter while the
     * divider has focus, or a drag on the divider of a collapsed pane. The value is the side about to collapse, or
     * null to restore the collapsed one. Canceling it keeps the pane as it is; a handler may set another side, or
     * null, which the pane then takes. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<SplitPane, Side>> COLLAPSE = EventType.valueRequest("split_pane.collapse");
    /**
     * Told after a side collapsed or was restored. The value is the collapsed side, or null when none is collapsed
     * now, and the previous value the side collapsed before, or null. Fires for changes made from code too.
     */
    EventType<ValueEvent<SplitPane, Side>> COLLAPSED = EventType.valueReport("split_pane.collapsed");

    /**
     * How a pane shares a change of its size between its sides.
     */
    enum ResizePolicy {
        /**
         * The position is kept, so both sides grow and shrink in proportion. A new pane's policy.
         */
        PROPORTIONAL,
        /**
         * The first side keeps its length in units and the second takes the change.
         */
        KEEP_FIRST,
        /**
         * The second side keeps its length in units and the first takes the change.
         */
        KEEP_SECOND
    }

    /**
     * One of a pane's two sides.
     */
    enum Side {
        /**
         * The left or top side.
         */
        FIRST,
        /**
         * The right or bottom side.
         */
        SECOND
    }

    /**
     * Returns the direction of the split.
     *
     * @return {@link Orientation#HORIZONTAL} for side by side, {@link Orientation#VERTICAL} for one above the other
     */
    Orientation orientation();

    /**
     * Sets the direction of the split. The position is kept. A new layout runs.
     *
     * @param orientation {@link Orientation#HORIZONTAL} for side by side, {@link Orientation#VERTICAL} for one
     *                    above the other
     * @return this pane
     */
    SplitPane setOrientation(Orientation orientation);

    /**
     * Returns the left or top component.
     *
     * @return the first component, or empty if none was set
     */
    Optional<Component<?>> first();

    /**
     * Replaces the left or top component. The old one is removed from the tree.
     *
     * @param first the new component, or null for none; it must not have a parent
     * @return this pane
     * @throws IllegalArgumentException if {@code first} already has a parent, is a host's content, or is this pane
     *     or one of its ancestors; the pane is left as it was
     */
    SplitPane setFirst(Component<?> first);

    /**
     * Returns the right or bottom component.
     *
     * @return the second component, or empty if none was set
     */
    Optional<Component<?>> second();

    /**
     * Replaces the right or bottom component. The old one is removed from the tree.
     *
     * @param second the new component, or null for none; it must not have a parent
     * @return this pane
     * @throws IllegalArgumentException if {@code second} already has a parent, is a host's content, or is this pane
     *     or one of its ancestors; the pane is left as it was
     */
    SplitPane setSecond(Component<?> second);

    /**
     * Returns the divider's position.
     *
     * @return the fraction of the space the first component gets, from 0 to 1; 0.5 by default
     */
    float position();

    /**
     * Moves the divider. Fires {@link #MOVED} if the position changed. Unlike a drag, it is not limited by the
     * minimum sizes.
     *
     * @param position the fraction of the space the first component gets
     * @return this pane
     * @throws IllegalArgumentException if {@code position} is outside 0 to 1, or NaN
     */
    SplitPane setPosition(float position);

    /**
     * Sets the smallest each side may be, whether dragged or laid out. A new layout runs.
     *
     * @param first the first component's minimum along the split, in UI units
     * @param second the second component's minimum, in UI units
     * @return this pane
     * @throws IllegalArgumentException if either is negative or infinite
     */
    SplitPane setMinSizes(float first, float second);

    /**
     * Returns how the pane shares a change of its size between its sides.
     *
     * @return the policy; {@link ResizePolicy#PROPORTIONAL} for a new pane
     */
    ResizePolicy resizePolicy();

    /**
     * Sets how the pane shares a change of its size between its sides. The sides keep their lengths as of now, and
     * the new policy applies from the next change of size. A new layout runs.
     *
     * @param policy the policy
     * @return this pane
     */
    SplitPane setResizePolicy(ResizePolicy policy);

    /**
     * Returns whether the user can collapse a side.
     *
     * @param side the side
     * @return true after {@code setCollapsible} turned it on; false for a new pane
     */
    boolean isCollapsible(Side side);

    /**
     * Sets which sides the user can collapse with a double click on the divider or Enter. When both can, the first
     * collapses. Code can collapse either side with {@link #setCollapsed} whatever this says.
     *
     * @param first whether the user can collapse the first side
     * @param second whether the user can collapse the second side
     * @return this pane
     */
    SplitPane setCollapsible(boolean first, boolean second);

    /**
     * Returns the collapsed side.
     *
     * @return the side, or empty while both show
     */
    Optional<Side> collapsed();

    /**
     * Collapses a side, or restores the collapsed one for null, without asking {@link #COLLAPSE}. Collapsing one
     * side while the other is collapsed restores that one first. Fires {@link #COLLAPSED} if anything changed. The
     * position is kept, so restoring puts the divider back where it was. A new layout runs.
     *
     * @param side the side to collapse, or null to show both
     * @return this pane
     */
    SplitPane setCollapsed(Side side);
}
