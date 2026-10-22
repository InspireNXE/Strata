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
package org.inspirenxe.strata.api.control.field;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.Focusable;
import org.inspirenxe.strata.api.component.HasContextMenu;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.text.Text;

import java.time.LocalDate;

/**
 * A field that shows a date in parts the user can type into or step, with a button that opens a
 * {@link CalendarView} in a popup.
 *
 * <h2>The field</h2>
 * <p>The date shows as day, month and year, each a number, in the language's order and with its separator, such as
 * {@code 20.10.2026}, {@code 10/20/2026} or {@code 2026-10-20}. Days and months have two digits and years four.
 * Empty and unfocused, the field shows its placeholder. Focused and empty, each part shows dashes.</p>
 * <ul>
 *     <li>A left press on a part focuses the field and that part. Left and Right move between the parts, and Home
 *     and End go to the first and last; Left on the first part and Right on the last are not consumed. Tab leaves
 *     the field.</li>
 *     <li>Up and Down step the focused part, wrapping within it without carrying into the next, so the 31st steps to
 *     the 1st and the month stays. The year does not wrap. An empty part starts from today's. Held keys repeat.</li>
 *     <li>Digits type into the focused part and move on to the next part when no further digit could fit, so typing
 *     {@code 2}, {@code 0}, {@code 1}, {@code 0} on a day-first field fills the day and the month. Typing a separator
 *     moves on too. Backspace empties the focused part.</li>
 *     <li>What the user types or steps is a draft. It becomes the date on Enter and when the field loses focus, if
 *     every part is filled and they make a date. A day past the end of the month is cut to the last day, and a year
 *     typed with one or two digits becomes the year ending in them in the hundred years around today. A draft that is
 *     incomplete, not a date, outside the range or refused by the filter stays on Enter, and is dropped when the field
 *     loses focus, which then shows the date again. Enter is consumed only when there was a draft, and so is
 *     Escape, which drops the draft, so an Enter or an Escape with nothing to take or drop still reaches a
 *     dialog.</li>
 *     <li>Delete drops the draft and clears the date while the picker is {@linkplain #isClearable() clearable}.</li>
 *     <li>The keys above work only without Control or Alt held. The picker consumes every typed character while it
 *     has focus.</li>
 *     <li>A left press on the button at the right end opens the calendar, as do Alt and Down, and F4 without Control or Alt, while the field
 *     has focus (not repeated). The calendar shows the date's month, or today's, with the picker's range, filter and
 *     first day of the week. Focus moves into it, and its own keys apply. Picking a day sets the date, closes the calendar and returns focus to the field. Escape and a
 *     press outside close it with no change. The calendar is private to the picker and fires no events of its own.</li>
 *     <li>A disabled picker takes no focus, draws the {@link TextBox} disabled face and text, and cannot open.</li>
 * </ul>
 * <p>The picker asks for the width of its widest possible text in the current font and language, plus its padding
 * and the button, so it never resizes as the date changes.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link HasDate#PICK_DATE}: Asked before the user sets the date: a draft taken on Enter or focus loss, a day
 *     picked in the calendar, or Delete, which asks with null. A handler may cancel it or set another date, which
 *     applies as {@link #setDate} does. Fires only for the user's actions, and only when the date would change.</li>
 *     <li>{@link HasDate#DATE_PICKED}: Told after the date changed, by the user or from code.</li>
 *     <li>{@link Openable#OPEN}: Asked before the user opens the calendar. Cancel it to keep it closed.</li>
 *     <li>{@link Openable#OPENED} and {@link Closable#CLOSED}: Told after the calendar opened and closed, from any
 *     source.</li>
 *     <li>{@link Closable#CLOSE}: Asked before Escape or a press outside closes the calendar. Cancel it to keep the
 *     calendar open. Picking a day, Enter, or {@code close()} from code asks nothing.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The field draws with the {@link TextBox} faces, text styles, placeholder style and, unless the picker has its
 * own, padding. The calendar draws with the {@link CalendarView} keys, so a picker needs
 * {@value CalendarView#STYLE_DAY_SELECTED} as well as the keys a text box needs.</p>
 * <ul>
 *     <li>{@value #STYLE_SEGMENT_FOCUSED} (paint, optional): behind the focused part while the field has focus. Falls
 *     back to {@value TextBox#STYLE_SELECTION}.</li>
 *     <li>{@value #STYLE_BUTTON}, {@value #STYLE_BUTTON_HOVERED} and {@value #STYLE_BUTTON_DISABLED} (paints,
 *     optional): the button, hovered and disabled. They fall back to {@value Spinner#STYLE_BUTTON},
 *     {@value Spinner#STYLE_BUTTON_HOVERED} and {@value Spinner#STYLE_BUTTON_DISABLED}, which fall back to the
 *     button keys as on a spinner.</li>
 *     <li>{@value #STYLE_ICON} (paint, optional): the icon on the button. Without it the button draws a down arrow
 *     in the text color, as a combo box does.</li>
 *     <li>{@value #STYLE_POPUP} (paint, optional): behind the popup. Falls back to
 *     {@value org.inspirenxe.strata.api.control.collection.ComboBox#STYLE_POPUP}.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): between the field's edges and its text. Falls back to
 *     {@value TextBox#STYLE_PADDING}.</li>
 * </ul>
 *
 * <pre>{@code
 * DatePicker due = factory.datePicker()
 *         .setDateRange(LocalDate.now(), null)
 *         .onEvent(HasDate.DATE_PICKED, event -> this.task.setDue(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#datePicker()}. Do not implement this
 * interface.</p>
 */
public interface DatePicker extends Component<DatePicker>, Focusable<DatePicker>, HasCursor<DatePicker>,
        HasTooltip<DatePicker>, HasContextMenu<DatePicker>, HasDate<DatePicker>, Openable<DatePicker>,
        Validatable<DatePicker> {
    /**
     * Style key for behind the focused part (paint, optional).
     */
    String STYLE_SEGMENT_FOCUSED = "picker.segment.focused";
    /**
     * Style key for the button that opens the popup (paint, optional).
     */
    String STYLE_BUTTON = "picker.button";
    /**
     * Style key for the button under the pointer (paint, optional).
     */
    String STYLE_BUTTON_HOVERED = "picker.button.hovered";
    /**
     * Style key for the button of a disabled picker (paint, optional).
     */
    String STYLE_BUTTON_DISABLED = "picker.button.disabled";
    /**
     * Style key for the icon on a date picker's button (paint, optional).
     */
    String STYLE_ICON = "picker.icon.date";
    /**
     * Style key for behind the popup (paint, optional).
     */
    String STYLE_POPUP = "picker.popup";
    /**
     * Style key for the space between the field's edges and its text (insets, optional).
     */
    String STYLE_PADDING = "picker.padding";
    /**
     * The width of the button, in UI units.
     */
    float BUTTON_WIDTH = 12f;

    /**
     * Returns what the field shows while empty and unfocused.
     *
     * @return the placeholder; the translated {@code strata.date.placeholder}, "Pick a date", by default
     */
    Text placeholder();

    /**
     * Sets what the field shows while empty and unfocused.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    DatePicker setPlaceholder(Text placeholder);

    /**
     * Sets what the field shows while empty and unfocused, as plain text.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    default DatePicker setPlaceholder(final String placeholder) {
        return this.setPlaceholder(Text.of(placeholder));
    }

    /**
     * Returns whether the user can clear the date with Delete.
     *
     * @return true by default
     */
    boolean isClearable();

    /**
     * Sets whether the user can clear the date with Delete. Code can always clear it.
     *
     * @param clearable true to let the user clear the date
     * @return this picker
     */
    DatePicker setClearable(boolean clearable);

    /**
     * Opens the calendar and fires {@link Openable#OPENED}, but not {@link Openable#OPEN}. Does nothing while the
     * picker is open, disabled or not in a tree.
     *
     * @return this picker
     */
    DatePicker open();

    /**
     * Closes the calendar, which fires {@link Closable#CLOSED}. Does nothing while closed.
     *
     * @return this picker
     */
    @Override
    DatePicker close();

    /**
     * A date picker's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param date the {@linkplain #date() date}, or null when empty
     * @param open whether the calendar was {@linkplain #isOpen() open}
     */
    record State(LocalDate date, boolean open) {
    }
}
