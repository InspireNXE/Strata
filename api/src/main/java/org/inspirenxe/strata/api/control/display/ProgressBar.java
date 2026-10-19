package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;

/**
 * A bar filled from the left to show a fraction, with an optional caption centered on it, such as {@code 12/36}.
 *
 * <p>It asks for 100 units of width and one line plus 4 units of height; stretch it or give it a size. It is not
 * focusable and has no default behavior for input.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_TRACK} (paint, required): the whole bar, under the fill.</li>
 *     <li>{@value #STYLE_FILL} (paint, required): the filled part.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the caption. Falls back to the default style.</li>
 * </ul>
 *
 * <pre>{@code
 * ProgressBar slots = factory.progressBar().setValue(used / (float) limit).setText(used + "/" + limit);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#progressBar()}. Do not implement this
 * interface.</p>
 */
public interface ProgressBar extends Component<ProgressBar> {
    /**
     * Style key for the bar's track (paint, required).
     */
    String STYLE_TRACK = "progress.track";
    /**
     * Style key for the filled part (paint, required).
     */
    String STYLE_FILL = "progress.fill";
    /**
     * Style key for the caption (text style, optional).
     */
    String STYLE_TEXT = "progress.text";

    /**
     * Returns the fraction filled.
     *
     * @return a value from 0 to 1; 0 by default
     */
    float value();

    /**
     * Sets the fraction filled, clamped to 0 to 1. Not a number counts as 0.
     *
     * @param value the fraction
     * @return this bar
     */
    ProgressBar setValue(float value);

    /**
     * Returns the caption.
     *
     * @return the caption; empty by default
     */
    String text();

    /**
     * Sets the caption drawn centered on the bar.
     *
     * @param text the caption, or an empty string for none
     * @return this bar
     */
    ProgressBar setText(String text);
}
