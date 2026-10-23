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
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.TextInputKind;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Set;

/**
 * The calls a peer makes into its component's protected hooks. Each
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} hands a private instance to its peer; each method
 * forwards to the hook of the same name, so the peer can reach hooks that are {@code protected} on the component.
 * Not for use outside Strata. Do not implement this interface.
 */
public interface ComponentHooks {

    /**
     * Forwards to {@code CustomComponent.measure}.
     *
     * @param context the measure context
     * @param availableWidth the width offered, margin removed
     * @param availableHeight the height offered, margin removed
     * @return the natural size of the content
     */
    Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Forwards to {@code CustomComponent.measureMinimum}.
     *
     * @param context the measure context
     * @return the smallest size of the content
     */
    Size measureMinimum(MeasureContext context);

    /**
     * Forwards to {@code CustomComponent.baseline}.
     *
     * @return the baseline from the top of the bounds, or NaN for none
     */
    float baseline();

    /**
     * Forwards to {@code CustomComposite.arrange}; does nothing for a leaf component.
     *
     * @param context the arrange context
     * @param bounds the component's final bounds
     */
    void arrange(ArrangeContext context, Rect bounds);

    /**
     * Forwards to {@code CustomComponent.paint}.
     *
     * @param painter the painter
     * @param bounds the component's bounds
     */
    void paint(Painter painter, Rect bounds);

    /**
     * Forwards to {@code CustomComponent.requiredStyleKeys}.
     *
     * @return the required style keys
     */
    Set<String> requiredStyleKeys();

    /**
     * Forwards to {@code CustomComponent.onApplyTheme}.
     *
     * @param theme the host's theme
     */
    void applyTheme(Theme theme);

    /**
     * Forwards to {@code CustomComponent.acceptsTextInput}.
     *
     * @return true if the component takes typed text
     */
    boolean acceptsTextInput();

    /**
     * Forwards to {@code CustomComponent.textInputKind}.
     *
     * @return the kind of text the component takes
     */
    TextInputKind textInputKind();

    /**
     * Runs the component's default behavior for an event, by calling the matching {@code on...} hook.
     *
     * @param type the event type
     * @param event the event
     */
    void handle(EventType<?> type, Event event);

    /**
     * Forwards to {@code CustomComponent.snapshotState}.
     *
     * @return the component's own state for a snapshot, or null for none
     */
    Object snapshotState();

    /**
     * Forwards to {@code CustomComposite.onChildRemoved}; does nothing for a leaf component.
     *
     * @param child the removed child
     */
    void childRemoved(Component<?> child);

    /**
     * Forwards to {@code CustomComposite.childClip}; returns the bounds for a leaf component.
     *
     * @return the rectangle children are clipped and hit-tested against
     */
    Rect childClip();

    /**
     * Forwards to {@code CustomComposite.paintOverChildren}; does nothing for a leaf component.
     *
     * @param painter the painter
     * @param bounds the component's bounds
     */
    void paintOverChildren(Painter painter, Rect bounds);

    /**
     * Forwards to {@code CustomComposite.onDescendantFocused}; does nothing for a leaf component.
     *
     * @param descendant the component inside this one that gained focus
     */
    void descendantFocused(Component<?> descendant);

    /**
     * Forwards to {@code CustomComponent.onAttached}.
     */
    void attached();

    /**
     * Forwards to {@code CustomComponent.onDetached}.
     */
    void detached();
}
