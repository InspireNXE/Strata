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
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.core.node.BuiltInContainer;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The core {@link AbsolutePanel}. Children are measured with unlimited space and get their desired size at their
 * position. A child added without a position sits at the content box's origin. The panel's desired size reaches the
 * right and bottom edges of its farthest child, plus padding.
 */
public final class AbsolutePanelImpl extends BuiltInContainer<AbsolutePanel> implements AbsolutePanel {
    private static final Position ORIGIN = new Position(0f, 0f);

    private final Map<Component<?>, Position> positions;

    /**
     * Creates an empty panel.
     *
     * @param ui the factory that owns the panel
     */
    public AbsolutePanelImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.positions = new IdentityHashMap<>();
    }

    @Override
    public AbsolutePanel add(final Component<?> child, final float x, final float y) {
        this.add(child);
        this.positions.put(child, new Position(x, y));
        return this;
    }

    @Override
    public AbsolutePanel move(final Component<?> child, final float x, final float y) {
        if (this.children().contains(child)) {
            this.positions.put(child, new Position(x, y));
            this.invalidateLayout();
        }
        return this;
    }

    @Override
    protected void onChildRemoved(final Component<?> child) {
        this.positions.remove(child);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.measure(child, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            final Position position = this.positions.getOrDefault(child, ORIGIN);
            width = Math.max(width, position.x + outer.width());
            height = Math.max(height, position.y + outer.height());
        }
        final Insets padding = this.padding();
        return new Size(width + padding.horizontal(), height + padding.vertical());
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
            final Position position = this.positions.getOrDefault(child, ORIGIN);
            width = Math.max(width, position.x + minimum.width());
            height = Math.max(height, position.y + minimum.height());
        }
        final Insets padding = this.padding();
        return new Size(width + padding.horizontal(), height + padding.vertical());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect content = this.contentBox();
        for (final Component<?> child : this.children()) {
            final Position position = this.positions.getOrDefault(child, ORIGIN);
            final Size outer = context.outerSize(child);
            context.arrange(child, new Rect(content.x() + position.x, content.y() + position.y, outer.width(), outer.height()));
        }
    }

    private record Position(float x, float y) {
    }
}
