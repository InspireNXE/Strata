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
package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The core {@link StackPanel}. Children without a weight are measured with unlimited space along the stacking axis
 * and the content size across it. Weighted children are measured after them, each with its share of the space the
 * others and the gaps leave, or with unlimited space when the panel itself has no limit; the panel asks for the
 * smallest weighted length that gives each its desired size in proportion. Children are then placed one after
 * another with the spacing between them, weighted ones in their share of the room left in the content box. Each slot
 * spans the whole content box across the axis. Hidden children take no space and add no spacing. Spreading widens
 * the gaps only when no visible child has a weight.
 */
public final class StackPanelImpl extends CustomContainer<StackPanel> implements StackPanel {
    private final Map<Component<?>, Float> weights;
    private Orientation orientation;
    private float spacing;
    private boolean spread;

    /**
     * Creates an empty panel with no spacing.
     *
     * @param ui the factory that owns the panel
     * @param orientation the stacking direction
     */
    public StackPanelImpl(final ComponentFactory ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.weights = new IdentityHashMap<>();
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public StackPanel setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.invalidateLayout();
        return this;
    }

    @Override
    public float spacing() {
        return this.spacing;
    }

    @Override
    public StackPanel setSpacing(final float spacing) {
        this.spacing = spacing;
        this.invalidateLayout();
        return this;
    }

    @Override
    public boolean isSpread() {
        return this.spread;
    }

    @Override
    public StackPanel setSpread(final boolean spread) {
        if (this.spread != spread) {
            this.spread = spread;
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public float weight(final Component<?> child) {
        return this.weights.getOrDefault(child, 0f);
    }

    @Override
    public StackPanel setWeight(final Component<?> child, final float weight) {
        if (child.parent() != this) {
            throw new IllegalArgumentException("Not a child of this panel: " + child);
        }
        if (!(weight >= 0f) || Float.isInfinite(weight)) {
            throw new IllegalArgumentException("Weight must be finite and not negative: " + weight);
        }
        final Float previous = weight == 0f ? this.weights.remove(child) : this.weights.put(child, weight);
        if (previous == null ? weight != 0f : previous != weight) {
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    protected void onChildRemoved(final Component<?> child) {
        this.weights.remove(child);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();

        float along = 0f;
        float across = 0f;
        int visible = 0;
        float totalWeight = 0f;
        for (final Component<?> child : this.children()) {
            final float weight = this.weight(child);
            if (weight > 0f) {
                if (child.isVisible()) {
                    totalWeight += weight;
                    visible++;
                }
                continue;
            }
            final Size outer = vertical
                    ? context.measure(child, innerWidth, Float.POSITIVE_INFINITY)
                    : context.measure(child, Float.POSITIVE_INFINITY, innerHeight);
            if (!child.isVisible()) {
                continue;
            }
            along += vertical ? outer.height() : outer.width();
            across = Math.max(across, vertical ? outer.width() : outer.height());
            visible++;
        }
        final float gaps = visible > 1 ? this.spacing * (visible - 1) : 0f;

        // Weighted children are offered their share of what is left; the panel asks for the smallest unit that fits
        final float space = (vertical ? innerHeight : innerWidth) - along - gaps;
        float unit = 0f;
        for (final Component<?> child : this.children()) {
            final float weight = this.weight(child);
            if (weight <= 0f) {
                continue;
            }
            final float share = Float.isInfinite(space) || totalWeight <= 0f
                    ? Float.POSITIVE_INFINITY
                    : Math.max(0f, space) * weight / totalWeight;
            final Size outer = vertical
                    ? context.measure(child, innerWidth, share)
                    : context.measure(child, share, innerHeight);
            if (!child.isVisible()) {
                continue;
            }
            unit = Math.max(unit, (vertical ? outer.height() : outer.width()) / weight);
            across = Math.max(across, vertical ? outer.width() : outer.height());
        }
        along += unit * totalWeight + gaps;

        final float width = (vertical ? across : along) + padding.horizontal();
        final float height = (vertical ? along : across) + padding.vertical();
        return new Size(width, height);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final Rect content = this.contentBox();

        float fixed = 0f;
        float totalWeight = 0f;
        int visible = 0;
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                continue;
            }
            visible++;
            final float weight = this.weight(child);
            if (weight > 0f) {
                totalWeight += weight;
            } else {
                final Size outer = context.outerSize(child);
                fixed += vertical ? outer.height() : outer.width();
            }
        }
        final float length = vertical ? content.height() : content.width();
        final float left = Math.max(0f, length - fixed - (visible > 1 ? this.spacing * (visible - 1) : 0f));
        final float gap = this.spread && totalWeight <= 0f && visible > 1
                ? Math.max(this.spacing, (length - fixed) / (visible - 1))
                : this.spacing;

        float offset = vertical ? content.y() : content.x();
        boolean first = true;
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                context.arrange(child, new Rect(content.x(), content.y(), 0f, 0f));
                continue;
            }
            if (!first) {
                offset += gap;
            }
            first = false;

            final float weight = this.weight(child);
            final float size;
            if (weight > 0f) {
                size = left * weight / totalWeight;
            } else {
                final Size outer = context.outerSize(child);
                size = vertical ? outer.height() : outer.width();
            }
            if (vertical) {
                context.arrange(child, new Rect(content.x(), offset, content.width(), size));
            } else {
                context.arrange(child, new Rect(offset, content.y(), size, content.height()));
            }
            offset += size;
        }
    }

    @Override
    protected Object snapshotState() {
        return new StackPanel.State(this.orientation, this.spacing, this.spread);
    }
}
