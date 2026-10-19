package org.inspirenxe.strata.api.component;

/**
 * A component that can choose the pointer's cursor. Every container and nearly every control has one.
 *
 * <p>The host shows the cursor of the deepest component under the pointer that has one set, looking through its
 * ancestors, and {@link CursorKind#ARROW} when none has. A cursor set on a panel therefore covers what is inside it.
 * Engines without a matching cursor show the closest one they have.</p>
 *
 * <p>To give a custom component a cursor, extend {@link org.inspirenxe.strata.api.extension.CustomComponent} and
 * declare {@code implements HasCursor<Self>}: the class already has both methods. Do not implement this interface any
 * other way.</p>
 *
 * @param <S> the component's own type, which the setter returns
 */
public interface HasCursor<S extends HasCursor<S>> {

    /**
     * Returns the cursor shown while the pointer is over this component.
     *
     * @return the cursor, or {@code null} to use the nearest ancestor's
     */
    CursorKind cursor();

    /**
     * Sets the cursor shown while the pointer is over this component or a descendant that sets none.
     *
     * @param cursor the cursor, or {@code null} to use the nearest ancestor's
     * @return this component
     */
    S setCursor(CursorKind cursor);
}
