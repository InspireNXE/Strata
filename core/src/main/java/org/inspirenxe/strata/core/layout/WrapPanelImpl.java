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
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.WrapPanel;
import org.inspirenxe.strata.core.node.BuiltInContainer;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.List;
import java.util.Objects;

/**
 * The core {@link WrapPanel}. Measuring offers each child the content length along the lines and unlimited space
 * across them, then breaks the children into lines where the next child would pass the content length; the panel
 * asks for its longest line and the sum of its line thicknesses and gaps. Arranging breaks the lines again at the
 * content box's length, from the sizes the children asked for, and gives each child a slot as long as it asked for
 * and as thick as its line, placed along the line by the line alignment. Neither pass allocates beyond the slots.
 *
 * <p>The panel's content minimum is the largest outer minimum of its visible children on each axis, plus the
 * padding.</p>
 */
public final class WrapPanelImpl extends BuiltInContainer<WrapPanel> implements WrapPanel {
    private Orientation orientation;
    private float spacing;
    private float lineSpacing;
    private HorizontalAlignment lineAlignment;

    /**
     * Creates an empty panel with no spacing and lines aligned to their start.
     *
     * @param ui the factory that owns the panel
     * @param orientation the direction each line fills in
     */
    public WrapPanelImpl(final ComponentFactoryImpl ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.lineAlignment = HorizontalAlignment.LEFT;
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public WrapPanel setOrientation(final Orientation orientation) {
        if (this.orientation != Objects.requireNonNull(orientation, "orientation")) {
            this.orientation = orientation;
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    public float spacing() {
        return this.spacing;
    }

    @Override
    public float lineSpacing() {
        return this.lineSpacing;
    }

    @Override
    public WrapPanel setSpacing(final float spacing, final float lineSpacing) {
        if (!Float.isFinite(spacing) || !Float.isFinite(lineSpacing)) {
            throw new IllegalArgumentException("Spacing must be finite: " + spacing + ", " + lineSpacing);
        }
        this.spacing = spacing;
        this.lineSpacing = lineSpacing;
        this.invalidateLayout();
        return this;
    }

    @Override
    public HorizontalAlignment lineAlignment() {
        return this.lineAlignment;
    }

    @Override
    public WrapPanel setLineAlignment(final HorizontalAlignment alignment) {
        if (this.lineAlignment != Objects.requireNonNull(alignment, "alignment")) {
            this.lineAlignment = alignment;
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final boolean rows = this.orientation == Orientation.HORIZONTAL;
        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();
        final float limit = Math.max(0f, rows ? innerWidth : innerHeight);

        float longest = 0f;
        float thickness = 0f;
        int lines = 0;
        float along = 0f;
        float lineThickness = 0f;
        int inLine = 0;
        for (final Component<?> child : this.children()) {
            final Size outer = rows
                    ? context.measure(child, innerWidth, Float.POSITIVE_INFINITY)
                    : context.measure(child, Float.POSITIVE_INFINITY, innerHeight);
            if (!child.isVisible()) {
                continue;
            }
            final float length = rows ? outer.width() : outer.height();
            if (inLine > 0 && along + this.spacing + length > limit) {
                longest = Math.max(longest, along);
                thickness += lineThickness;
                lines++;
                along = 0f;
                lineThickness = 0f;
                inLine = 0;
            }
            along += (inLine > 0 ? this.spacing : 0f) + length;
            lineThickness = Math.max(lineThickness, rows ? outer.height() : outer.width());
            inLine++;
        }
        if (inLine > 0) {
            longest = Math.max(longest, along);
            thickness += lineThickness;
            lines++;
        }
        thickness += lines > 1 ? this.lineSpacing * (lines - 1) : 0f;
        return rows
                ? new Size(longest + padding.horizontal(), thickness + padding.vertical())
                : new Size(thickness + padding.horizontal(), longest + padding.vertical());
    }

    @Override
    protected Size measureMinimum(final MeasureContext context) {
        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                continue;
            }
            final Size minimum = context.minimumSize(child);
            width = Math.max(width, minimum.width());
            height = Math.max(height, minimum.height());
        }
        final Insets padding = this.padding();
        return new Size(width + padding.horizontal(), height + padding.vertical());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final boolean rows = this.orientation == Orientation.HORIZONTAL;
        final Rect content = this.contentBox();
        final float limit = Math.max(0f, rows ? content.width() : content.height());
        final List<Component<?>> children = this.children();

        float lineStart = rows ? content.y() : content.x();
        int start = 0;
        while (start < children.size()) {
            // Find the end of the line and its length and thickness
            float along = 0f;
            float thickness = 0f;
            int inLine = 0;
            int end = start;
            for (; end < children.size(); end++) {
                final Component<?> child = children.get(end);
                if (!child.isVisible()) {
                    continue;
                }
                final Size outer = context.outerSize(child);
                final float length = rows ? outer.width() : outer.height();
                if (inLine > 0 && along + this.spacing + length > limit) {
                    break;
                }
                along += (inLine > 0 ? this.spacing : 0f) + length;
                thickness = Math.max(thickness, rows ? outer.height() : outer.width());
                inLine++;
            }
            this.arrangeLine(context, content, children, start, end, lineStart, along, thickness, inLine, limit);
            if (inLine > 0) {
                lineStart += thickness + this.lineSpacing;
            }
            start = end;
        }
    }

    /**
     * Gives the children from {@code start} up to {@code end} their slots in one line.
     */
    private void arrangeLine(final ArrangeContext context, final Rect content, final List<Component<?>> children,
                             final int start, final int end, final float lineStart, final float along,
                             final float thickness, final int inLine, final float limit) {
        final boolean rows = this.orientation == Orientation.HORIZONTAL;
        final float spare = Math.max(0f, limit - along);
        float gap = this.spacing;
        final float offset = switch (this.lineAlignment) {
            case LEFT, STRETCH -> 0f;
            case CENTER -> spare * 0.5f;
            case RIGHT -> spare;
        };
        if (this.lineAlignment == HorizontalAlignment.STRETCH && inLine > 1) {
            gap += spare / (inLine - 1);
        }
        float position = (rows ? content.x() : content.y()) + offset;
        boolean first = true;
        for (int i = start; i < end; i++) {
            final Component<?> child = children.get(i);
            if (!child.isVisible()) {
                context.arrange(child, new Rect(content.x(), content.y(), 0f, 0f));
                continue;
            }
            if (!first) {
                position += gap;
            }
            first = false;
            final Size outer = context.outerSize(child);
            if (rows) {
                context.arrange(child, new Rect(position, lineStart, outer.width(), thickness));
                position += outer.width();
            } else {
                context.arrange(child, new Rect(lineStart, position, thickness, outer.height()));
                position += outer.height();
            }
        }
    }

    @Override
    protected Object snapshotState() {
        return new WrapPanel.State(this.orientation, this.spacing, this.lineSpacing, this.lineAlignment);
    }
}
