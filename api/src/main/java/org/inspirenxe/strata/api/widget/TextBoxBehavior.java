package org.inspirenxe.strata.api.widget;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates a {@link TextBox}. Pass one to {@link org.inspirenxe.strata.api.ComponentFactory#textBox(TextBoxBehavior)};
 * see {@link ComponentBehavior} for how the hooks and {@code next} work.
 *
 * <pre>{@code
 * // Only digits get through
 * TextBox amount = factory.textBox(new TextBoxBehavior() {
 *     @Override
 *     public void onChar(TextBox box, CharEvent event, Next next) {
 *         if (Character.isDigit(event.codePoint())) {
 *             next.proceed();
 *         } else {
 *             event.consume();
 *         }
 *     }
 * });
 * }</pre>
 */
public interface TextBoxBehavior extends ComponentBehavior<TextBox> {
}
