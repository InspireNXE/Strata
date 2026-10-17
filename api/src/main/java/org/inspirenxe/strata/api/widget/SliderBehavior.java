package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates a {@link Slider}. Pass one to {@link org.inspirenxe.strata.api.ComponentFactory#slider(SliderBehavior)}; see {@link ComponentBehavior}
 * for how the hooks and {@code next} work.
 */
public interface SliderBehavior extends ComponentBehavior<Slider> {
}
