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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A component that needs one style key, and remembers the factory's theme each time it is given one.
 */
final class Needy extends CustomComponent<Needy> {
    private final ComponentFactory factory;
    private final String key;
    final List<Theme> seen = new ArrayList<>();

    Needy(final ComponentFactory factory, final String key) {
        super(factory);
        this.factory = factory;
        this.key = key;
    }

    Theme current() {
        return this.theme();
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return Set.of(this.key);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.seen.add(this.factory.theme());
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(10, 10);
    }
}
