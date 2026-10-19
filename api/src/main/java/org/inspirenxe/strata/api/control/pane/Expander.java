package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * A titled header that shows or hides the component below it.
 *
 * <p>A left click on the header flips it, and so does Enter, keypad Enter or Space while it has focus (not
 * repeated). It consumes those inputs. While collapsed, the content is taken out of the tree, so it takes no space
 * and no input and loses focus if it had it; {@link #content()} still returns it. A disabled expander does not
 * flip.</p>
 *
 * <p>It is focusable and tabbable, and the header shows the hand cursor. The header is one line plus 6 units tall and
 * spans the expander's width: a 7-unit arrow 4 units in from the left, pointing right when collapsed and down when
 * expanded, and the title 4 units after it. The desired size is the wider of the header and the content, and the
 * header's height plus the content's when expanded. The content is placed below the header, across the expander's
 * width, by its own alignment.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_HEADER} (paint, optional): behind the header. Nothing if missing.</li>
 *     <li>{@value #STYLE_HEADER_HOVERED} (paint, optional): behind the header when hovered or focused. Falls back
 *     to {@value #STYLE_HEADER}.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the title and the arrow. Falls back to the default text
 *     style.</li>
 *     <li>{@value #STYLE_TEXT_DISABLED} (text style, optional): the title and the arrow when disabled. Falls back
 *     to {@value #STYLE_TEXT}.</li>
 * </ul>
 *
 * <pre>{@code
 * Expander filters = factory.expander("Filters").setContent(filterPanel).setExpanded(true);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#expander(String)}. Do not implement this
 * interface.</p>
 */
public interface Expander extends Component<Expander> {
    /**
     * Style key for the header (paint, optional).
     */
    String STYLE_HEADER = "expander.header";
    /**
     * Style key for the header when hovered or focused (paint, optional).
     */
    String STYLE_HEADER_HOVERED = "expander.header.hovered";
    /**
     * Style key for the title and arrow (text style, optional).
     */
    String STYLE_TEXT = "expander.text";
    /**
     * Style key for the title and arrow when disabled (text style, optional).
     */
    String STYLE_TEXT_DISABLED = "expander.text.disabled";

    /**
     * Returns the title.
     *
     * @return the title text
     */
    String title();

    /**
     * Replaces the title. A new layout runs.
     *
     * @param title the title, on one line
     * @return this expander
     */
    Expander setTitle(String title);

    /**
     * Returns the content.
     *
     * @return the content, or null if none was set
     */
    Component<?> content();

    /**
     * Replaces the content. The old content is removed from the tree.
     *
     * @param content the component to show below the header, or null for none; it must not have a parent
     * @return this expander
     * @throws IllegalArgumentException if {@code content} already has a parent
     */
    Expander setContent(Component<?> content);

    /**
     * Returns whether the content shows.
     *
     * @return true if expanded; false by default
     */
    boolean isExpanded();

    /**
     * Shows or hides the content. Listeners run if the state changed. Works while disabled too.
     *
     * @param expanded true to show the content
     * @return this expander
     */
    Expander setExpanded(boolean expanded);

    /**
     * Flips the expander as if its header had been clicked, and runs the listeners. Does nothing while disabled.
     */
    void toggle();

    /**
     * Adds a listener told the new state after every change. Listeners run in the order they were added.
     *
     * @param listener receives true when the content was shown
     * @return this expander
     */
    Expander onExpandedChanged(Consumer<Boolean> listener);

    /**
     * An expander's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param expanded whether the content showed
     * @param title the title; never null
     */
    record State(boolean expanded, String title) {

        /**
         * Checks the title.
         */
        public State {
            Objects.requireNonNull(title, "title");
        }
    }
}
