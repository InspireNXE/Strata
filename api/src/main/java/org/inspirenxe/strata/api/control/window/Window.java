package org.inspirenxe.strata.api.control.window;

import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.text.Titled;

/**
 * A framed panel with a title bar, a close button, and content below the bar. Screens use it as their main frame,
 * and dialogs as their box.
 *
 * <h2>Layout</h2>
 * <p>The title bar runs along the top, one line plus {@link #TITLE_PADDING} units above and below tall. Children are
 * laid out below it as in an {@code AnchorPanel}, inside the {@linkplain #padding() padding} (3 units by default).
 * A new window is centered in its slot.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>Dragging the title bar moves the window by its {@linkplain #setOffset offset}, kept inside the slot its
 *     parent gives it, while it {@linkplain #setMovable is movable}.</li>
 *     <li>A click on the close button {@linkplain #close closes} it while it {@linkplain #setClosable is
 *     closable}. So does an Escape press nothing inside consumed, if a handler for {@link #CLOSE} or
 *     {@link #CLOSED} is registered on the window or an ancestor, or the window is shown in an overlay; otherwise
 *     Escape goes on to the engine, which usually closes the screen. Either way the window fires {@link #CLOSE}
 *     first.</li>
 * </ul>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Closable#CLOSE}: Requested when the user closes the window: a click on the close button, or an Escape
 *     press that the window acts on as described above. An Escape press reaches the window when focus is inside it, or
 *     when nothing has focus and the window is the content of the top dialog. Unless a handler cancels the request, the
 *     window does what {@link #close()} does. Cancel it to keep the window open, for example to ask about unsaved
 *     changes first; the Escape press is used either way. An overlay that closes without the window, such as a popup
 *     closed by a press outside it or by an Escape press that never reached the window, fires no request. Fires only
 *     for the user's actions.</li>
 *     <li>{@link Closable#CLOSED}: Reported each time the window closes, before the overlay showing it closes. Fires
 *     for {@link #close()} called from code too.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, required): the whole window, drawn under the title bar.</li>
 *     <li>{@value #STYLE_TITLE_BAR} (paint, optional): the title bar. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_TITLE_TEXT} (text style, optional): the title, centered on the bar. Falls back to the
 *     default style.</li>
 *     <li>{@value #STYLE_CLOSE} (text style, optional): the close button's {@code x}. Falls back to
 *     {@value #STYLE_TITLE_TEXT}.</li>
 *     <li>{@value #STYLE_CLOSE_HOVERED} (text style, optional): the {@code x} while hovered. Falls back to
 *     {@value #STYLE_CLOSE}.</li>
 * </ul>
 *
 * <pre>{@code
 * Window window = factory.window("Exchange").add(content);
 * window.setSize(600, 370);
 * window.onEvent(Closable.CLOSED, event -> handle.close());
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#window(String)}. Do not implement this
 * interface.</p>
 */
public interface Window extends Container<Window>, Titled<Window>, Closable<Window> {
    /**
     * Style key for the window's background (paint, required).
     */
    String STYLE_BACKGROUND = "window.background";
    /**
     * Style key for the title bar (paint, optional).
     */
    String STYLE_TITLE_BAR = "window.title";
    /**
     * Style key for the title text (text style, optional).
     */
    String STYLE_TITLE_TEXT = "window.title.text";
    /**
     * Style key for the close button (text style, optional).
     */
    String STYLE_CLOSE = "window.close";
    /**
     * Style key for the close button while hovered (text style, optional).
     */
    String STYLE_CLOSE_HOVERED = "window.close.hovered";
    /**
     * The space above and below the title in the title bar, in UI units.
     */
    float TITLE_PADDING = 3f;

    /**
     * Returns the title.
     *
     * @return the title
     */
    String title();

    /**
     * Replaces the title.
     *
     * @param title the title
     * @return this window
     */
    Window setTitle(String title);

    /**
     * Returns whether the window can be closed by its close button and Escape.
     *
     * @return true by default
     */
    boolean isClosable();

    /**
     * Sets whether the window can be closed by its close button and Escape. An unclosable window shows no close
     * button.
     *
     * @param closable false to hide the close button and ignore Escape
     * @return this window
     */
    Window setClosable(boolean closable);

    /**
     * Returns whether dragging the title bar moves the window.
     *
     * @return true by default
     */
    boolean isMovable();

    /**
     * Sets whether dragging the title bar moves the window.
     *
     * @param movable false to keep it in place
     * @return this window
     */
    Window setMovable(boolean movable);

    /**
     * Closes the window: reports {@link #CLOSED}, then closes the overlay showing it, if it is in one. A window that
     * is a screen's content stays; close the screen from a {@link #CLOSED} handler. Each call reports, even on a
     * window that is not shown.
     *
     * @return this window
     */
    @Override
    Window close();
}
