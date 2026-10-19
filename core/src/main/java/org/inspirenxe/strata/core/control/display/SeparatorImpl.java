package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.display.Separator;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.VerticalAlignment;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link Separator}. A new one stretches along its line and sits at the start of the other axis.
 */
public final class SeparatorImpl extends CustomComponent<Separator> implements Separator {
    private static final Set<String> REQUIRED = Set.of(STYLE_LINE);

    private final Orientation orientation;
    private float thickness;
    private Paint line;

    /**
     * Creates a separator one unit thick.
     *
     * @param ui the factory that owns the separator
     * @param orientation which way the line runs
     */
    @SuppressWarnings("this-escape")
    public SeparatorImpl(final ComponentFactory ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
        this.thickness = 1f;
        this.line = Paint.NONE;
        if (orientation == Orientation.HORIZONTAL) {
            this.setAlignment(HorizontalAlignment.STRETCH, VerticalAlignment.TOP);
        } else {
            this.setAlignment(HorizontalAlignment.LEFT, VerticalAlignment.STRETCH);
        }
    }

    @Override
    public Orientation orientation() {
        return this.orientation;
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
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return this.orientation == Orientation.HORIZONTAL ? new Size(0f, this.thickness) : new Size(this.thickness, 0f);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        this.line.paint(painter, bounds);
    }
}
