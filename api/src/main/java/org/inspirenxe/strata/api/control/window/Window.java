/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.api.control.window;

import org.inspirenxe.strata.api.component.Container;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;

/**
 * A framed panel with a title bar, a close button, and content below the bar. Screens use it as their main frame,
 * and dialogs as their box.
 *
 * <h2>Layout</h2>
 * <p>The title bar runs along the top, one line tall plus {@link #TITLE_PADDING} units above and below. Children are
 * laid out below it as in an {@code AnchorPanel}, inside the {@linkplain #padding() padding} (3 units by default).
 * A new window is centered in its slot.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>A press with any button anywhere in the window, on a child included, {@linkplain Container#raise raises} it
 *     in its parent while it {@linkplain #setRaiseOnPress raises on press}, so of several windows in an
 *     {@code AbsolutePanel} the one pressed comes to the front. It is raised before the press is delivered, whatever
 *     handlers then do with the press.</li>
 *     <li>Dragging the title bar moves the window by its {@linkplain #setOffset offset}, kept inside the slot its
 *     parent gives it, while it {@linkplain #setMovable is movable}. A window the user moved or resized stays inside
 *     that slot when it later shrinks or moves, as when the screen gets smaller; an offset set from code is kept as
 *     set.</li>
 *     <li>A click on the close button {@linkplain #close closes} it while it {@linkplain #setClosable is
 *     closable}. So does an Escape press that nothing inside consumed, if a handler for {@link #CLOSE} or
 *     {@link #CLOSED} is registered on the window or an ancestor, or the window is shown in an overlay; otherwise
 *     Escape goes on to the engine, which usually closes the screen. Either way the window fires {@link #CLOSE}
 *     first. An unclosable window consumes that Escape press without closing or firing anything, so the overlay
 *     showing it stays open.</li>
 *     <li>While the window {@linkplain #setResizable is resizable}, dragging the grip, a {@value #GRIP_SIZE}-unit
 *     square in the bottom-right corner, resizes it with its top-left corner in place. With
 *     {@linkplain #setEdgeResizable edge resizing} on, dragging any edge within {@value #EDGE_SIZE} units resizes it
 *     too, and so does a corner, the part of an edge within {@value #GRIP_SIZE} units of the next edge; the edge
 *     opposite the dragged one stays in place. The new size is kept within the window's minimum and maximum sizes,
 *     never below the width its title bar asks for or the bar's height plus the grip, and inside the slot its parent
 *     gives it. Each resize sets the window's {@linkplain #setSize fixed size} and moves it by its
 *     {@linkplain #setOffset offset}, so its alignment in the slot no longer matters. The pointer shows
 *     {@link org.inspirenxe.strata.api.component.CursorKind#RESIZE_DIAGONAL_DOWN} over the grip and the top left
 *     corner, {@code RESIZE_DIAGONAL_UP} over the other two corners, and
 *     {@code RESIZE_HORIZONTAL} or {@code RESIZE_VERTICAL} over an edge. A child under the grip or an edge that
 *     consumes the press, such as a button, takes it instead, and one with its own cursor shows that.</li>
 * </ul>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Closable#CLOSE}: Requested when the user closes the window: a click on the close button, or an Escape
 *     press that the window acts on as described above. An Escape press reaches the window when focus is inside it, or
 *     when nothing has focus and the window is the content of the top dialog. Unless a handler cancels the request, the
 *     window does what {@link #close()} does. Cancel it to keep the window open, for example to ask about unsaved
 *     changes first; the Escape press is consumed either way. An overlay that closes without the window, such as a
 *     popup closed by a press outside it or by an Escape press that never reached the window, fires no request. Fires
 *     only for the user's actions.</li>
 *     <li>{@link Closable#CLOSED}: Reported each time the window closes, before the overlay showing it closes. Fires
 *     for {@link #close()} called from code too.</li>
 *     <li>{@link #RESIZE}: Requested on each move of a resize drag that would change the window's size. Cancel it to
 *     keep the size for that move, or set another size.</li>
 *     <li>{@link #RESIZED}: Reported after each resize by the user.</li>
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
 *     <li>{@value #STYLE_GRIP} (paint, optional): the grip of a resizable window, drawn over the children as six
 *     2 by 2 squares in a triangle against the bottom-right corner. Nothing is drawn if it is missing.</li>
 *     <li>{@value #STYLE_GRIP_HOVERED} (paint, optional): the grip while the pointer is over the grip or an edge that
 *     resizes, and during a resize. Falls back to {@value #STYLE_GRIP}.</li>
 *     <li>{@value #STYLE_TITLE_PADDING} (insets, optional): the space around the title in the title bar. Defaults to
 *     {@value #TITLE_PADDING} on every side.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): the space between the window's edges, below the title bar, and
 *     its content. Defaults to 3 on every side. Padding set with {@link #setPadding} wins over it.</li>
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
     * Style key for the resize grip (paint, optional).
     */
    String STYLE_GRIP = "window.grip";
    /**
     * Style key for the resize grip while the pointer is over a part that resizes, or during a resize (paint,
     * optional).
     */
    String STYLE_GRIP_HOVERED = "window.grip.hovered";
    /**
     * Style key for the space around the title in the title bar (insets, optional). Defaults to
     * {@value #TITLE_PADDING} on every side.
     */
    String STYLE_TITLE_PADDING = "window.title.padding";
    /**
     * Style key for the space between the window's edges, below the title bar, and its content (insets,
     * optional). Defaults to 3 on every side. Padding set with {@link #setPadding} wins over it.
     */
    String STYLE_PADDING = "window.padding";
    /**
     * The space above and below the title in the title bar, in UI units.
     */
    float TITLE_PADDING = 3f;
    /**
     * The width and height of the resize grip in the bottom-right corner, in UI units.
     */
    float GRIP_SIZE = 10f;
    /**
     * How far in from each edge a press starts resizing that edge, with edge resizing on, in UI units.
     */
    float EDGE_SIZE = 3f;

    /**
     * Fired on each move of a resize drag with the left button that would change the window's size, before it
     * changes. The value is the size the window is about to take, its bounds without the margin, already kept within
     * the limits described in the class documentation. Not fired while the size would stay the same. Canceling it
     * leaves the window as it is for that move; the drag goes on, and the next move asks again. A handler may set
     * another size, which the window keeps within the same limits, with the edges opposite the dragged ones in
     * place; null and NaN sizes are ignored, as if the request were canceled. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<Window, Size>> RESIZE = EventType.valueRequest("window.resize");
    /**
     * Fired after the user resized the window. The value is the new size and the previous value the size before
     * this move, each the window's bounds without the margin. A size set from code with {@link #setSize}, or a new
     * slot for a stretched window, is layout and fires nothing.
     */
    EventType<ValueEvent<Window, Size>> RESIZED = EventType.valueReport("window.resized");

    /**
     * Returns the title.
     *
     * @return the title
     */
    Text title();

    /**
     * Replaces the title.
     *
     * @param title the title
     * @return this window
     */
    Window setTitle(Text title);

    /**
     * Returns whether the window can be closed by its close button and Escape.
     *
     * @return true by default
     */
    boolean isClosable();

    /**
     * Sets whether the window can be closed by its close button and Escape. An unclosable window shows no close
     * button, and an Escape press it would have closed on is consumed, as the class description says.
     *
     * @param closable false to hide the close button and keep Escape from closing the window
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
     * Returns whether the user can resize the window by dragging its grip.
     *
     * @return false by default
     */
    boolean isResizable();

    /**
     * Sets whether the user can resize the window by dragging its grip in the bottom-right corner, and its edges
     * while {@linkplain #isEdgeResizable edge resizing} is on. A resizable window draws the grip.
     *
     * @param resizable true to let the user resize the window
     * @return this window
     */
    Window setResizable(boolean resizable);

    /**
     * Returns whether dragging an edge or corner of a resizable window resizes it too.
     *
     * @return false by default
     */
    boolean isEdgeResizable();

    /**
     * Sets whether dragging an edge or corner of a resizable window resizes it too, on top of the grip. It has no
     * effect while the window is not {@linkplain #setResizable resizable}.
     *
     * @param edgeResizable true to resize from every edge as well
     * @return this window
     */
    Window setEdgeResizable(boolean edgeResizable);

    /**
     * Returns whether a press anywhere in the window raises it in its parent.
     *
     * @return true by default
     */
    boolean isRaiseOnPress();

    /**
     * Sets whether a press anywhere in the window, on a child included, {@linkplain Container#raise raises} it among
     * its siblings with the same z index before the press is delivered. A window with no parent, such as a screen's
     * root or an overlay's content, has nothing to be raised in.
     *
     * @param raiseOnPress false to leave the drawing order alone
     * @return this window
     */
    Window setRaiseOnPress(boolean raiseOnPress);

    /**
     * Closes the window: reports {@link #CLOSED}, then closes the overlay showing it, if it is in one. A window that
     * is a screen's content stays; close the screen from a {@link #CLOSED} handler. Each call reports, even on a
     * window that is not shown.
     *
     * @return this window
     */
    @Override
    Window close();

    /**
     * Returns how far the window is moved right of where its parent's layout puts it.
     *
     * @return the horizontal offset in UI units; 0 for a new window
     */
    float offsetX();

    /**
     * Returns how far the window is moved down from where its parent's layout puts it.
     *
     * @return the vertical offset in UI units; 0 for a new window
     */
    float offsetY();

    /**
     * Moves the window from where its parent's layout puts it, as dragging its title bar does. Siblings and the
     * parent's size do not change. Dragging keeps the window inside its slot; this method does not.
     *
     * @param offsetX the distance to move right, in UI units; negative moves left
     * @param offsetY the distance to move down, in UI units; negative moves up
     * @return this window
     */
    Window setOffset(float offsetX, float offsetY);
}
