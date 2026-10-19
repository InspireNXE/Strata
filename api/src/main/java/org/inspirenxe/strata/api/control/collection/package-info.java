/**
 * Controls that show a collection of items and let the user pick one:
 * {@link org.inspirenxe.strata.api.control.collection.ListView},
 * {@link org.inspirenxe.strata.api.control.collection.ComboBox},
 * {@link org.inspirenxe.strata.api.control.collection.TableView},
 * {@link org.inspirenxe.strata.api.control.collection.TreeView} and
 * {@link org.inspirenxe.strata.api.control.collection.SlotGrid}.
 *
 * <p>Make them with a {@link org.inspirenxe.strata.api.ComponentFactory}. Each control lists the theme style
 * keys it reads as {@code STYLE_*} constants and says which are required, and its events as constants too:
 * a request for each change the user can make, such as {@code Button.PRESS}, and a report for each change
 * from any source, such as {@code Button.PRESSED}.</p>
 */
package org.inspirenxe.strata.api.control.collection;
