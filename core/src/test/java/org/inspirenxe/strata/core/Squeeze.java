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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;

/**
 * A container built on the public extension point that measures its children with its slot's width and unlimited
 * height, and arranges each in a slot of the size the test sets. {@link #squeeze} changes the slot with only a new
 * arrange, so a child can be arranged smaller than it was measured.
 */
public final class Squeeze extends CustomContainer<Squeeze> {
    private float slotWidth;
    private float slotHeight;

    public Squeeze(final ComponentFactory ui, final float slotWidth, final float slotHeight) {
        super(ui);
        this.slotWidth = slotWidth;
        this.slotHeight = slotHeight;
    }

    /**
     * Gives the children a new slot at the next layout, without measuring them again.
     */
    public Squeeze squeeze(final float slotWidth, final float slotHeight) {
        this.slotWidth = slotWidth;
        this.slotHeight = slotHeight;
        this.invalidateArrangement();
        return this;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        for (final Component<?> child : this.children()) {
            context.measure(child, this.slotWidth, Float.POSITIVE_INFINITY);
        }
        return new Size(this.slotWidth, this.slotHeight);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect content = this.contentBox();
        for (final Component<?> child : this.children()) {
            context.arrange(child, new Rect(content.x(), content.y(), this.slotWidth, this.slotHeight));
        }
    }
}
