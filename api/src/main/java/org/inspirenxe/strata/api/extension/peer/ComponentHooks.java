package org.inspirenxe.strata.api.extension.peer;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.Event;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Set;

/**
 * The calls a peer makes into its component's protected hooks. Each
 * {@link org.inspirenxe.strata.api.extension.CustomComponent} hands a private instance to its peer; each method
 * forwards to the hook of the same name. Not for use outside Strata.
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
     * Forwards to {@code CustomContainer.arrange}; does nothing for a leaf component.
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
     * Forwards to {@code CustomContainer.onChildRemoved}; does nothing for a leaf component.
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
}
