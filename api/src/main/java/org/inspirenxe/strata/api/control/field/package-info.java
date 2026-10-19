/**
 * Controls that edit a value: text or a number: {@link org.inspirenxe.strata.api.control.field.TextBox},
 * {@link org.inspirenxe.strata.api.control.field.TextArea}, {@link org.inspirenxe.strata.api.control.field.Spinner}
 * and {@link org.inspirenxe.strata.api.control.field.Slider}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and its events as constants too:
 * a request for each change the user can make, such as {@code Button.PRESS}, and a report for each change
 * from any source, such as {@code Button.PRESSED}.</p>
 */
package org.inspirenxe.strata.api.control.field;
