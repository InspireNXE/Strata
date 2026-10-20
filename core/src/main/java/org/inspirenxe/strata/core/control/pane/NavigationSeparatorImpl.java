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
import org.inspirenxe.strata.api.control.pane.NavigationSeparator;

/**
 * The core {@link NavigationSeparator}.
 */
public final class NavigationSeparatorImpl extends NavigationEntryImpl implements NavigationSeparator {

    /**
     * Creates a separator.
     */
    public NavigationSeparatorImpl() {
    }

    @Override
    NavigationEntry asEntry() {
        return this;
    }

    @Override
    public String toString() {
        return "NavigationSeparator";
    }
}
