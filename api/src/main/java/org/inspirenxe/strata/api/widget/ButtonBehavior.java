package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates a {@link Button}. Pass one to {@link org.inspirenxe.strata.api.ComponentFactory#button(String,
 * ButtonBehavior)}; see {@link ComponentBehavior} for how the hooks and {@code next} work.
 *
 * <pre>{@code
 * Button save = factory.button("Save", new ButtonBehavior() {
 *     @Override
 *     public void paint(Button button, Painter painter, Rect bounds, Next next) {
 *         next.proceed();                                    // the normal look
 *         painter.sprite(SAVE_ICON, iconArea(bounds), 0xFFFFFFFF);
 *     }
 * });
 * }</pre>
 */
public interface ButtonBehavior extends ComponentBehavior<Button> {
}
