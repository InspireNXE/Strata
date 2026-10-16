package org.inspirenxe.strata.examples.extending;

// --8<-- [start:wrap]
import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;

/**
 * Lays children out left to right and starts a new row when the next child would not fit, like words in a
 * paragraph. Hidden children measure as zero and take no gap.
 */
public final class WrapPanel extends CustomContainer<WrapPanel> {
    private final float gap;

    public WrapPanel(final ComponentFactory factory, final float gap) {
        super(factory);
        this.gap = gap;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        // Measure every child, hidden ones included, so none keeps a stale size
        final float inner = availableWidth - this.padding().horizontal();
        float x = 0f;
        float rowHeight = 0f;
        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.measure(child, inner, Float.POSITIVE_INFINITY);
            if (outer.width() == 0f && outer.height() == 0f) {
                continue;
            }
            if (x > 0f && x + outer.width() > inner) {
                height += rowHeight + this.gap;
                x = 0f;
                rowHeight = 0f;
            }
            x += outer.width() + this.gap;
            rowHeight = Math.max(rowHeight, outer.height());
            width = Math.max(width, x - this.gap);
        }
        height += rowHeight;
        return new Size(width + this.padding().horizontal(), height + this.padding().vertical());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        // Place children inside the content box, which leaves out the padding
        final Rect content = this.contentBox();
        float x = 0f;
        float y = 0f;
        float rowHeight = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.outerSize(child);
            if (x > 0f && x + outer.width() > content.width()) {
                y += rowHeight + this.gap;
                x = 0f;
                rowHeight = 0f;
            }
            // Arrange every child, hidden ones included, so none keeps stale bounds
            context.arrange(child, new Rect(content.x() + x, content.y() + y, outer.width(), outer.height()));
            if (outer.width() > 0f || outer.height() > 0f) {
                x += outer.width() + this.gap;
                rowHeight = Math.max(rowHeight, outer.height());
            }
        }
    }
}
// --8<-- [end:wrap]
