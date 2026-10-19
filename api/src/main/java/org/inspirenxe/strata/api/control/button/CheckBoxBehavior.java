package org.inspirenxe.strata.api.control.button;

import org.inspirenxe.strata.api.extension.ComponentBehavior;

/**
 * Decorates a {@link CheckBox}. Pass one to {@link org.inspirenxe.strata.api.ComponentFactory#checkBox(String, CheckBoxBehavior)}; see {@link ComponentBehavior}
 * for how the hooks and {@code next} work.
 */
public interface CheckBoxBehavior extends ComponentBehavior<CheckBox> {
}
