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

/**
 * Something the user can be kept from using: every {@link Component}, and the entries that controls hold, a
 * {@link org.inspirenxe.strata.api.control.menu.MenuItem}, {@link org.inspirenxe.strata.api.control.menu.CheckMenuItem},
 * {@link org.inspirenxe.strata.api.control.menu.RadioMenuItem},
 * {@link org.inspirenxe.strata.api.control.menu.SubmenuItem}, {@link org.inspirenxe.strata.api.control.pane.Tab} or
 * {@link org.inspirenxe.strata.api.control.pane.NavigationItem}. Code that takes an {@code Enableable<?>} turns any
 * of them on and off without a branch for each.
 *
 * <p>It has no events and does not extend {@link org.inspirenxe.strata.api.event.EventTarget}, so entries, which are
 * not event targets, can have it. Each implementation says what disabled means for it. Binding goes through
 * {@link Component#ENABLED}, since entries do not bind.</p>
 *
 * <p>Do not implement this interface.</p>
 *
 * @param <S> the implementation's own type, which the setter returns
 */
public interface Enableable<S extends Enableable<S>> {
    /**
     * Returns whether it is enabled.
     *
     * @return true unless disabled with {@code setEnabled(false)}; true by default
     */
    boolean isEnabled();

    /**
     * Enables or disables it. A disabled component or entry draws its disabled look and does not respond to the
     * user; code can still change it.
     *
     * @param enabled false to disable
     * @return this
     */
    S setEnabled(boolean enabled);
}
