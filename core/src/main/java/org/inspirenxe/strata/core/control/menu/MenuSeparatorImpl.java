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
package org.inspirenxe.strata.core.control.menu;

import org.inspirenxe.strata.api.control.menu.MenuEntry;
import org.inspirenxe.strata.api.control.menu.MenuSeparator;

/**
 * The core {@link MenuSeparator}.
 */
public final class MenuSeparatorImpl extends MenuEntryImpl implements MenuSeparator {

    /**
     * Creates a separator.
     */
    public MenuSeparatorImpl() {
        super();
    }

    @Override
    MenuEntry entry() {
        return this;
    }

    @Override
    boolean separator() {
        return true;
    }

    @Override
    public String toString() {
        return "MenuSeparator";
    }
}
