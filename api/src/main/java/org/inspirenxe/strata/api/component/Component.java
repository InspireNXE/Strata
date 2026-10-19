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

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.component.MeasureEvent;
import org.inspirenxe.strata.api.event.component.PaintEvent;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;

/**
 * An element of a Strata UI tree.
 *
 * <p>Do not implement this interface. Get components from a {@link org.inspirenxe.strata.api.ComponentFactory}, or
 * extend {@link org.inspirenxe.strata.api.extension.CustomComponent} to make a new kind. A class that implements
 * this interface directly has no Strata implementation behind it; adding one to a container throws
 * {@link IllegalArgumentException}.</p>
 *
 * <h2>Layout</h2>
 * <p>Layout runs in two passes before a frame is drawn, and only after something changed. First each component is
 * <em>measured</em>: its parent offers some space and the component reports the size it wants, its
 * {@linkplain #desiredWidth() desired size}. Then each component is <em>arranged</em>: its parent gives it a slot,
 * and the component places itself in the slot by its {@linkplain #margin() margin},
 * {@linkplain #horizontalAlignment() alignment} and size limits. The result is its {@linkplain #bounds() bounds}.
 * A component with {@code STRETCH} alignment fills its slot; with any other alignment it keeps its desired size and
 * may overflow a small slot, in which case a clipping parent cuts it off.</p>
 *
 * <p>Desired sizes exclude the margin. A fixed size set with {@link #setWidth}/{@link #setHeight} replaces the
 * measured size. Every size is clamped to the {@linkplain #minWidth() minimum} and {@linkplain #maxWidth()
 * maximum}.</p>
 *
 * <h2>What only some components have</h2>
 * <p>This interface holds what every component needs. The rest comes from smaller interfaces a component implements
 * when it fits: {@link Focusable} for keyboard focus and tab order, {@link HasCursor} and {@link HasTooltip}, and
 * {@link Container} for children, background and border.</p>
 *
 * <h2>Events</h2>
 * <p>Everything a component does reaches code as an event, registered with
 * {@link #onEvent(EventType, EventHandler)}: input (see {@link org.inspirenxe.strata.api.event.input.InputEvents}), a
 * control's own requests and reports, and drawing and sizing ({@link #PAINT}, {@link #PAINTED},
 * {@link #MEASURED}). For each component an input event reaches, the handlers run in priority order, then the
 * component's own default behavior (a button pressing, a text box inserting a character) unless a handler canceled
 * the event. A handler can read the state components had when the event fired; see {@link ComponentSnapshot}.</p>
 *
 * <h2>Threading</h2>
 * <p>Call components only from the engine's UI thread.</p>
 *
 * @param <T> the type callers see the component as, so setters can return it for chaining
 */
public interface Component<T extends Component<T>> extends EventTarget<T> {

    /**
     * Fired before the component draws itself. Canceling it skips the component's own drawing; its
     * children still draw. See {@link PaintEvent}.
     */
    EventType<PaintEvent.Pre> PAINT = EventType.direct("paint", PaintEvent.Pre.class);

    /**
     * Fired after the component and its children have drawn, so handlers draw on top. See {@link PaintEvent}.
     */
    EventType<PaintEvent> PAINTED = EventType.direct("painted", PaintEvent.class);

    /**
     * Fired after the component measured its content, so a handler can change the size it asks for. See
     * {@link MeasureEvent}.
     */
    EventType<MeasureEvent> MEASURED = EventType.direct("measured", MeasureEvent.class);

    // Tree

    /**
     * Returns the container this component was added to.
     *
     * @return the parent, or {@code null} for a screen's root component or a component not added anywhere
     */
    Container<?> parent();

    /**
     * Returns whether the component is in a tree that a host is showing. Hover, focus and press state only change
     * while attached, and the theme is only applied once attached.
     *
     * @return true while attached to a host
     */
    boolean isAttached();

    // State

    /**
     * Returns whether the component is visible.
     *
     * @return true unless hidden with {@code setVisible(false)}
     */
    boolean isVisible();

    /**
     * Shows or hides the component. A hidden component is not drawn, cannot be hovered or focused, and takes no
     * space in layout. Hiding the focused component, or one of its ancestors, clears focus.
     *
     * @param visible false to hide
     * @return this component
     */
    T setVisible(boolean visible);

    /**
     * Returns whether the component is enabled.
     *
     * @return true unless disabled with {@code setEnabled(false)}
     */
    boolean isEnabled();

    /**
     * Enables or disables the component. A disabled component is still drawn (built-in controls draw a disabled
     * look) and still takes space, but it receives no pointer or keyboard input and cannot be focused. Disabling
     * the focused component, or one of its ancestors, clears focus.
     *
     * @param enabled false to disable
     * @return this component
     */
    T setEnabled(boolean enabled);

    // Layout

    /**
     * Returns the margin.
     *
     * @return the space kept clear around the component; {@link Insets#NONE} by default
     */
    Insets margin();

    /**
     * Sets the space kept clear around the component, outside its bounds. Parents add the margin to the
     * component's desired size when they make room for it.
     *
     * @param margin the margin
     * @return this component
     */
    T setMargin(Insets margin);

    /**
     * Sets the same margin on all four sides.
     *
     * @param all the margin on every side
     * @return this component
     */
    default T setMargin(final float all) {
        return this.setMargin(Insets.of(all));
    }

    /**
     * Sets one margin for the left and right sides and another for the top and bottom.
     *
     * @param horizontal the margin on the left and right
     * @param vertical the margin on the top and bottom
     * @return this component
     */
    default T setMargin(final float horizontal, final float vertical) {
        return this.setMargin(Insets.of(horizontal, vertical));
    }

    /**
     * Sets the margin on each side.
     *
     * @param left the margin on the left
     * @param top the margin on the top
     * @param right the margin on the right
     * @param bottom the margin on the bottom
     * @return this component
     */
    default T setMargin(final float left, final float top, final float right, final float bottom) {
        return this.setMargin(new Insets(left, top, right, bottom));
    }

    /**
     * Returns the minimum width.
     *
     * @return the minimum width; 0 by default
     */
    float minWidth();

    /**
     * Returns the maximum width.
     *
     * @return the maximum width; {@link Float#POSITIVE_INFINITY} by default
     */
    float maxWidth();

    /**
     * Returns the minimum height.
     *
     * @return the minimum height; 0 by default
     */
    float minHeight();

    /**
     * Returns the maximum height.
     *
     * @return the maximum height; {@link Float#POSITIVE_INFINITY} by default
     */
    float maxHeight();

    /**
     * Sets the smallest size the component may take. Applies to measured sizes, fixed sizes and stretching alike.
     *
     * @param minWidth the minimum width
     * @param minHeight the minimum height
     * @return this component
     */
    T setMinSize(float minWidth, float minHeight);

    /**
     * Sets the largest size the component may take. Applies to measured sizes, fixed sizes and stretching alike.
     * The maximum also limits the space offered to the component when it is measured, so text wraps within it.
     *
     * @param maxWidth the maximum width; {@link Float#POSITIVE_INFINITY} for no limit
     * @param maxHeight the maximum height; {@link Float#POSITIVE_INFINITY} for no limit
     * @return this component
     */
    T setMaxSize(float maxWidth, float maxHeight);

    /**
     * Fixes the width, replacing the measured width and overriding {@code STRETCH}. The minimum and maximum still
     * apply.
     *
     * @param width the width, or {@link Float#NaN} to go back to the measured width
     * @return this component
     */
    T setWidth(float width);

    /**
     * Fixes the height, replacing the measured height and overriding {@code STRETCH}. The minimum and maximum still
     * apply.
     *
     * @param height the height, or {@link Float#NaN} to go back to the measured height
     * @return this component
     */
    T setHeight(float height);

    /**
     * Fixes both the width and the height; see {@link #setWidth} and {@link #setHeight}.
     *
     * @param width the width, or {@link Float#NaN} for the measured width
     * @param height the height, or {@link Float#NaN} for the measured height
     * @return this component
     */
    default T setSize(final float width, final float height) {
        this.setWidth(width);
        return this.setHeight(height);
    }

    /**
     * Returns how the component places itself horizontally in its slot.
     *
     * @return the horizontal alignment; {@link HorizontalAlignment#LEFT} by default
     */
    HorizontalAlignment horizontalAlignment();

    /**
     * Returns how the component places itself vertically in its slot.
     *
     * @return the vertical alignment; {@link VerticalAlignment#TOP} by default
     */
    VerticalAlignment verticalAlignment();

    /**
     * Sets how the component places itself in the slot its parent gives it.
     *
     * @param horizontal the horizontal alignment
     * @param vertical the vertical alignment
     * @return this component
     */
    T setAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical);

    /**
     * Sets both alignments from an anchor, for example {@link Anchor#CENTER} or {@link Anchor#FILL}.
     *
     * @param anchor the anchor
     * @return this component
     */
    default T setAnchor(final Anchor anchor) {
        return this.setAlignment(anchor.horizontal(), anchor.vertical());
    }

    /**
     * Returns where the component sits, in the host's coordinates, as of the last layout. Before the first layout
     * this is {@link Rect#ZERO}. A hidden component has an empty rectangle.
     *
     * @return the bounds
     */
    Rect bounds();

    /**
     * Returns the width the component asked for in the last layout, margin excluded.
     *
     * @return the desired width
     */
    float desiredWidth();

    /**
     * Returns the height the component asked for in the last layout, margin excluded.
     *
     * @return the desired height
     */
    float desiredHeight();

    // Events
}
