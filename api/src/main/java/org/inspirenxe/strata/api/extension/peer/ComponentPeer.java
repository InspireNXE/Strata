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
package org.inspirenxe.strata.api.extension.peer;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventHandler;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.extension.Environment;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Border;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Optional;
import java.util.function.Consumer;

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
     * @return the parent, or null
     */
    Component<?> parent();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#parent(Class)}.
     *
     * @param type the type the parent must have
     * @param <P> the type
     * @return the parent as that type, or empty
     */
    <P> Optional<P> parent(Class<P> type);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#ancestor(Class)}.
     *
     * @param type the type the ancestor must have
     * @param <A> the type
     * @return the nearest ancestor of that type, or empty
     */
    <A> Optional<A> ancestor(Class<A> type);

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
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#theme()}.
     *
     * @return the theme, or null before the first attach
     */
    Theme theme();

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
     * See {@link org.inspirenxe.strata.api.component.Focusable#isFocusable()}.
     *
     * @return true if focusable
     */
    boolean isFocusable();

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#setFocusable(boolean)}.
     *
     * @param focusable true to allow focus
     */
    void setFocusable(boolean focusable);

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#isTabbable()}.
     *
     * @return true if tabbable
     */
    boolean isTabbable();

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#setTabbable(boolean)}.
     *
     * @param tabbable true to include in tab order
     */
    void setTabbable(boolean tabbable);

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#tabIndex()}.
     *
     * @return the tab index
     */
    int tabIndex();

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#setTabIndex(int)}.
     *
     * @param tabIndex the tab index
     */
    void setTabIndex(int tabIndex);

    /**
     * See {@link org.inspirenxe.strata.api.component.HasCursor#cursor()}.
     *
     * @return the cursor, or null
     */
    CursorKind cursor();

    /**
     * See {@link org.inspirenxe.strata.api.component.HasCursor#setCursor(CursorKind)}.
     *
     * @param cursor the cursor, or null
     */
    void setCursor(CursorKind cursor);

    /**
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#isHovered()}.
     *
     * @return true while hovered
     */
    boolean isHovered();

    /**
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#isHoverWithin()}.
     *
     * @return true while this component or a descendant is hovered
     */
    boolean isHoverWithin();

    /**
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#isPressed()}.
     *
     * @return true while pressed
     */
    boolean isPressed();

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#isFocused()}.
     *
     * @return true while focused
     */
    boolean isFocused();

    /**
     * See {@link org.inspirenxe.strata.api.component.Focusable#requestFocus()}.
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
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#offsetX()}.
     *
     * @return the horizontal offset
     */
    float offsetX();

    /**
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#offsetY()}.
     *
     * @return the vertical offset
     */
    float offsetY();

    /**
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#setOffset(float, float)}.
     *
     * @param offsetX the horizontal offset
     * @param offsetY the vertical offset
     */
    void setOffset(float offsetX, float offsetY);

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#zIndex()}.
     *
     * @return the z index
     */
    int zIndex();

    /**
     * See {@link org.inspirenxe.strata.api.component.Component#setZIndex(int)}.
     *
     * @param zIndex the z index
     */
    void setZIndex(int zIndex);

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#border()}.
     *
     * @return the border
     */
    Border border();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#setBorder(Border)}.
     *
     * @param border the border
     */
    void setBorder(Border border);

    /**
     * See {@link org.inspirenxe.strata.api.component.HasTooltip#tooltip()}.
     *
     * @return the tooltip, or null
     */
    Tooltip tooltip();

    /**
     * See {@link org.inspirenxe.strata.api.component.HasTooltip#setTooltip(Tooltip)}.
     *
     * @param tooltip the tooltip, or null
     */
    void setTooltip(Tooltip tooltip);

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#background()}.
     *
     * @return the background
     */
    Paint background();

    /**
     * See {@link org.inspirenxe.strata.api.component.Container#setBackground(Paint)}.
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
     * See {@link org.inspirenxe.strata.api.extension.CustomComponent#invalidateLayout()}.
     */
    void invalidateLayout();

    /**
     * See {@code CustomComposite.invalidateArrangement()}.
     */
    void invalidateArrangement();

    /**
     * See {@link org.inspirenxe.strata.api.event.EventTarget#subscribe(EventType, int, EventHandler)}.
     *
     * @param <E> the event interface
     * @param type the event type
     * @param priority where the handler runs; the highest runs last
     * @param handler the handler
     * @return a subscription that removes the handler
     */
    <E extends Event> Subscription subscribe(EventType<E> type, int priority, EventHandler<? super E> handler);

    /**
     * Fires a request without a value. See {@code CustomComponent.request(EventType)}.
     *
     * @param type the request type
     * @return true unless a handler canceled the request
     */
    boolean request(EventType<?> type);

    /**
     * Fires a request with a value and applies the final value unless canceled. See
     * {@code CustomComponent.request(EventType, Object, Consumer)}.
     *
     * @param <V> the value type
     * @param type the request type
     * @param value the value the user's action produced
     * @param apply makes the change with the final value
     */
    <V> void request(EventType<?> type, V value, Consumer<? super V> apply);

    /**
     * Fires a report without a value. See {@code CustomComponent.report(EventType)}.
     *
     * @param type the report type
     */
    void report(EventType<?> type);

    /**
     * Fires a report with a value. See {@code CustomComponent.report(EventType, Object, Object)}.
     *
     * @param <V> the value type
     * @param type the report type
     * @param value the value after the change
     * @param previous the value before the change
     */
    <V> void report(EventType<?> type, V value, V previous);

    /**
     * Returns whether a handler for a type is on the component's path. See {@code CustomComponent.isHandled}.
     *
     * @param type the event type
     * @return true if firing the type would reach a handler
     */
    boolean isHandled(EventType<?> type);
}
