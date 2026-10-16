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
 * Decorates a component without replacing it. Every hook receives the component and a {@code next} handle that
 * runs the component's own behavior, much like calling {@code super}:
 * <ul>
 *     <li>a hook left as is runs the component's behavior unchanged;</li>
 *     <li>call {@code next} and add to it to extend the behavior;</li>
 *     <li>skip {@code next} to replace it.</li>
 * </ul>
 * Event hooks run between the {@code BEFORE} and {@code AFTER} handlers, so a canceled event skips them too.
 *
 * @param <C> the component type the behavior decorates
 */
public interface ComponentBehavior<C extends Component<C>> {

    default Size measure(final C component, final MeasureContext context, final float availableWidth,
                         final float availableHeight, final MeasureNext next) {
        return next.proceed();
    }

    /**
     * Draws the component. The background is already drawn; children of a container are drawn afterwards.
     */
    default void paint(final C component, final Painter painter, final Rect bounds, final Next next) {
        next.proceed();
    }

    default void onPointerEnter(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    default void onPointerExit(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    default void onPointerMove(final C component, final PointerEvent event, final Next next) {
        next.proceed();
    }

    default void onPointerPress(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    default void onPointerRelease(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    default void onClick(final C component, final PointerButtonEvent event, final Next next) {
        next.proceed();
    }

    default void onDragStart(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    default void onDrag(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    default void onDragEnd(final C component, final DragEvent event, final Next next) {
        next.proceed();
    }

    default void onScroll(final C component, final ScrollEvent event, final Next next) {
        next.proceed();
    }

    default void onKey(final C component, final KeyEvent event, final Next next) {
        next.proceed();
    }

    default void onChar(final C component, final CharEvent event, final Next next) {
        next.proceed();
    }

    default void onFocusGained(final C component, final FocusEvent event, final Next next) {
        next.proceed();
    }

    default void onFocusLost(final C component, final FocusEvent event, final Next next) {
        next.proceed();
    }
}
