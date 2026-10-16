package org.inspirenxe.strata.core.node;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.text.TextMetrics;

/**
 * The context handed to measure and arrange hooks. One per host, reused for every layout.
 */
public final class LayoutPass implements MeasureContext, ArrangeContext {
    private final CoreUi ui;
    private final TextMetrics text;

    public LayoutPass(final CoreUi ui, final TextMetrics text) {
        this.ui = ui;
        this.text = text;
    }

    @Override
    public TextMetrics text() {
        return this.text;
    }

    @Override
    public Size measure(final Component<?> child, final float availableWidth, final float availableHeight) {
        final Node node = this.ui.nodeOf(child);
        node.measure(this, availableWidth, availableHeight);
        return new Size(node.outerWidth(), node.outerHeight());
    }

    @Override
    public Size outerSize(final Component<?> child) {
        final Node node = this.ui.nodeOf(child);
        return new Size(node.outerWidth(), node.outerHeight());
    }

    @Override
    public void arrange(final Component<?> child, final Rect slot) {
        this.ui.nodeOf(child).arrange(this, slot);
    }
}
