package org.inspirenxe.strata.api.layout;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Container;

/**
 * Places each child at a fixed position relative to the content box, at its desired size.
 * {@link #add(Component)} places a child at the content box's top-left corner.
 */
public interface AbsolutePanel extends Container<AbsolutePanel> {

    AbsolutePanel add(Component<?> child, float x, float y);

    AbsolutePanel move(Component<?> child, float x, float y);
}
