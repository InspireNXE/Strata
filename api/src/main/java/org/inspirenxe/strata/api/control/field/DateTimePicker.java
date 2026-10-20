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
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.control.attribute.popup.Closable;
import org.inspirenxe.strata.api.control.attribute.popup.Openable;
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.control.attribute.value.HasTime;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.text.Text;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;

/**
 * One field for a date and a time of day: the date's parts, a space, then the time's, with a popup that holds a
 * {@link CalendarView} beside the time columns.
 *
 * <h2>The value</h2>
 * <p>The value is one {@link LocalDateTime} with one range, or empty. {@link #date()} and {@link #time()} are its
 * parts. Setting one part of an empty value fills the other: the date with today, kept inside the range, and the time
 * with the {@linkplain #defaultTime() default time}. Clearing either part clears the whole value. The date range is
 * the dates of the range, and {@link #setDateRange} sets the range from the start of the first day to the end of the
 * last. The value is kept inside the range, and the filter refuses dates as on a {@link DatePicker}.</p>
 *
 * <h2>The field and the popup</h2>
 * <p>The field edits as those of {@link DatePicker} and {@link TimePicker} do, one part at a time, and its draft
 * becomes the value only when every part is filled. The button, Alt and Down, and F4 open the calendar beside the time
 * columns. Focus stays in the field. Picking a day sets the date and leaves the popup open, so the time can follow;
 * picking a row sets that part of the time at once. Left and Right move through the field's parts, Up and Down step
 * the focused part and set the value at once, and Enter and Escape close the popup. The calendar and the columns
 * are private to the picker and fire no events of their own.</p>
 *
 * <h2>Events</h2>
 * <p>A change by the user first asks {@link #PICK} with the whole new value, then {@link HasDate#PICK_DATE} if the
 * date changes and {@link HasTime#PICK_TIME} if the time does. Canceling any of them stops the change. Rewriting
 * {@link #PICK} changes both parts, and rewriting {@link HasDate#PICK_DATE} only the date; a part rewritten to null
 * with the other part still set is filled again as above. Every change reports {@link #PICKED}, then
 * {@link HasDate#DATE_PICKED} and {@link HasTime#TIME_PICKED} for the parts that changed, whoever made it.</p>
 * <ul>
 *     <li>{@link #PICK}: Asked before the user sets the value: a draft taken on Enter or focus loss, a day or a row
 *     picked in the popup, or Delete, which asks with null. Fires only for the user's actions, and only when the
 *     value would change.</li>
 *     <li>{@link #PICKED}: Told after the value changed, by the user or from code.</li>
 *     <li>{@link Openable#OPEN}, {@link Openable#OPENED} and {@link Closable#CLOSED}: as on {@link DatePicker}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The field reads the same keys as a {@link DatePicker}'s, the calendar the {@link CalendarView} keys and the
 * columns the list keys, so a picker needs {@value CalendarView#STYLE_DAY_SELECTED},
 * {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_ROW_SELECTED} and
 * {@value org.inspirenxe.strata.api.layout.ScrollPanel#STYLE_THUMB} as well as the keys a text box needs.</p>
 *
 * <pre>{@code
 * DateTimePicker start = factory.dateTimePicker()
 *         .setDefaultTime(LocalTime.of(9, 0))
 *         .onEvent(DateTimePicker.PICKED, event -> this.meeting.setStart(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#dateTimePicker()}. Do not implement this
 * interface.</p>
 */
public interface DateTimePicker extends Component<DateTimePicker>, Focusable<DateTimePicker>,
        HasCursor<DateTimePicker>, HasTooltip<DateTimePicker>, HasDate<DateTimePicker>, HasTime<DateTimePicker>,
        Openable<DateTimePicker> {
    /**
     * Style key for behind the focused part (paint, optional).
     */
    String STYLE_SEGMENT_FOCUSED = DatePicker.STYLE_SEGMENT_FOCUSED;
    /**
     * Style key for the button that opens the popup (paint, optional).
     */
    String STYLE_BUTTON = DatePicker.STYLE_BUTTON;
    /**
     * Style key for the button under the pointer (paint, optional).
     */
    String STYLE_BUTTON_HOVERED = DatePicker.STYLE_BUTTON_HOVERED;
    /**
     * Style key for the button of a disabled picker (paint, optional).
     */
    String STYLE_BUTTON_DISABLED = DatePicker.STYLE_BUTTON_DISABLED;
    /**
     * Style key for the icon on the button (paint, optional), the date picker's. Without it the button draws a down
     * arrow.
     */
    String STYLE_ICON = DatePicker.STYLE_ICON;
    /**
     * Style key for behind the popup (paint, optional).
     */
    String STYLE_POPUP = DatePicker.STYLE_POPUP;
    /**
     * Style key for the space between the field's edges and its text (insets, optional).
     */
    String STYLE_PADDING = DatePicker.STYLE_PADDING;
    /**
     * The width of the button, in UI units.
     */
    float BUTTON_WIDTH = DatePicker.BUTTON_WIDTH;

    /**
     * Asked before the user sets the value or clears it. The value is the date and time about to be set, or null when
     * clearing. A handler may cancel it or set another value, null included. Fires only for the user's actions, and
     * only when the value would change.
     */
    EventType<ValueRequestEvent<DateTimePicker, LocalDateTime>> PICK = EventType.valueRequest("date_time_picker.pick");
    /**
     * Told after the value changed, by the user or from code. The value is the new value and the previous value the
     * old one; either is null when empty.
     */
    EventType<ValueEvent<DateTimePicker, LocalDateTime>> PICKED = EventType.valueReport("date_time_picker.picked");

    /**
     * Returns the value.
     *
     * @return the date and time, or empty
     */
    Optional<LocalDateTime> dateTime();

    /**
     * Sets the value, kept inside the range, or clears it. The seconds are dropped unless shown, and the fraction of
     * a second always. Fires {@link #PICKED}, and then {@link HasDate#DATE_PICKED} and {@link HasTime#TIME_PICKED} for
     * the parts that changed; never a request. Works while disabled too.
     *
     * @param value the value, or null to clear it
     * @return this picker
     */
    DateTimePicker setDateTime(LocalDateTime value);

    /**
     * Returns the earliest value the picker holds.
     *
     * @return the earliest value, or empty for no limit
     */
    Optional<LocalDateTime> minDateTime();

    /**
     * Returns the latest value the picker holds.
     *
     * @return the latest value, or empty for no limit
     */
    Optional<LocalDateTime> maxDateTime();

    /**
     * Sets the range, both ends included, and moves a value outside it to the nearest end.
     *
     * @param min the earliest value, or null for no limit
     * @param max the latest value, or null for no limit
     * @return this picker
     * @throws IllegalArgumentException if both are given and {@code min} is after {@code max}
     */
    DateTimePicker setRange(LocalDateTime min, LocalDateTime max);

    /**
     * Returns the time a date picked into an empty value gets.
     *
     * @return the default time; midnight by default
     */
    LocalTime defaultTime();

    /**
     * Sets the time a date picked into an empty value gets.
     *
     * @param time the time
     * @return this picker
     */
    DateTimePicker setDefaultTime(LocalTime time);

    /**
     * Returns what the field shows while empty and unfocused.
     *
     * @return the placeholder; the translated {@code strata.datetime.placeholder}, "Pick a date and time", by default
     */
    Text placeholder();

    /**
     * Sets what the field shows while empty and unfocused.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    DateTimePicker setPlaceholder(Text placeholder);

    /**
     * Sets what the field shows while empty and unfocused, as plain text.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    default DateTimePicker setPlaceholder(final String placeholder) {
        return this.setPlaceholder(Text.of(placeholder));
    }

    /**
     * Returns whether the user can clear the value with Delete.
     *
     * @return true by default
     */
    boolean isClearable();

    /**
     * Sets whether the user can clear the value with Delete. Code can always clear it.
     *
     * @param clearable true to let the user clear the value
     * @return this picker
     */
    DateTimePicker setClearable(boolean clearable);

    /**
     * Opens the popup and fires {@link Openable#OPENED}, but not {@link Openable#OPEN}. Does nothing while the picker
     * is open, disabled or not in a tree.
     *
     * @return this picker
     */
    DateTimePicker open();

    /**
     * Closes the popup, which fires {@link Closable#CLOSED}. Does nothing while closed.
     *
     * @return this picker
     */
    @Override
    DateTimePicker close();

    /**
     * A date and time picker's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param dateTime the {@linkplain #dateTime() value}, or null when empty
     * @param open whether the popup was {@linkplain #isOpen() open}
     */
    record State(LocalDateTime dateTime, boolean open) {
    }
}
