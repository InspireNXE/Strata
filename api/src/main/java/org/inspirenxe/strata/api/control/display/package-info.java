/**
 * Controls that show something and take no input: {@link org.inspirenxe.strata.api.control.display.Label},
 * {@link org.inspirenxe.strata.api.control.display.Image},
 * {@link org.inspirenxe.strata.api.control.display.ProgressBar},
 * {@link org.inspirenxe.strata.api.control.display.BusyIndicator} and
 * {@link org.inspirenxe.strata.api.control.display.Separator}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and its events as constants too:
 * a request for each change the user can make, such as {@code Button.PRESS}, and a report for each change
 * from any source, such as {@code Button.PRESSED}.</p>
 */
package org.inspirenxe.strata.api.control.display;
