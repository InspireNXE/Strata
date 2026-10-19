package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates a {@link Label}. Pass one to {@link org.inspirenxe.strata.api.ComponentFactory#label(String,
 * LabelBehavior)}; see {@link ComponentBehavior} for how the hooks and {@code next} work.
 */
public interface LabelBehavior extends ComponentBehavior<Label> {
}
