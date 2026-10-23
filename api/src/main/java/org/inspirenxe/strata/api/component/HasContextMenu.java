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

import org.inspirenxe.strata.api.control.attribute.popup.Popup;
import org.inspirenxe.strata.api.event.EventTarget;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;

import java.util.Optional;

/**
 * A component that can show a context menu. Every container and every control but spacers, separators and busy
 * indicators has one.
 *
 * <p>A right click, the Menu key and Shift+F10 open the context menu of the deepest component under the pointer, or the
 * focused component for the keys, that has one, looking through its ancestors as tooltips do, so a menu on a panel
 * covers what is inside it. A right click on a disabled component opens nothing, as any press on one does nothing. The
 * pointer opens the popup at the pointer, with {@link Popup#openAt}; a key opens it below the component, with
 * {@link Popup#open}. A right click opens it on the release, or on the press where the platform's conventions say
 * buttons act on the press, and only when nothing consumed the click or press. Any {@link Popup} is accepted: a menu,
 * a flyout, or a popup of a developer's own. Text boxes, text areas and password boxes start with a default menu of
 * editing commands.</p>
 *
 * <p>To give a custom component a context menu, extend {@link org.inspirenxe.strata.api.extension.CustomComponent}
 * and declare {@code implements HasContextMenu<Self>}: the class already has these methods. Do not implement this
 * interface any other way.</p>
 *
 * @param <S> the component's own type, which the setter returns
 */
public interface HasContextMenu<S extends HasContextMenu<S>> extends EventTarget<S> {
    /**
     * Requested before the component's context menu opens for the user. The target is the component whose menu
     * opens, and the value the popup. A handler may cancel the request, so nothing opens, or rewrite the popup, for
     * example to a menu built for the row under the pointer; a popup rewritten to null opens nothing, as canceling
     * does. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<HasContextMenu<?>, Popup<?>>> SHOW_CONTEXT_MENU =
            EventType.valueRequest("has_context_menu.show");

    /**
     * Returns the context menu.
     *
     * @return the popup, or empty for none
     */
    Optional<Popup<?>> contextMenu();

    /**
     * Sets the popup that opens as this component's context menu, or clears it. The popup belongs to whoever set it:
     * the component never closes it except as any popup closes.
     *
     * @param popup the popup, or {@code null} for none
     * @return this component
     */
    S setContextMenu(Popup<?> popup);
}
