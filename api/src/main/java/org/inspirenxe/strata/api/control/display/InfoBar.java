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
package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.text.HasText;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;
import java.util.Optional;

/**
 * A strip inside a screen's layout that tells the user something about the screen, such as a failed save or a
 * message from a server: a severity, a title, a message, an optional action and an optional close button. It stays
 * until it is closed, unlike a toast, which closes itself after a while.
 *
 * <p>From the left, the bar shows an accent strip {@value #ACCENT_WIDTH} units wide in the severity's color, the
 * severity's icon when the theme has one, the {@linkplain #title() title} in the title style, the
 * {@linkplain #text() message}, the {@linkplain #setAction action} and, at the right end, the close button. When the
 * title, the message and the action fit on one line beside the close button they share it, centered on it.
 * Otherwise the title and the message wrap, the message below the title, and the action goes below the message. The
 * bar stretches across its slot by default.</p>
 *
 * <p>The close button is an {@code x} like a window's. A click on it, or Enter or Space while it has focus, asks
 * {@link Closable#CLOSE}; unless a handler cancels it, the bar closes and reports {@link Closable#CLOSED}. The
 * button is in the tab order, and its tooltip is the translated {@code strata.button.close}. A closed bar measures
 * zero and takes its action and close button out of the tree, as a collapsed expander takes out its content, so
 * nothing in it holds focus. {@link #open()} shows it again and reports {@link Openable#OPENED}; it never asks
 * {@link Openable#OPEN}, since only code opens a bar. {@code setVisible} stays separate from opening and closing, so
 * layout code that hides a bar does not count as the user closing it.</p>
 *
 * <p>The action is the developer's content, so its control events, such as a button's {@code PRESSED}, go through
 * the bar to its ancestors. Pressing it does not close the bar; a handler does that if wanted. The bar itself takes no
 * focus. Disabled, the bar and everything in it take no input.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link Closable#CLOSE}: Asked when the user presses the close button. Cancel it to keep the bar open.</li>
 *     <li>{@link Closable#CLOSED}: Reported after the bar closed, by the user or with {@link #close()}.</li>
 *     <li>{@link Openable#OPENED}: Reported after {@link #open()} showed a closed bar again.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, required): behind the bar.</li>
 *     <li>{@value #STYLE_BACKGROUND_INFO}, {@value #STYLE_BACKGROUND_SUCCESS}, {@value #STYLE_BACKGROUND_WARNING}
 *     and {@value #STYLE_BACKGROUND_ERROR} (paint, optional): behind a bar of that severity. Each falls back to
 *     {@value #STYLE_BACKGROUND}.</li>
 *     <li>{@value #STYLE_ACCENT_INFO}, {@value #STYLE_ACCENT_SUCCESS}, {@value #STYLE_ACCENT_WARNING} and
 *     {@value #STYLE_ACCENT_ERROR} (paint, optional): the strip down the left edge. Nothing is drawn if it is
 *     missing; the strip's space stays.</li>
 *     <li>{@value #STYLE_ICON_INFO}, {@value #STYLE_ICON_SUCCESS}, {@value #STYLE_ICON_WARNING} and
 *     {@value #STYLE_ICON_ERROR} (paint, optional): the severity's icon, drawn as a square a line tall. A bar whose
 *     theme has none shows no icon and leaves no space for it.</li>
 *     <li>{@value #STYLE_TITLE} (text style, optional): the title. Falls back to {@value #STYLE_TEXT} made
 *     bold.</li>
 *     <li>{@value #STYLE_TEXT} (text style, optional): the message. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_CLOSE} and {@value #STYLE_CLOSE_HOVERED} (text style, optional): the close button's
 *     {@code x}, and while the pointer is over it or it has focus. Each falls back to the window's matching close
 *     button key, then the first to {@value #STYLE_TEXT} and the second to the first.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): between the accent strip and the bar's other edges and its
 *     contents. Defaults to 6 at the sides and 4 at the top and bottom.</li>
 * </ul>
 *
 * <p>The close button draws the theme's {@code button.focus} paint as a ring while it has focus.</p>
 *
 * <pre>{@code
 * InfoBar saved = factory.infoBar(InfoBar.Severity.ERROR, "Not saved", "The server did not answer.")
 *         .setAction("Try again", this::save);
 * panel.add(saved);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#infoBar()}. Do not implement this
 * interface.</p>
 */
public interface InfoBar extends Component<InfoBar>, HasCursor<InfoBar>, HasTooltip<InfoBar>, Titled<InfoBar>,
        HasText<InfoBar>, Openable<InfoBar> {
    /**
     * Style key for behind the bar (paint, required).
     */
    String STYLE_BACKGROUND = "infobar.background";
    /**
     * Style key for behind a bar of {@link Severity#INFO} (paint, optional). Falls back to
     * {@value #STYLE_BACKGROUND}.
     */
    String STYLE_BACKGROUND_INFO = "infobar.background.info";
    /**
     * Style key for behind a bar of {@link Severity#SUCCESS} (paint, optional). Falls back to
     * {@value #STYLE_BACKGROUND}.
     */
    String STYLE_BACKGROUND_SUCCESS = "infobar.background.success";
    /**
     * Style key for behind a bar of {@link Severity#WARNING} (paint, optional). Falls back to
     * {@value #STYLE_BACKGROUND}.
     */
    String STYLE_BACKGROUND_WARNING = "infobar.background.warning";
    /**
     * Style key for behind a bar of {@link Severity#ERROR} (paint, optional). Falls back to
     * {@value #STYLE_BACKGROUND}.
     */
    String STYLE_BACKGROUND_ERROR = "infobar.background.error";
    /**
     * Style key for the accent strip of {@link Severity#INFO} (paint, optional). Nothing is drawn if it is missing.
     */
    String STYLE_ACCENT_INFO = "infobar.accent.info";
    /**
     * Style key for the accent strip of {@link Severity#SUCCESS} (paint, optional). Nothing is drawn if it is
     * missing.
     */
    String STYLE_ACCENT_SUCCESS = "infobar.accent.success";
    /**
     * Style key for the accent strip of {@link Severity#WARNING} (paint, optional). Nothing is drawn if it is
     * missing.
     */
    String STYLE_ACCENT_WARNING = "infobar.accent.warning";
    /**
     * Style key for the accent strip of {@link Severity#ERROR} (paint, optional). Nothing is drawn if it is missing.
     */
    String STYLE_ACCENT_ERROR = "infobar.accent.error";
    /**
     * Style key for the icon of {@link Severity#INFO} (paint, optional). No icon shows if it is missing.
     */
    String STYLE_ICON_INFO = "infobar.icon.info";
    /**
     * Style key for the icon of {@link Severity#SUCCESS} (paint, optional). No icon shows if it is missing.
     */
    String STYLE_ICON_SUCCESS = "infobar.icon.success";
    /**
     * Style key for the icon of {@link Severity#WARNING} (paint, optional). No icon shows if it is missing.
     */
    String STYLE_ICON_WARNING = "infobar.icon.warning";
    /**
     * Style key for the icon of {@link Severity#ERROR} (paint, optional). No icon shows if it is missing.
     */
    String STYLE_ICON_ERROR = "infobar.icon.error";
    /**
     * Style key for the title (text style, optional). Falls back to {@value #STYLE_TEXT} made bold.
     */
    String STYLE_TITLE = "infobar.title";
    /**
     * Style key for the message (text style, optional).
     */
    String STYLE_TEXT = "infobar.text";
    /**
     * Style key for the close button's {@code x} (text style, optional). Falls back to the window's
     * {@code window.close}, then to {@value #STYLE_TEXT}.
     */
    String STYLE_CLOSE = "infobar.close";
    /**
     * Style key for the close button's {@code x} while hovered or focused (text style, optional). Falls back to the
     * window's {@code window.close.hovered}, then to {@value #STYLE_CLOSE}.
     */
    String STYLE_CLOSE_HOVERED = "infobar.close.hovered";
    /**
     * Style key for the space between the accent strip and the bar's other edges and its contents (insets,
     * optional). Defaults to 6 at the sides and 4 at the top and bottom.
     */
    String STYLE_PADDING = "infobar.padding";
    /**
     * The width of the accent strip down the left edge, in UI units.
     */
    float ACCENT_WIDTH = 3f;
    /**
     * Whether the bar shows, as {@link #isOpen()}, {@link #open()} and {@link #close()} say. Binds two ways through
     * {@link Openable#OPENED} and {@link Closable#CLOSED}, so a press on the close button writes false.
     */
    PropertyKey<InfoBar, Boolean> IS_OPEN = PropertyKey.of("info_bar.is_open", InfoBar::isOpen, (bar, open) -> {
        if (open) {
            bar.open();
        } else {
            bar.close();
        }
    }, OPENED, CLOSED);
    /**
     * The severity, as {@link #severity()} and {@link #setSeverity} say. Binds one way.
     */
    PropertyKey<InfoBar, Severity> SEVERITY = PropertyKey.of("info_bar.severity", InfoBar::severity,
            InfoBar::setSeverity);

    /**
     * Returns the severity.
     *
     * @return the severity; {@link Severity#INFO} by default
     */
    Severity severity();

    /**
     * Sets the severity, which picks the background, the accent strip and the icon.
     *
     * @param severity the severity
     * @return this bar
     */
    InfoBar setSeverity(Severity severity);

    /**
     * Returns the title, shown before the message in the title style.
     *
     * @return the title; empty when there is none
     */
    @Override
    Text title();

    /**
     * Replaces the title. The bar lays out again.
     *
     * @param title the title, or {@link Text#empty()} for none
     * @return this bar
     */
    @Override
    InfoBar setTitle(Text title);

    /**
     * Returns the message.
     *
     * @return the message; empty when there is none
     */
    @Override
    Text text();

    /**
     * Replaces the message. The bar lays out again.
     *
     * @param message the message, or {@link Text#empty()} for none
     * @return this bar
     */
    @Override
    InfoBar setText(Text message);

    /**
     * Returns the action.
     *
     * @return the component shown after the message, or empty when there is none
     */
    Optional<Component<?>> action();

    /**
     * Shows a component after the message, such as a button that tries again. It is content, so its control events
     * go through the bar. It replaces the action set before, which leaves the tree. While the bar is closed, the
     * action is kept and joins the tree when the bar opens.
     *
     * @param action the component, or {@code null} to remove the action
     * @return this bar
     * @throws IllegalArgumentException if the component is already in a tree or was made by another factory
     */
    InfoBar setAction(Component<?> action);

    /**
     * Shows a link button with a caption as the action, which runs code when the user presses it. The bar stays
     * open.
     *
     * @param caption the button's caption
     * @param action what pressing it runs
     * @return this bar
     */
    InfoBar setAction(Text caption, Runnable action);

    /**
     * Does the same as {@link #setAction(Text, Runnable)} with literal text.
     *
     * @param caption the button's caption
     * @param action what pressing it runs
     * @return this bar
     */
    default InfoBar setAction(final String caption, final Runnable action) {
        return this.setAction(Text.of(caption), action);
    }

    /**
     * Returns whether the bar shows its close button.
     *
     * @return true by default
     */
    boolean isClosable();

    /**
     * Sets whether the bar shows its close button. A bar without one closes only from code.
     *
     * @param closable false to leave the close button out
     * @return this bar
     */
    InfoBar setClosable(boolean closable);

    /**
     * Returns whether the bar shows its severity's icon when the theme has one.
     *
     * @return true by default
     */
    boolean isIconShown();

    /**
     * Sets whether the bar shows its severity's icon when the theme has one.
     *
     * @param shown false to leave the icon out, with no space kept for it
     * @return this bar
     */
    InfoBar setIconShown(boolean shown);

    /**
     * Returns whether the bar is open.
     *
     * @return true for a new bar, until it is closed
     */
    @Override
    boolean isOpen();

    /**
     * Shows a closed bar again, puts its action and close button back in the tree, and reports
     * {@link Openable#OPENED}. Never asks {@link Openable#OPEN}. Does nothing if the bar is open.
     *
     * @return this bar
     */
    InfoBar open();

    /**
     * Closes the bar, which then measures zero, takes its action and close button out of the tree and reports
     * {@link Closable#CLOSED}. Never asks {@link Closable#CLOSE}. Does nothing if the bar is closed.
     *
     * @return this bar
     */
    @Override
    InfoBar close();

    /**
     * How serious what an {@link InfoBar} says is. It picks the bar's background, accent strip and icon.
     */
    enum Severity {
        /** Something the user may want to know. */
        INFO,
        /** Something went well. */
        SUCCESS,
        /** Something may need the user's attention. */
        WARNING,
        /** Something failed. */
        ERROR
    }

    /**
     * An info bar's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param severity the {@linkplain #severity() severity}; never null
     * @param title the {@linkplain #title() title}; never null
     * @param text the {@linkplain #text() message}; never null
     * @param open whether the bar {@linkplain #isOpen() was open}
     */
    record State(Severity severity, Text title, Text text, boolean open) {

        /**
         * Checks the severity, the title and the message.
         */
        public State {
            Objects.requireNonNull(severity, "severity");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(text, "text");
        }
    }
}
