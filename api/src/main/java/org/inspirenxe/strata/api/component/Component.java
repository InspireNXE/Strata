package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

/**
 * An element of a Strata tree.
 *
 * <p><b>Not for implementation.</b> Components come from the {@link org.inspirenxe.strata.api.Ui} factory, or
 * from subclassing {@link org.inspirenxe.strata.api.extension.CustomComponent}. A class that implements this
 * interface directly has no Strata implementation behind it and is rejected when added to a tree.</p>
 *
 * <p><b>Layout</b> runs in two passes. A component is first measured for the size it wants within the space
 * offered, then given a slot, inside which it places itself by its alignment, margin and size limits. Desired
 * sizes exclude the margin. Layout only runs after something changed.</p>
 *
 * <p><b>Events</b>: handlers registered with {@link #on} run before ({@link Phase#BEFORE}) or after
 * ({@link Phase#AFTER}) the component's default behavior. Canceling an event in a {@code BEFORE} handler skips the
 * default behavior.</p>
 *
 * @param <T> the component's own type, so setters chain
 */
public interface Component<T extends Component<T>> {

    // Tree

    /**
     * The parent container, or {@code null} for a root or a component not yet added.
     */
    Container<?> parent();

    /**
     * Whether the component is part of a tree that a host is showing.
     */
    boolean isAttached();

    /**
     * The theme last applied, or {@code null} before the component joins a tree.
     */
    Theme theme();

    String id();

    T setId(String id);

    // State

    boolean isVisible();

    T setVisible(boolean visible);

    boolean isEnabled();

    T setEnabled(boolean enabled);

    boolean isFocusable();

    T setFocusable(boolean focusable);

    boolean isTabbable();

    T setTabbable(boolean tabbable);

    int tabIndex();

    /**
     * Lower indexes come first in tab order; equal indexes keep tree order.
     */
    T setTabIndex(int tabIndex);

    /**
     * The cursor to show while the pointer is over this component, or {@code null} to use the parent's.
     */
    CursorKind cursor();

    T setCursor(CursorKind cursor);

    boolean isHovered();

    boolean isPressed();

    boolean isFocused();

    /**
     * Asks the host to focus this component.
     *
     * @return true if it now has focus
     */
    boolean requestFocus();

    // Layout

    Insets margin();

    T setMargin(Insets margin);

    default T setMargin(final float all) {
        return this.setMargin(Insets.of(all));
    }

    float minWidth();

    float maxWidth();

    float minHeight();

    float maxHeight();

    T setMinSize(float minWidth, float minHeight);

    T setMaxSize(float maxWidth, float maxHeight);

    /**
     * Fixes the width; {@link Float#NaN} returns to the measured width.
     */
    T setWidth(float width);

    /**
     * Fixes the height; {@link Float#NaN} returns to the measured height.
     */
    T setHeight(float height);

    default T setSize(final float width, final float height) {
        this.setWidth(width);
        return this.setHeight(height);
    }

    HorizontalAlignment horizontalAlignment();

    VerticalAlignment verticalAlignment();

    T setAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical);

    default T setAnchor(final Anchor anchor) {
        return this.setAlignment(anchor.horizontal(), anchor.vertical());
    }

    Paint background();

    T setBackground(Paint background);

    /**
     * Final bounds in root coordinates, valid after layout.
     */
    Rect bounds();

    float desiredWidth();

    float desiredHeight();

    /**
     * Asks for layout to run again before the next frame.
     */
    void invalidateLayout();

    // Events

    /**
     * Registers a handler that runs before this component's default behavior, so it can cancel it.
     */
    default <E extends Event> Subscription on(final EventType<E> type, final EventHandler<? super E> handler) {
        return this.on(type, Phase.BEFORE, handler, null);
    }

    default <E extends Event> Subscription on(final EventType<E> type, final Phase phase, final EventHandler<? super E> handler) {
        return this.on(type, phase, handler, null);
    }

    /**
     * @param owner a token that {@link #removeHandlers(Object)} can later use to remove every handler it registered
     */
    <E extends Event> Subscription on(EventType<E> type, Phase phase, EventHandler<? super E> handler, Object owner);

    void removeHandlers(Object owner);
}
