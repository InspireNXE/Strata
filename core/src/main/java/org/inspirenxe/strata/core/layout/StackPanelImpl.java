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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.core.node.BuiltInContainer;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * The core {@link StackPanel}. Children without a weight are measured with unlimited space along the stacking axis
 * and the content size across it. Weighted children are measured after them, each with its share of the space the
 * others and the gaps leave, or with unlimited space when the panel itself has no limit; the panel asks for the
 * smallest weighted length that gives each its desired size in proportion, less what a child's maximum size cuts
 * from its part. Children are then placed one after another with the spacing between them, weighted ones in their
 * share of the room left in the content box. Each slot spans the whole content box across the axis. Hidden children
 * take no space and add no spacing. Spreading widens the gaps only when no visible child has a weight.
 *
 * <p>Shares respect each weighted child's minimum and maximum size along the axis, margin included: a child whose
 * share would cross a limit gets the limit, and the other weighted children share what is left, as
 * {@link WeightedShares} describes. The lower limit is the child's outer minimum
 * ({@link MeasureContext#minimumSize}), so a content minimum or a fixed size holds a share up too.</p>
 *
 * <p>In a row, children aligned {@link org.inspirenxe.strata.api.layout.VerticalAlignment#BASELINE} are lined up
 * as {@link BaselineRow} describes: the row is at least as tall as they need lined up, and each one's slot starts
 * where its baseline meets theirs, as tall as the child asked for.</p>
 *
 * <p>The panel's own content minimum is its children's minimums along the axis plus the gaps, the largest across,
 * and the padding.</p>
 */
public final class StackPanelImpl extends BuiltInContainer<StackPanel> implements StackPanel {
    private final Map<Component<?>, Float> weights;
    // Reused between layouts; one slot per child, 0 weight for children that take no share
    private final WeightedShares shares;
    private final BaselineRow baselines;
    private Orientation orientation;
    private float spacing;
    private boolean spread;

    /**
     * Creates an empty panel with no spacing.
     *
     * @param ui the factory that owns the panel
     * @param orientation the stacking direction
     */
    public StackPanelImpl(final ComponentFactoryImpl ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.weights = new IdentityHashMap<>();
        this.shares = new WeightedShares();
        this.baselines = new BaselineRow();
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
        if (!Float.isFinite(spacing)) {
            throw new IllegalArgumentException("Spacing must be finite: " + spacing);
        }
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

        final List<Component<?>> children = this.children();
        final BaselineRow baselines = this.baselines;
        baselines.reset();
        float along = 0f;
        float across = 0f;
        int visible = 0;
        float totalWeight = 0f;
        for (final Component<?> child : children) {
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
            if (!vertical && BaselineRow.aligns(child)) {
                baselines.add(context.baseline(child), outer.height());
            }
            visible++;
        }
        final float gaps = visible > 1 ? this.spacing * (visible - 1) : 0f;

        // Weighted children are offered their share of what is left; the panel asks for the smallest unit that fits
        final float space = (vertical ? innerHeight : innerWidth) - along - gaps;
        final boolean bounded = !Float.isInfinite(space) && totalWeight > 0f;
        this.weighShares(children, vertical, context::minimumSize);
        if (bounded) {
            this.shares.resolve(space);
        }
        for (int i = 0; i < children.size(); i++) {
            final Component<?> child = children.get(i);
            if (this.weight(child) <= 0f) {
                continue;
            }
            final float share = bounded && child.isVisible() ? this.shares.size(i) : Float.POSITIVE_INFINITY;
            final Size outer = vertical
                    ? context.measure(child, innerWidth, share)
                    : context.measure(child, share, innerHeight);
            if (!child.isVisible()) {
                continue;
            }
            this.shares.need(i, vertical ? outer.height() : outer.width());
            across = Math.max(across, vertical ? outer.width() : outer.height());
            if (!vertical && BaselineRow.aligns(child)) {
                baselines.add(context.baseline(child), outer.height());
            }
        }
        along += this.shares.fit() + gaps;
        across = Math.max(across, baselines.height());

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
        final List<Component<?>> children = this.children();
        this.weighShares(children, vertical, context::minimumSize);
        this.shares.resolve(left);
        final BaselineRow baselines = this.baselines;
        baselines.reset();
        if (!vertical) {
            for (final Component<?> child : children) {
                if (BaselineRow.aligns(child)) {
                    baselines.add(context.baseline(child), context.outerSize(child).height());
                }
            }
        }

        float offset = vertical ? content.y() : content.x();
        boolean first = true;
        for (int i = 0; i < children.size(); i++) {
            final Component<?> child = children.get(i);
            if (!child.isVisible()) {
                context.arrange(child, new Rect(content.x(), content.y(), 0f, 0f));
                continue;
            }
            if (!first) {
                offset += gap;
            }
            first = false;

            final float size;
            if (this.weight(child) > 0f) {
                size = this.shares.size(i);
            } else {
                final Size outer = context.outerSize(child);
                size = vertical ? outer.height() : outer.width();
            }
            if (vertical) {
                context.arrange(child, new Rect(content.x(), offset, content.width(), size));
            } else if (BaselineRow.aligns(child)) {
                final float height = context.outerSize(child).height();
                context.arrange(child, new Rect(offset, baselines.top(content.y(), context.baseline(child), height),
                        size, height));
            } else {
                context.arrange(child, new Rect(offset, content.y(), size, content.height()));
            }
            offset += size;
        }
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        float along = 0f;
        float across = 0f;
        int visible = 0;
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                continue;
            }
            final Size minimum = context.minimumSize(child);
            along += vertical ? minimum.height() : minimum.width();
            across = Math.max(across, vertical ? minimum.width() : minimum.height());
            visible++;
        }
        along += visible > 1 ? this.spacing * (visible - 1) : 0f;
        final Insets padding = this.padding();
        return new Size((vertical ? across : along) + padding.horizontal(), (vertical ? along : across) + padding.vertical());
    }

    /**
     * Gives the shares one slot per child: each visible weighted child its weight and its outer limits along the
     * axis, every other child none. The lower limit is the child's outer minimum, which takes in its content
     * minimum, its minimum size and its fixed size.
     */
    private void weighShares(final List<Component<?>> children, final boolean vertical,
                             final Function<Component<?>, Size> minimums) {
        this.shares.reset(children.size());
        for (int i = 0; i < children.size(); i++) {
            final Component<?> child = children.get(i);
            final float weight = this.weight(child);
            if (weight > 0f && child.isVisible()) {
                final Insets margin = child.margin();
                final float outside = vertical ? margin.vertical() : margin.horizontal();
                final Size minimum = minimums.apply(child);
                this.shares.set(i, weight, vertical ? minimum.height() : minimum.width(),
                        (vertical ? child.maxHeight() : child.maxWidth()) + outside);
            }
        }
    }

    @Override
    protected Object snapshotState() {
        return new StackPanel.State(this.orientation, this.spacing, this.spread);
    }
}
