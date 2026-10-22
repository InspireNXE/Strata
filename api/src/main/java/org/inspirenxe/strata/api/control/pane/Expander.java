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
package org.inspirenxe.strata.api.control.pane;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.text.Titled;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;

import java.util.Objects;

/**
 * A titled header that shows or hides the component below it.
 *
 * <p>A left click on the header flips it, and so does Enter, keypad Enter or Space while it has focus (not
 * repeated). It consumes those inputs. While collapsed, the content is taken out of the tree, so it takes no space
 * and no input and loses focus if it had it; {@link #content()} still returns it. Each flip by the user fires
 * {@link #EXPAND}, and every change fires {@link #EXPANDED}. A disabled expander does not flip.</p>
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
 *     <li>{@value #STYLE_HEADER_PADDING} (insets, optional): the space between the header's edges and its arrow
 *     and text. Defaults to 4 at the sides and 3 at the top and bottom.</li>
 * </ul>
 *
 * <pre>{@code
 * Expander filters = factory.expander("Filters").setContent(filterPanel).setExpanded(true);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#expander(String)}. Do not implement this
 * interface.</p>
 */
public interface Expander extends Component<Expander>, Focusable<Expander>, HasCursor<Expander>, HasTooltip<Expander>,
        Titled<Expander> {
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
     * Style key for the space between the header's edges and its arrow and text (insets, optional). Defaults
     * to 4 at the sides and 3 at the top and bottom.
     */
    String STYLE_HEADER_PADDING = "expander.header.padding";

    /**
     * Fired when the user flips the expander by click or key, before it changes. The value is the state it is about
     * to take: true to show the content. Canceling it leaves the expander as it is. A handler may set another value,
     * which the expander applies like {@link #setExpanded}; null is ignored. Fires only for the user's actions.
     */
    EventType<ValueRequestEvent<Expander, Boolean>> EXPAND = EventType.valueRequest("expander.expand");
    /**
     * Fired after the content was shown or hidden. The value is the new state, true when shown, and the previous
     * value the old one. Fires for changes made from code too.
     */
    EventType<ValueEvent<Expander, Boolean>> EXPANDED = EventType.valueReport("expander.expanded");
    /**
     * Whether the content shows, as {@link #isExpanded()} and {@link #setExpanded} say. Binds two ways through
     * {@link #EXPANDED}. It is named after the getter because the report has the property's name.
     */
    PropertyKey<Expander, Boolean> IS_EXPANDED = PropertyKey.of("expander.is_expanded", Expander::isExpanded,
            Expander::setExpanded, EXPANDED);

    /**
     * Returns the title.
     *
     * @return the title text
     */
    Text title();

    /**
     * Replaces the title. A new layout runs.
     *
     * @param title the title, on one line
     * @return this expander
     */
    Expander setTitle(Text title);

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
     * @throws IllegalArgumentException if {@code content} already has a parent, is a host's content, or is this
     *     expander or one of its ancestors; the expander is left as it was
     */
    Expander setContent(Component<?> content);

    /**
     * Returns whether the content shows.
     *
     * @return true if expanded; false by default
     */
    boolean isExpanded();

    /**
     * Shows or hides the content. Fires {@link #EXPANDED} if the state changed. Works while disabled too.
     *
     * @param expanded true to show the content
     * @return this expander
     */
    Expander setExpanded(boolean expanded);

    /**
     * Flips the expander as if its header had been clicked, firing {@link #EXPANDED} but not {@link #EXPAND}. Does
     * nothing while disabled.
     *
     * @return this expander
     */
    Expander toggle();

    /**
     * An expander's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param expanded whether the content showed
     * @param title the title; never null
     */
    record State(boolean expanded, Text title) {

        /**
         * Checks the title.
         */
        public State {
            Objects.requireNonNull(title, "title");
        }
    }
}
