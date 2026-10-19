/**
 * Controls that arrange, switch between or scroll other content:
 * {@link org.inspirenxe.strata.api.control.pane.TabBar}, {@link org.inspirenxe.strata.api.control.pane.Expander},
 * {@link org.inspirenxe.strata.api.control.pane.SplitPane} and {@link org.inspirenxe.strata.api.control.pane.ScrollBar}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and its events as constants too:
 * a request for each change the user can make, such as {@code Button.PRESS}, and a report for each change
 * from any source, such as {@code Button.PRESSED}.</p>
 */
package org.inspirenxe.strata.api.control.pane;
