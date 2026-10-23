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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.visual.Visual;

/**
 * The visual {@link Canvas#pixels} passes to {@link Canvas#visual} by default. No adapter made it, so every canvas
 * draws its placeholder for it.
 */
final class PixelsPlaceholder implements Visual {
    static final Visual INSTANCE = new PixelsPlaceholder();
    private static final Size SIZE = new Size(1f, 1f);

    private PixelsPlaceholder() {
    }

    @Override
    public Size size() {
        return SIZE;
    }

    @Override
    public String toString() {
        return "PixelsPlaceholder";
    }
}
