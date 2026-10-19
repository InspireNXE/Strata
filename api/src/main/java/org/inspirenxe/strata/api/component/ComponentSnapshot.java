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

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;

import java.util.Objects;
import java.util.Optional;

/**
 * The state of one component at the moment an event fired, before any handler or default behavior ran. Compare it
 * with the live component, which {@link #component()} returns, to see what the event changed.
 *
 * <p>A snapshot covers only the component itself. A container's snapshot holds the container's own state and
 * nothing about its children; each child that was captured has a snapshot of its own.</p>
 *
 * <p>Each field holds what the component's getter of the same name returned when the snapshot was taken, or for
 * hover, press and focus, what the component's own code would have read.
 * {@link #state()} holds what the component's kind adds, such as a text box's text and caret: the built-in controls
 * and panels record a nested {@code State} type (for example
 * {@link org.inspirenxe.strata.api.control.field.TextBox.State}), and a
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} records whatever its {@code snapshotState} hook
 * returns.</p>
 *
 * <p>Snapshots are immutable and stay valid after the event is delivered, unlike the event itself. They hold a
 * reference to the live component, so keeping one keeps the component reachable.</p>
 *
 * @param component the live component this snapshot was taken of; never null
 * @param visible whether the component was {@linkplain Component#isVisible() visible}
 * @param enabled whether the component was {@linkplain Component#isEnabled() enabled}
 * @param hovered whether the pointer was over the component, and not over a descendant
 * @param pressed whether a mouse button was held down that went down on the component
 * @param focused whether the component had keyboard focus; always false for one that is not {@link Focusable}
 * @param margin the component's {@linkplain Component#margin() margin}; never null
 * @param minWidth the component's {@linkplain Component#minWidth() minimum width}
 * @param maxWidth the component's {@linkplain Component#maxWidth() maximum width}
 * @param minHeight the component's {@linkplain Component#minHeight() minimum height}
 * @param maxHeight the component's {@linkplain Component#maxHeight() maximum height}
 * @param horizontalAlignment the component's {@linkplain Component#horizontalAlignment() horizontal alignment};
 *                            never null
 * @param verticalAlignment the component's {@linkplain Component#verticalAlignment() vertical alignment}; never
 *                          null
 * @param bounds the component's {@linkplain Component#bounds() bounds}, in the host's coordinates; never null
 * @param desiredWidth the component's {@linkplain Component#desiredWidth() desired width}
 * @param desiredHeight the component's {@linkplain Component#desiredHeight() desired height}
 * @param state what the component's kind records about itself, or null if it records nothing
 */
public record ComponentSnapshot(
        Component<?> component,
        boolean visible,
        boolean enabled,
        boolean hovered,
        boolean pressed,
        boolean focused,
        Insets margin,
        float minWidth,
        float maxWidth,
        float minHeight,
        float maxHeight,
        HorizontalAlignment horizontalAlignment,
        VerticalAlignment verticalAlignment,
        Rect bounds,
        float desiredWidth,
        float desiredHeight,
        Object state
) {

    /**
     * Checks the fields that must not be null.
     */
    public ComponentSnapshot {
        Objects.requireNonNull(component, "component");
        Objects.requireNonNull(margin, "margin");
        Objects.requireNonNull(horizontalAlignment, "horizontalAlignment");
        Objects.requireNonNull(verticalAlignment, "verticalAlignment");
        Objects.requireNonNull(bounds, "bounds");
    }

    /**
     * Returns {@link #state()} as a given type.
     *
     * <pre>{@code
     * event.snapshot().state(TextBox.State.class).ifPresent(before -> ...);
     * }</pre>
     *
     * @param <S> the state type
     * @param type the class the state is expected to be
     * @return the state, or empty if there is none or it is not an instance of {@code type}
     */
    public <S> Optional<S> state(final Class<S> type) {
        Objects.requireNonNull(type, "type");
        return type.isInstance(this.state) ? Optional.of(type.cast(this.state)) : Optional.empty();
    }
}
