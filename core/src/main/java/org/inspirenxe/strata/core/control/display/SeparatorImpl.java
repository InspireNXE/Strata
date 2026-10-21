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
package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Separator}. A new one stretches along its line and sits at the start of the other axis.
 */
public final class SeparatorImpl extends BuiltInComponent<Separator> implements Separator {
    private static final Set<String> REQUIRED = Set.of(STYLE_LINE);

    private Orientation orientation;
    private float thickness;
    private Paint line;
    private Paint shadow;

    /**
     * Creates a separator one unit thick.
     *
     * @param ui the factory that owns the separator
     * @param orientation which way the line runs
     */
    @SuppressWarnings("this-escape")
    public SeparatorImpl(final ComponentFactoryImpl ui, final Orientation orientation) {
        super(ui);
        this.thickness = 1f;
        this.line = Paint.NONE;
        this.shadow = Paint.NONE;
        this.setOrientation(orientation);
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
    }

    @Override
    public Separator setOrientation(final Orientation orientation) {
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        if (orientation == Orientation.HORIZONTAL) {
            this.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        } else {
            this.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH);
        }
        this.invalidateLayout();
        return this;
    }

    @Override
    public float thickness() {
        return this.thickness;
    }

    @Override
    public Separator setThickness(final float thickness) {
        if (!(thickness >= 0f) || Float.isInfinite(thickness)) {
            throw new IllegalArgumentException("Thickness must be finite and not negative: " + thickness);
        }
        this.thickness = thickness;
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.line = theme.paintOr(STYLE_LINE, Paint.NONE);
        this.shadow = theme.paintOr(STYLE_SHADOW, Paint.NONE);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final float across = this.shadow != Paint.NONE ? this.thickness * 2f : this.thickness;
        return this.orientation == Orientation.HORIZONTAL ? new Size(0f, across) : new Size(across, 0f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.shadow == Paint.NONE) {
            this.line.paint(painter, bounds);
            return;
        }
        // The line takes the first half across, the shadow the second
        if (this.orientation == Orientation.HORIZONTAL) {
            final float half = bounds.height() * 0.5f;
            this.line.paint(painter, new Rect(bounds.x(), bounds.y(), bounds.width(), half));
            this.shadow.paint(painter, new Rect(bounds.x(), bounds.y() + half, bounds.width(), half));
        } else {
            final float half = bounds.width() * 0.5f;
            this.line.paint(painter, new Rect(bounds.x(), bounds.y(), half, bounds.height()));
            this.shadow.paint(painter, new Rect(bounds.x() + half, bounds.y(), half, bounds.height()));
        }
    }
}
