package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.layout.Orientation;

/**
 * A thin line that divides a panel, across or down.
 *
 * <p>A horizontal separator is {@linkplain #setThickness thickness} units tall and stretches across its slot; a
 * vertical one is that wide and stretches down it. It is not focusable and has no default behavior for input.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_LINE} (paint, required): the line.</li>
 * </ul>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#separator(Orientation)}. Do not implement
 * this interface.</p>
 */
public interface Separator extends Component<Separator> {
    /**
     * Style key for the line (paint, required).
     */
    String STYLE_LINE = "separator";

    /**
     * Returns which way the line runs.
     *
     * @return the orientation given when it was made
     */
    Orientation orientation();

    /**
     * Returns the thickness.
     *
     * @return the line's thickness in UI units; 1 by default
     */
    float thickness();

    /**
     * Sets the line's thickness.
     *
     * @param thickness the thickness in UI units
     * @return this separator
     * @throws IllegalArgumentException if the thickness is negative or not finite
     */
    Separator setThickness(float thickness);
}
