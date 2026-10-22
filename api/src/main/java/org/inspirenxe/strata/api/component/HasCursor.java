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

import java.util.Optional;

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
     * @return the cursor, or empty to use the nearest ancestor's
     */
    Optional<CursorKind> cursor();

    /**
     * Sets the cursor shown while the pointer is over this component or a descendant that sets none.
     *
     * @param cursor the cursor, or {@code null} to use the nearest ancestor's
     * @return this component
     */
    S setCursor(CursorKind cursor);
}
