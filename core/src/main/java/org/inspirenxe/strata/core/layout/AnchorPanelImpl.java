package org.inspirenxe.strata.core.layout;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.extension.ArrangeContext;
import org.inspirenxe.strata.api.extension.CustomContainer;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.layout.AnchorPanel;

/**
 * The core {@link AnchorPanel}. Every child is measured with the content area and arranged into all of it, where
 * the child's alignment places it. The panel's desired size is that of its largest child on each axis, plus padding.
 */
public final class AnchorPanelImpl extends CustomContainer<AnchorPanel> implements AnchorPanel {

    /**
     * Creates an empty panel.
     *
     * @param ui the factory that owns the panel
     */
    public AnchorPanelImpl(final ComponentFactory ui) {
        super(ui);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final Insets padding = this.padding();
        final float innerWidth = availableWidth - padding.horizontal();
        final float innerHeight = availableHeight - padding.vertical();

        float width = 0f;
        float height = 0f;
        for (final Component<?> child : this.children()) {
            final Size outer = context.measure(child, innerWidth, innerHeight);
            width = Math.max(width, outer.width());
            height = Math.max(height, outer.height());
        }
        return new Size(width + padding.horizontal(), height + padding.vertical());
    }

    @Override
    protected void arrange(final ArrangeContext context, final Rect bounds) {
        final Rect content = this.contentBox();
        for (final Component<?> child : this.children()) {
            context.arrange(child, content);
        }
    }
}
