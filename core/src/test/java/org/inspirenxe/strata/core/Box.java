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
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.Border;

/**
 * A leaf with a fixed natural size, built on the public extension point. It opens up the protected border, offset
 * and press state so tests can reach them.
 */
public final class Box extends CustomComponent<Box> implements HasTooltip<Box> {
    private final float naturalWidth;
    private final float naturalHeight;

    public Box(final ComponentFactory ui, final float naturalWidth, final float naturalHeight) {
        super(ui);
        this.naturalWidth = naturalWidth;
        this.naturalHeight = naturalHeight;
    }

    @Override
    public Box setBorder(final Border border) {
        return super.setBorder(border);
    }

    @Override
    public Box setOffset(final float offsetX, final float offsetY) {
        return super.setOffset(offsetX, offsetY);
    }

    public boolean pressed() {
        return this.isPressed();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(this.naturalWidth, this.naturalHeight);
    }
}
