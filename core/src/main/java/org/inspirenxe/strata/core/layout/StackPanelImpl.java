package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.geom.Size;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;

import java.util.Objects;

/**
 * The core {@link StackPanel}. Children are measured with unlimited space along the stacking axis and the content
 * size across it, then placed one after another with the spacing between them. Each slot spans the whole content
 * box across the axis. Hidden children take no space and add no spacing.
 */
public final class StackPanelImpl extends CustomContainer<StackPanel> implements StackPanel {
    private Orientation orientation;
    private float spacing;

    /**
     * Creates an empty panel with no spacing.
     *
     * @param ui the factory that owns the panel
     * @param orientation the stacking direction
     */
    public StackPanelImpl(final ComponentFactory ui, final Orientation orientation) {
        super(ui);
        this.orientation = Objects.requireNonNull(orientation, "orientation");
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
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();

        float along = 0f;
        float across = 0f;
        int visible = 0;
        for (final Component<?> child : this.children()) {
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
        if (visible > 1) {
            along += this.spacing * (visible - 1);
        }

        final float width = (vertical ? across : along) + padding.horizontal();
        final float height = (vertical ? along : across) + padding.vertical();
        return new Size(width, height);
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final boolean vertical = this.orientation == Orientation.VERTICAL;
        final Rect content = this.contentBox();

        float offset = vertical ? content.y() : content.x();
        boolean first = true;
        for (final Component<?> child : this.children()) {
            if (!child.isVisible()) {
                context.arrange(child, new Rect(content.x(), content.y(), 0f, 0f));
                continue;
            }
            if (!first) {
                offset += this.spacing;
            }
            first = false;

            final Size outer = context.outerSize(child);
            if (vertical) {
                context.arrange(child, new Rect(content.x(), offset, content.width(), outer.height()));
                offset += outer.height();
            } else {
                context.arrange(child, new Rect(offset, content.y(), outer.width(), content.height()));
                offset += outer.width();
            }
        }
    }

    @Override
    protected Object snapshotState() {
        return new StackPanel.State(this.orientation, this.spacing);
    }
}
