package org.inspirenxe.strata.api.extension;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.CharEvent;
import org.inspirenxe.strata.api.event.DragEvent;
import org.inspirenxe.strata.api.event.FocusEvent;
import org.inspirenxe.strata.api.event.KeyEvent;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.event.PointerEvent;
import org.inspirenxe.strata.api.event.ScrollEvent;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;

/**
 * Changes what an existing component does without writing a new one. Pass a behavior to a factory method, such
 * as {@code factory.button("Save", behavior)}, and its hooks wrap the component's own.
 *
 * <p>Every hook receives the component and a {@code next} handle that runs the component's own behavior, much
 * like calling {@code super}:</p>
 * <ul>
 *     <li>leave a hook alone and the component behaves as usual;</li>
 *     <li>call {@code next} and add to it to extend the behavior;</li>
 *     <li>do not call {@code next} to replace the behavior.</li>
 * </ul>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Call {@code next} at most once per hook call, and only during that call; it is not valid afterwards.</li>
 *     <li>Event hooks run between the {@link org.inspirenxe.strata.api.event.Phase#BEFORE} and
 *     {@link org.inspirenxe.strata.api.event.Phase#AFTER} handlers, so a canceled event skips them too.</li>
 *     <li>{@link #measure} and {@link #paint} follow the same rules as {@link CustomComponent#measure} and
 *     {@link CustomComponent#paint}: no layout changes, no allocation while painting if avoidable.</li>
 *     <li>A behavior object may be shared by several components; keep per-component state off it, or key it by the
 *     component argument.</li>
 * </ul>
 *
 * @param <C> the component type the behavior decorates
 */
public interface ComponentBehavior<C extends Component<C>> {

    /**
     * Wraps the component's measurement.
     *
     * @param component the component being measured
     * @param context text measurement and child measuring
     * @param availableWidth the width offered
     * @param availableHeight the height offered
     * @param next runs the component's own measurement and returns its result
     * @return the size of the content; the default returns {@code next.proceed()}
     */
    default Size measure(final C component, final MeasureContext context, final float availableWidth,
                         final float availableHeight, final MeasureNext next) {
        return next.proceed();
    }

    /**
     * Wraps the component's drawing. The background is already drawn; a container's children are drawn after this
     * returns.
     *
     * @param component the component being drawn
     * @param painter what to draw with
     * @param bounds the component's bounds
     * @param next draws the component as usual
     */
    default void paint(final C component, final Painter painter, final Rect bounds, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#POINTER_ENTER}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onPointerEnter(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#POINTER_EXIT}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onPointerExit(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#POINTER_MOVE}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onPointerMove(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#POINTER_PRESS}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onPointerPress(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#POINTER_RELEASE}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onPointerRelease(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#CLICK}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onClick(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#DRAG_START}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onDragStart(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#DRAG}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onDrag(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#DRAG_END}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onDragEnd(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#SCROLL}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onScroll(final C component, final ScrollEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#KEY}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onKey(final C component, final KeyEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#CHAR}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onChar(final C component, final CharEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#FOCUS_GAINED}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onFocusGained(final C component, final FocusEvent event, final Next next) {
        next.proceed();
    }

    /**
     * Wraps the default behavior for {@link org.inspirenxe.strata.api.event.EventTypes#FOCUS_LOST}.
     *
     * @param component the component
     * @param event the event
     * @param next runs the component's own behavior
     */
    default void onFocusLost(final C component, final FocusEvent event, final Next next) {
        next.proceed();
    }
}
