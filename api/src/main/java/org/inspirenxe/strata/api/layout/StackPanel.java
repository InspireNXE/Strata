package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Container;

/**
 * Lays children out one after another along an orientation, with spacing between them. Across the orientation,
 * each child gets the full content size and aligns itself in it. Hidden children take no space.
 */
public interface StackPanel extends Container<StackPanel> {

    Orientation orientation();

    StackPanel setOrientation(Orientation orientation);

    float spacing();

    StackPanel setSpacing(float spacing);
}
