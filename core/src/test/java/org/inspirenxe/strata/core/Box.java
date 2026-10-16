package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.Ui;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Size;

/**
 * A leaf with a fixed natural size, built on the public extension point.
 */
public final class Box extends CustomComponent<Box> {
    private final float naturalWidth;
    private final float naturalHeight;

    public Box(final Ui ui, final float naturalWidth, final float naturalHeight) {
        super(ui);
        this.naturalWidth = naturalWidth;
        this.naturalHeight = naturalHeight;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        return new Size(this.naturalWidth, this.naturalHeight);
    }
}
