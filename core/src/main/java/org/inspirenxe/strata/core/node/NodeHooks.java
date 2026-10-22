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
package org.inspirenxe.strata.core.node;

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
 * The calls a {@link Node} makes into its component. A {@link BuiltInComponent} hands the node a private instance
 * that calls its protected hooks. A developer's {@code CustomComponent} hands its peer the public
 * {@link org.inspirenxe.strata.api.extension.peer.ComponentHooks}, which {@link ExtensionHooks} adapts to this
 * interface. Calls that only Strata's own controls answer, {@link #enabledChanged()}, {@link #disabled()},
 * {@link #released()} and {@link #pressedWithin}, do nothing for a developer's component, so they never widen the
 * public extension API.
 */
interface NodeHooks {

    /**
     * Measures the content.
     *
     * @param context the measure context
     * @param availableWidth the width offered, margin removed
     * @param availableHeight the height offered, margin removed
     * @return the natural size of the content
     */
    Size measure(MeasureContext context, float availableWidth, float availableHeight);

    /**
     * Returns the smallest size of the content, for parents that share space.
     *
     * @param context the measure context
     * @return the smallest size of the content
     */
    Size measureMinimum(MeasureContext context);

    /**
     * Returns the distance from the top of the bounds to the first baseline.
     *
     * @return the baseline, or NaN for none
     */
    float baseline();

    /**
     * Gives every child its slot; does nothing for a leaf component.
     *
     * @param context the arrange context
     * @param bounds the component's final bounds
     */
    void arrange(ArrangeContext context, Rect bounds);

    /**
     * Draws the component under its children.
     *
     * @param painter the painter
     * @param bounds the component's bounds
     */
    void paint(Painter painter, Rect bounds);

    /**
     * Returns the style keys the component needs from a theme.
     *
     * @return the required style keys
     */
    Set<String> requiredStyleKeys();

    /**
     * Reads styles from a theme.
     *
     * @param theme the host's theme
     */
    void applyTheme(Theme theme);

    /**
     * Returns whether the component takes typed text.
     *
     * @return true if it does
     */
    boolean acceptsTextInput();

    /**
     * Returns the kind of text the component takes.
     *
     * @return the kind of text
     */
    TextInputKind textInputKind();

    /**
     * Runs the component's default behavior for an event.
     *
     * @param type the event type
     * @param event the event
     */
    void handle(EventType<?> type, Event event);

    /**
     * Returns the component's own state for a snapshot.
     *
     * @return the state, or null for none
     */
    Object snapshotState();

    /**
     * Tells a composite that a child was removed; does nothing for a leaf component.
     *
     * @param child the removed child
     */
    void childRemoved(Component<?> child);

    /**
     * Returns the rectangle children are clipped and hit-tested against; the bounds for a leaf component.
     *
     * @return the child clip
     */
    Rect childClip();

    /**
     * Draws over the children; does nothing for a leaf component.
     *
     * @param painter the painter
     * @param bounds the component's bounds
     */
    void paintOverChildren(Painter painter, Rect bounds);

    /**
     * Tells a composite that a component inside it gained focus; does nothing for a leaf component.
     *
     * @param descendant the component that gained focus
     */
    void descendantFocused(Component<?> descendant);

    /**
     * Tells the component that its own enabled flag changed, either way, not through an ancestor. Runs after the
     * tree took the change and before {@link #disabled()}.
     */
    void enabledChanged();

    /**
     * Tells the component it was disabled itself, not through an ancestor.
     */
    void disabled();

    /**
     * Tells the component that the root let go of the tree it is in and it was not attached again.
     */
    void released();

    /**
     * Tells the component that a pointer button went down on it or on a component inside it, before the press is
     * delivered.
     *
     * @param target the component the press is aimed at
     */
    void pressedWithin(Component<?> target);
}
