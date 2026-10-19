package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates an {@link Image}. Pass one to
 * {@link org.inspirenxe.strata.api.ComponentFactory#image(org.inspirenxe.strata.api.theme.SpriteId, ImageBehavior)};
 * see {@link ComponentBehavior} for how the hooks and {@code next} work.
 */
public interface ImageBehavior extends ComponentBehavior<Image> {
}
