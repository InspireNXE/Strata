package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

/**
 * Gives every child the whole content box; each child places itself by its own alignment or anchor. Children
 * overlap, later ones on top.
 */
public interface Panel extends Container<Panel> {
}
