package org.inspirenxe.strata.api.extension.peer;

import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Phase;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

/**
 * Strata's half of a {@link org.inspirenxe.strata.api.extension.CustomComponent}: the state and bookkeeping every
 * component needs. The component's {@code final} methods delegate here; each method has the meaning of the
 * {@link org.inspirenxe.strata.api.component.Component} method of the same name, with setters returning nothing.
 *
 * <p>Only Strata's implementation provides peers, through
 * {@link org.inspirenxe.strata.api.ComponentFactory#createPeer}. Not for use outside Strata.</p>
 */
public interface ComponentPeer {

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#parent()}.
     *
     * @return the parent container, or null
     */
    Container<?> parent();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isAttached()}.
     *
     * @return true while attached
     */
    boolean isAttached();

    /**
     * Returns the host's environment, available before the component is attached.
     *
     * @return the environment
     */
    Environment environment();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#theme()}.
     *
     * @return the theme, or null before the first attach
     */
    Theme theme();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#id()}.
     *
     * @return the id, or null
     */
    String id();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setId(String)}.
     *
     * @param id the id, or null
     */
    void setId(String id);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isVisible()}.
     *
     * @return true if visible
     */
    boolean isVisible();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setVisible(boolean)}.
     *
     * @param visible false to hide
     */
    void setVisible(boolean visible);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isEnabled()}.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setEnabled(boolean)}.
     *
     * @param enabled false to disable
     */
    void setEnabled(boolean enabled);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isFocusable()}.
     *
     * @return true if focusable
     */
    boolean isFocusable();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setFocusable(boolean)}.
     *
     * @param focusable true to allow focus
     */
    void setFocusable(boolean focusable);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isTabbable()}.
     *
     * @return true if tabbable
     */
    boolean isTabbable();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setTabbable(boolean)}.
     *
     * @param tabbable true to include in tab order
     */
    void setTabbable(boolean tabbable);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#tabIndex()}.
     *
     * @return the tab index
     */
    int tabIndex();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setTabIndex(int)}.
     *
     * @param tabIndex the tab index
     */
    void setTabIndex(int tabIndex);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#cursor()}.
     *
     * @return the cursor, or null
     */
    CursorKind cursor();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setCursor(CursorKind)}.
     *
     * @param cursor the cursor, or null
     */
    void setCursor(CursorKind cursor);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isHovered()}.
     *
     * @return true while hovered
     */
    boolean isHovered();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isPressed()}.
     *
     * @return true while pressed
     */
    boolean isPressed();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#isFocused()}.
     *
     * @return true while focused
     */
    boolean isFocused();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#requestFocus()}.
     *
     * @return true if the component now has focus
     */
    boolean requestFocus();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#margin()}.
     *
     * @return the margin
     */
    Insets margin();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setMargin(Insets)}.
     *
     * @param margin the margin
     */
    void setMargin(Insets margin);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#minWidth()}.
     *
     * @return the minimum width
     */
    float minWidth();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#maxWidth()}.
     *
     * @return the maximum width
     */
    float maxWidth();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#minHeight()}.
     *
     * @return the minimum height
     */
    float minHeight();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#maxHeight()}.
     *
     * @return the maximum height
     */
    float maxHeight();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setMinSize(float, float)}.
     *
     * @param minWidth the minimum width
     * @param minHeight the minimum height
     */
    void setMinSize(float minWidth, float minHeight);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setMaxSize(float, float)}.
     *
     * @param maxWidth the maximum width
     * @param maxHeight the maximum height
     */
    void setMaxSize(float maxWidth, float maxHeight);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setWidth(float)}.
     *
     * @param width the width, or NaN for the measured width
     */
    void setWidth(float width);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setHeight(float)}.
     *
     * @param height the height, or NaN for the measured height
     */
    void setHeight(float height);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#horizontalAlignment()}.
     *
     * @return the horizontal alignment
     */
    HorizontalAlignment horizontalAlignment();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#verticalAlignment()}.
     *
     * @return the vertical alignment
     */
    VerticalAlignment verticalAlignment();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setAlignment}.
     *
     * @param horizontal the horizontal alignment
     * @param vertical the vertical alignment
     */
    void setAlignment(HorizontalAlignment horizontal, VerticalAlignment vertical);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#background()}.
     *
     * @return the background
     */
    Paint background();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setBackground(Paint)}.
     *
     * @param background the background
     */
    void setBackground(Paint background);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#bounds()}.
     *
     * @return the bounds
     */
    Rect bounds();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#desiredWidth()}.
     *
     * @return the desired width
     */
    float desiredWidth();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#desiredHeight()}.
     *
     * @return the desired height
     */
    float desiredHeight();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#invalidateLayout()}.
     */
    void invalidateLayout();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#on(EventType, Phase, EventHandler, Object)}.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase the phase
     * @param handler the handler
     * @param owner the owner token, or null
     * @return the subscription
     */
    <E extends Event> Subscription on(EventType<E> type, Phase phase, EventHandler<? super E> handler, Object owner);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#onWithSnapshot(EventType, Phase, EventHandler, Object)}.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param phase the phase
     * @param handler the handler
     * @param owner the owner token, or null
     * @return the subscription
     */
    <E extends Event> Subscription onWithSnapshot(EventType<E> type, Phase phase, EventHandler<? super E> handler,
                                                  Object owner);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#removeHandlers(Object)}.
     *
     * @param owner the owner token
     */
    void removeHandlers(Object owner);
}
