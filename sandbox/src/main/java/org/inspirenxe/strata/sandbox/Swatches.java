package org.inspirenxe.strata.sandbox;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.event.PointerButtonEvent;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;

/**
 * A custom component built on the public extension point: a row of color swatches, one of them selected.
 */
final class Swatches extends CustomComponent<Swatches> {
    private static final float SIZE = 14f;
    private static final float GAP = 4f;

    private final int[] colors;
    private int selected;

    Swatches(final Ui ui, final int... colors) {
        super(ui);
        this.colors = colors.clone();
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(this.colors.length * SIZE + (this.colors.length - 1) * GAP, SIZE);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        for (int i = 0; i < this.colors.length; i++) {
            final Rect swatch = new Rect(bounds.x() + i * (SIZE + GAP), bounds.y(), SIZE, SIZE);
            if (i == this.selected) {
                painter.fill(swatch, 0xFFFFFFFF);
                painter.fill(swatch.shrink(Insets.of(2f)), this.colors[i]);
            } else {
                painter.fill(swatch, this.colors[i]);
            }
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        final int index = (int) (event.localX() / (SIZE + GAP));
        if (index >= 0 && index < this.colors.length) {
            this.selected = index;
            event.consume();
        }
    }
}
