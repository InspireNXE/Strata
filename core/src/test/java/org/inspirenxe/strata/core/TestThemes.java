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

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.Themes;

/**
 * Themes for tests that need a key the flat theme lacks.
 */
final class TestThemes {
    private TestThemes() {
    }

    /**
     * Returns the flat theme with each key added as a metric of 1.
     */
    static Theme flatWith(final String... keys) {
        final Theme.Builder builder = Themes.flat().toBuilder();
        for (final String key : keys) {
            builder.metric(key, 1f);
        }
        return builder.build();
    }
}
