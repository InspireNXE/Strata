package org.inspirenxe.strata.api.component;

import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Paint;

import java.util.Objects;
import java.util.Optional;

/**
 * The state of one component at the moment an event fired, before any handler or default behavior ran. Compare it
 * with the live component, which {@link #component()} returns, to see what the event changed.
 *
 * <p>A snapshot covers only the component itself. A container's snapshot holds the container's own state and
 * nothing about its children; each child that was captured has a snapshot of its own.</p>
 *
 * <p>Each field holds what the {@link Component} getter of the same name returned when the snapshot was taken.
 * {@link #state()} holds what the component's kind adds, such as a text box's text and caret: the built-in widgets
 * and panels record a nested {@code State} type (for example
 * {@link org.inspirenxe.strata.api.widget.TextBox.State}), and a
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} records whatever its {@code snapshotState} hook
 * returns.</p>
 *
 * <p>Snapshots are immutable and stay valid after the event is delivered, unlike the event itself. They hold a
 * reference to the live component, so keeping one keeps the component reachable.</p>
 *
 * @param component the live component this snapshot was taken of; never null
 * @param id the component's {@linkplain Component#id() id}, or null
 * @param visible whether the component was {@linkplain Component#isVisible() visible}
 * @param enabled whether the component was {@linkplain Component#isEnabled() enabled}
 * @param focusable whether the component was {@linkplain Component#isFocusable() focusable}
 * @param tabbable whether the component was {@linkplain Component#isTabbable() tabbable}
 * @param tabIndex the component's {@linkplain Component#tabIndex() tab index}
 * @param cursor the component's {@linkplain Component#cursor() cursor}, or null
 * @param hovered whether the component was {@linkplain Component#isHovered() hovered}
 * @param pressed whether the component was {@linkplain Component#isPressed() pressed}
 * @param focused whether the component had {@linkplain Component#isFocused() focus}
 * @param margin the component's {@linkplain Component#margin() margin}; never null
 * @param minWidth the component's {@linkplain Component#minWidth() minimum width}
 * @param maxWidth the component's {@linkplain Component#maxWidth() maximum width}
 * @param minHeight the component's {@linkplain Component#minHeight() minimum height}
 * @param maxHeight the component's {@linkplain Component#maxHeight() maximum height}
 * @param horizontalAlignment the component's {@linkplain Component#horizontalAlignment() horizontal alignment};
 *                            never null
 * @param verticalAlignment the component's {@linkplain Component#verticalAlignment() vertical alignment}; never
 *                          null
 * @param background the component's {@linkplain Component#background() background}; never null
 * @param bounds the component's {@linkplain Component#bounds() bounds}, in the host's coordinates; never null
 * @param desiredWidth the component's {@linkplain Component#desiredWidth() desired width}
 * @param desiredHeight the component's {@linkplain Component#desiredHeight() desired height}
 * @param state what the component's kind records about itself, or null if it records nothing
 */
public record ComponentSnapshot(
        Component<?> component,
        String id,
        boolean visible,
        boolean enabled,
        boolean focusable,
        boolean tabbable,
        int tabIndex,
        CursorKind cursor,
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
        Paint background,
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
        Objects.requireNonNull(background, "background");
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
