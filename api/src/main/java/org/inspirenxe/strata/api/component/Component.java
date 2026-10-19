package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.MeasureEvent;
import org.inspirenxe.strata.api.event.PaintEvent;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

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
 * <h2>Events</h2>
 * <p>Everything a component does reaches code as an event, registered with
 * {@link #onEvent(EventType, EventHandler)}: input (see {@link org.inspirenxe.strata.api.event.EventTypes}), a
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
     * Fired before the component draws its background. Canceling it skips the component's own drawing; its
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
     * while attached, and {@link #theme()} is only set once attached.
     *
     * @return true while attached to a host
     */
    boolean isAttached();

    /**
     * Returns the theme applied to this component.
     *
     * @return the host's theme, or {@code null} before the component is first attached
     */
    Theme theme();

    /**
     * Returns the identifier set with {@link #setId}.
     *
     * @return the identifier, or {@code null} if none was set
     */
    String id();

    /**
     * Sets a free-form identifier, used only for debugging and in {@code toString}. Strata does not require ids to
     * be unique.
     *
     * @param id the identifier, or {@code null} to clear it
     * @return this component
     */
    T setId(String id);

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

    /**
     * Returns whether the component can take keyboard focus.
     *
     * @return true if focusable
     */
    boolean isFocusable();

    /**
     * Sets whether the component can take keyboard focus, by a click or by {@link #requestFocus()}. Built-in
     * buttons and text boxes are focusable; labels and containers are not.
     *
     * @param focusable true to allow focus
     * @return this component
     */
    T setFocusable(boolean focusable);

    /**
     * Returns whether Tab and Shift+Tab stop at this component.
     *
     * @return true if tabbable
     */
    boolean isTabbable();

    /**
     * Sets whether Tab and Shift+Tab stop at this component. Only matters for focusable components.
     *
     * @param tabbable true to include the component in tab order
     * @return this component
     */
    T setTabbable(boolean tabbable);

    /**
     * Returns the tab index.
     *
     * @return the tab index; 0 by default
     */
    int tabIndex();

    /**
     * Sets the tab index. Tab visits components in ascending tab index, and components with equal indexes in tree
     * order (depth first, children in the order they were added). Negative values are allowed.
     *
     * @param tabIndex the tab index
     * @return this component
     */
    T setTabIndex(int tabIndex);

    /**
     * Returns the cursor shown while the pointer is over this component.
     *
     * @return the cursor, or {@code null} to use the nearest ancestor's
     */
    CursorKind cursor();

    /**
     * Sets the cursor shown while the pointer is over this component. With no cursor set on the component or any
     * ancestor, the host shows {@link CursorKind#ARROW}. Engines without a matching cursor show the closest one
     * they have.
     *
     * @param cursor the cursor, or {@code null} to use the nearest ancestor's
     * @return this component
     */
    T setCursor(CursorKind cursor);

    /**
     * Returns whether the pointer is over this component. Only the deepest component under the pointer counts as
     * hovered; its ancestors do not.
     *
     * @return true while hovered
     */
    boolean isHovered();

    /**
     * Returns whether the pointer is over this component or any of its descendants, so a list can light up the row
     * whose label is hovered.
     *
     * @return true while this component or a descendant is hovered
     */
    boolean isHoverWithin();

    /**
     * Returns whether a mouse button went down on this component and has not been released yet. Stays true while
     * the pointer is dragged away.
     *
     * @return true while pressed
     */
    boolean isPressed();

    /**
     * Returns whether this component has keyboard focus. At most one component per host has focus.
     *
     * @return true while focused
     */
    boolean isFocused();

    /**
     * Asks the host to give this component keyboard focus. Fails, changing nothing, unless the component is
     * attached, focusable, visible and enabled, with every ancestor visible and enabled.
     *
     * @return true if the component now has focus
     */
    boolean requestFocus();

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
     * Returns the horizontal offset.
     *
     * @return how far the component is moved right of where layout puts it; 0 by default
     */
    float offsetX();

    /**
     * Returns the vertical offset.
     *
     * @return how far the component is moved down from where layout puts it; 0 by default
     */
    float offsetY();

    /**
     * Moves the component from where its parent's layout puts it, along with everything inside it. Siblings and the
     * parent's size do not change, so this suits nudges and dragging, such as moving a window by its title bar.
     *
     * @param offsetX the distance to move right, in UI units; negative moves left
     * @param offsetY the distance to move down, in UI units; negative moves up
     * @return this component
     */
    T setOffset(float offsetX, float offsetY);

    /**
     * Returns the background paint.
     *
     * @return the background; {@link Paint#NONE} by default
     */
    Paint background();

    /**
     * Sets the paint drawn over the component's bounds before anything else of the component. Changing the
     * background never changes layout.
     *
     * @param background the background, or {@link Paint#NONE} for none
     * @return this component
     */
    T setBackground(Paint background);

    /**
     * Returns the border.
     *
     * @return the border; {@link Border#NONE} by default
     */
    Border border();

    /**
     * Sets the lines drawn along the inside of the component's edges, over its background and under its content.
     * A border takes no space in layout, so give a container padding at least as wide as its border to keep
     * children off it.
     *
     * @param border the border, or {@link Border#NONE} for none
     * @return this component
     */
    T setBorder(Border border);

    /**
     * Returns the tooltip.
     *
     * @return the tooltip, or {@code null} for none
     */
    Tooltip tooltip();

    /**
     * Sets what the host shows near the pointer while it rests on this component. A descendant without a tooltip
     * shows its nearest ancestor's.
     *
     * @param tooltip the tooltip, or {@code null} for none
     * @return this component
     */
    T setTooltip(Tooltip tooltip);

    /**
     * Sets a plain-text tooltip. Line breaks ({@code \n}) start new lines.
     *
     * @param text the text, or {@code null} for no tooltip
     * @return this component
     */
    default T setTooltip(final String text) {
        return this.setTooltip(text == null ? null : Tooltip.text(text));
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

    /**
     * Schedules a new layout before the next frame. Setters that change size already do this; call it from a
     * custom component when its own content changes size.
     */
    void invalidateLayout();

    // Events
}
