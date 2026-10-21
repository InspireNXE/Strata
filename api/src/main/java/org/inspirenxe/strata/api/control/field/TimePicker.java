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
import org.inspirenxe.strata.api.control.attribute.value.HasTime;
import org.inspirenxe.strata.api.text.Text;

import java.time.LocalTime;

/**
 * A field that shows a time of day in parts the user can type into or step, with a button that opens columns of
 * hours, minutes, seconds and the halves of the day in a popup.
 *
 * <h2>The field</h2>
 * <p>The time shows as hour, minute, the second while {@linkplain #isSecondsShown() shown} and, on a 12-hour clock,
 * the half of the day, such as {@code 14:05} or {@code 02:05 PM}. Every number has two digits. The clock and the names
 * of the halves follow the language unless code sets a {@linkplain #setClockFormat clock format}. The field edits the
 * way a {@link DatePicker}'s does: click or Left and Right to pick a part, Up and Down to step it (the minutes by the
 * {@linkplain #minuteStep() minute step}), digits to type, Backspace to empty a part, Enter or focus loss to take the
 * draft, Escape to drop it, and Delete to clear the time while {@linkplain #isClearable() clearable}. On the half of
 * the day, the first letter of either name picks it, and Up and Down flip it. An empty part starts from the current
 * time's.</p>
 *
 * <h2>The popup</h2>
 * <p>The button, Alt and Down, and F4 open a row of lists: hours (12, then 1 to 11, or 0 to 23), minutes in the minute step,
 * seconds while shown, and the halves of the day on a 12-hour clock. Each row is a line plus 4 units tall, at most
 * seven show, and the row of the current value starts in the middle where the column allows. Picking a row sets
 * that part of the time at once; when the time was empty, the other parts start at zero. Focus stays in the field,
 * whose focused part is the column the keys work on: Left and Right move between columns, Up and Down pick the row
 * above or below at once, and Enter and Escape close the popup. The lists are private to the picker and fire no
 * events of their own.</p>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link HasTime#PICK_TIME}: Asked before the user sets the time: a draft taken on Enter or focus loss, a row
 *     picked in the popup, or Delete, which asks with null. A handler may cancel it or set another time, which applies
 *     as {@link #setTime} does. Fires only for the user's actions, and only when the time would change.</li>
 *     <li>{@link HasTime#TIME_PICKED}: Told after the time changed, by the user or from code.</li>
 *     <li>{@link Openable#OPEN}: Asked before the user opens the popup. Cancel it to keep it closed.</li>
 *     <li>{@link Openable#OPENED} and {@link Closable#CLOSED}: Told after the popup opened and closed, from any source.
 *     The picker never fires {@link Closable#CLOSE}.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <p>The field reads the same keys as a {@link DatePicker}'s, with {@value #STYLE_ICON} for the button's icon. The
 * columns draw with the {@link org.inspirenxe.strata.api.control.collection.ListView} row keys and the scroll bar
 * keys, so a picker needs {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_ROW_SELECTED} and
 * {@value org.inspirenxe.strata.api.layout.ScrollPanel#STYLE_THUMB} as well as the keys a text box needs.</p>
 *
 * <pre>{@code
 * TimePicker alarm = factory.timePicker(LocalTime.of(7, 30))
 *         .setMinuteStep(5)
 *         .onEvent(HasTime.TIME_PICKED, event -> this.alarm.setTime(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#timePicker()}. Do not implement this
 * interface.</p>
 */
public interface TimePicker extends Component<TimePicker>, Focusable<TimePicker>, HasCursor<TimePicker>,
        HasTooltip<TimePicker>, HasTime<TimePicker>, Openable<TimePicker> {
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
     * Style key for the icon on a time picker's button (paint, optional). Without it the button draws a down arrow.
     */
    String STYLE_ICON = "picker.icon.time";
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
     * The most rows each column of the popup shows before it scrolls.
     */
    int VISIBLE_ROWS = 7;

    /**
     * Returns what the field shows while empty and unfocused.
     *
     * @return the placeholder; the translated {@code strata.time.placeholder}, "Pick a time", by default
     */
    Text placeholder();

    /**
     * Sets what the field shows while empty and unfocused.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    TimePicker setPlaceholder(Text placeholder);

    /**
     * Sets what the field shows while empty and unfocused, as plain text.
     *
     * @param placeholder the placeholder
     * @return this picker
     */
    default TimePicker setPlaceholder(final String placeholder) {
        return this.setPlaceholder(Text.of(placeholder));
    }

    /**
     * Returns whether the user can clear the time with Delete.
     *
     * @return true by default
     */
    boolean isClearable();

    /**
     * Sets whether the user can clear the time with Delete. Code can always clear it.
     *
     * @param clearable true to let the user clear the time
     * @return this picker
     */
    TimePicker setClearable(boolean clearable);

    /**
     * Opens the popup and fires {@link Openable#OPENED}, but not {@link Openable#OPEN}. Does nothing while the picker
     * is open, disabled or not in a tree.
     *
     * @return this picker
     */
    TimePicker open();

    /**
     * Closes the popup, which fires {@link Closable#CLOSED}. Does nothing while closed.
     *
     * @return this picker
     */
    @Override
    TimePicker close();

    /**
     * A time picker's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param time the {@linkplain #time() time}, or null when empty
     * @param open whether the popup was {@linkplain #isOpen() open}
     */
    record State(LocalTime time, boolean open) {
    }
}
