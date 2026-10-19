package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.attribute.text.HasText;

import java.util.Optional;

/**
 * One tab of a {@link TabBar}.
 *
 * <p>A tab is made by the {@link org.inspirenxe.strata.api.ComponentFactory}, or by {@link TabBar#addTab(String)}. It
 * belongs to at most one bar at a time. It is mutable: a change to it lays out or redraws its bar. A tab is not a
 * component and has no events of its own; the bar's selection events carry its index, and
 * {@link TabBar#selected()} returns the tab.</p>
 *
 * <pre>{@code
 * Tab sell = factory.tab("Sell")
 *         .setEnabled(false)
 *         .setTooltip(Tooltip.text("There are no items for sale."));
 * sides.addTab(sell);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#tab(String)}. Call tabs on the UI thread. Do not
 * implement this interface.</p>
 */
public interface Tab extends HasText<Tab> {

    /**
     * Returns whether the user can select the tab.
     *
     * @return true by default
     */
    boolean isEnabled();

    /**
     * Sets whether clicks and keys can select the tab. A disabled tab draws its disabled look and still shows its
     * tooltip. Disabling the selected tab keeps it selected.
     *
     * @param enabled false to keep clicks and keys from selecting it
     * @return this tab
     */
    Tab setEnabled(boolean enabled);

    /**
     * Returns the tooltip.
     *
     * @return the tooltip, or empty when there is none
     */
    Optional<Tooltip> tooltip();

    /**
     * Sets the tooltip shown while the pointer rests on the tab, such as one that says why it is disabled.
     *
     * @param tooltip the tooltip, or {@code null} for none
     * @return this tab
     */
    Tab setTooltip(Tooltip tooltip);
}
