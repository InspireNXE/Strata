package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
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
 * <p>Strata routes input to components as events (see {@link org.inspirenxe.strata.api.event.EventTypes}). For each
 * component an event reaches, {@link Phase#BEFORE} handlers run first, then the component's own default behavior
 * (a button pressing, a text box inserting a character), then {@link Phase#AFTER} handlers. A {@code BEFORE}
 * handler that cancels the event skips the default behavior. A handler registered with
 * {@link #onWithSnapshot(EventType, Phase, EventHandler, Object)} can also read the state components had when the
 * event fired; see {@link ComponentSnapshot}.</p>
 *
 * <h2>Threading</h2>
 * <p>Call components only from the engine's UI thread.</p>
 *
 * @param <T> the type callers see the component as, so setters can return it for chaining
 */
public interface Component<T extends Component<T>> {

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
     * Enables or disables the component. A disabled component is still drawn (built-in widgets draw a disabled
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
     * @throws NullPointerException if {@code margin} is null
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
     * @throws NullPointerException if either alignment is null
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
     * @throws NullPointerException if {@code background} is null
     */
    T setBackground(Paint background);

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

    /**
     * Registers a handler that runs in {@link Phase#BEFORE}, so it can cancel the component's default behavior.
     *
     * @param <E> the event interface
     * @param type the event type, usually a constant from {@link org.inspirenxe.strata.api.event.EventTypes}
     * @param handler the handler
     * @return a subscription that removes the handler when closed
     */
    default <E extends Event> Subscription on(final EventType<E> type, final EventHandler<? super E> handler) {
        return this.on(type, Phase.BEFORE, handler, null);
    }

    /**
     * Registers a handler for one phase.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase when the handler runs relative to the default behavior
     * @param handler the handler
     * @return a subscription that removes the handler when closed
     */
    default <E extends Event> Subscription on(final EventType<E> type, final Phase phase, final EventHandler<? super E> handler) {
        return this.on(type, phase, handler, null);
    }

    /**
     * Registers a handler for one phase, tagged with an owner. Handlers for the same type and phase run in the
     * order they were registered. A handler may register or remove handlers while it runs; the change applies from
     * the next event.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase when the handler runs relative to the default behavior
     * @param handler the handler
     * @param owner a token for {@link #removeHandlers(Object)}, or {@code null}
     * @return a subscription that removes the handler when closed; closing it twice does nothing
     * @throws NullPointerException if {@code type}, {@code phase} or {@code handler} is null
     */
    <E extends Event> Subscription on(EventType<E> type, Phase phase, EventHandler<? super E> handler, Object owner);

    /**
     * Registers a {@link Phase#BEFORE} handler that needs {@linkplain Event#snapshotOf(Component) snapshots}; see
     * {@link #onWithSnapshot(EventType, Phase, EventHandler, Object)}.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param handler the handler
     * @return a subscription that removes the handler when closed
     */
    default <E extends Event> Subscription onWithSnapshot(final EventType<E> type, final EventHandler<? super E> handler) {
        return this.onWithSnapshot(type, Phase.BEFORE, handler, null);
    }

    /**
     * Registers a handler for one phase that needs {@linkplain Event#snapshotOf(Component) snapshots}; see
     * {@link #onWithSnapshot(EventType, Phase, EventHandler, Object)}.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase when the handler runs relative to the default behavior
     * @param handler the handler
     * @return a subscription that removes the handler when closed
     */
    default <E extends Event> Subscription onWithSnapshot(final EventType<E> type, final Phase phase,
                                                          final EventHandler<? super E> handler) {
        return this.onWithSnapshot(type, phase, handler, null);
    }

    /**
     * Registers a handler like {@link #on(EventType, Phase, EventHandler, Object)}, and asks Strata to take
     * snapshots for every event of this type whose path includes this component. Before the event reaches any
     * handler, Strata records the state of the event's target and of this component, so the handler can compare
     * {@link Event#snapshot()} or {@link Event#currentSnapshot()} with the live components.
     *
     * <p>Snapshots cost an allocation per captured component, so register this way only the handlers that read
     * them. Events whose path has no such handler take no snapshots. Handlers registered with {@code on} on the same
     * component run as before and may read the snapshots too while they exist.</p>
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase when the handler runs relative to the default behavior
     * @param handler the handler
     * @param owner a token for {@link #removeHandlers(Object)}, or {@code null}
     * @return a subscription that removes the handler when closed; closing it twice does nothing
     * @throws NullPointerException if {@code type}, {@code phase} or {@code handler} is null
     */
    <E extends Event> Subscription onWithSnapshot(EventType<E> type, Phase phase, EventHandler<? super E> handler,
                                                  Object owner);

    /**
     * Removes every handler on this component registered with the given owner. Compared by identity.
     *
     * @param owner the token passed to {@link #on(EventType, Phase, EventHandler, Object)}
     * @throws NullPointerException if {@code owner} is null
     */
    void removeHandlers(Object owner);
}
