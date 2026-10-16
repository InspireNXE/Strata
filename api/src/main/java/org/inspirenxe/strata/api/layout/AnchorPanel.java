package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

/**
 * A container that gives every child the whole content area (its bounds minus padding). Each child then places
 * itself by its own anchor or alignment, so children overlap: use it to pin a title to the top, a close button to a
 * corner and a dialog to the center of the same area. Later children are drawn on top.
 *
 * <p>Its desired size is the largest child's size plus margin, plus the padding. A screen's root is usually an
 * {@code AnchorPanel} anchored with {@link Anchor#FILL}, so it covers the whole host.</p>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#anchorPanel()}. Do not implement this
 * interface.</p>
 */
public interface AnchorPanel extends Container<AnchorPanel> {
}
