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
package org.inspirenxe.strata.core.control.pane;

import org.inspirenxe.strata.api.control.pane.NavigationEntry;

/**
 * What core's navigation entries share: the view or item that holds them, and telling their view when they change.
 * The view draws each entry through a private row component of its own.
 */
public abstract sealed class NavigationEntryImpl permits NavigationItemImpl, NavigationHeaderImpl,
        NavigationSeparatorImpl {
    // The NavigationViewImpl or the NavigationItemImpl that holds this entry, or null
    private Object owner;

    NavigationEntryImpl() {
    }

    /**
     * Returns this entry as the API type it implements.
     *
     * @return this entry
     */
    abstract NavigationEntry asEntry();

    /**
     * Returns what holds the entry.
     *
     * @return the view or the parent item, or {@code null}
     */
    final Object owner() {
        return this.owner;
    }

    /**
     * Gives the entry to a view or an item, or takes it back with null.
     *
     * @throws IllegalArgumentException if the entry is given while something already holds it
     */
    final void setOwner(final Object owner) {
        if (owner != null && this.owner != null) {
            throw new IllegalArgumentException(this + " already belongs to a navigation view or item");
        }
        this.owner = owner;
    }

    /**
     * Returns the view the entry is in, through its parent items.
     *
     * @return the view, or {@code null} while the entry is in none
     */
    final NavigationViewImpl view() {
        Object at = this.owner;
        while (at instanceof final NavigationItemImpl item) {
            at = item.owner();
        }
        return at instanceof final NavigationViewImpl view ? view : null;
    }

    /**
     * Tells the entry's view, if any, that the entry changed how it looks.
     */
    final void changed() {
        final NavigationViewImpl view = this.view();
        if (view != null) {
            view.entryChanged(this);
        }
    }
}
