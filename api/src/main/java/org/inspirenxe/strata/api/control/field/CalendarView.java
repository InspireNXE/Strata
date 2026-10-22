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
import org.inspirenxe.strata.api.control.attribute.value.HasDate;
import org.inspirenxe.strata.api.event.EventType;
import org.inspirenxe.strata.api.event.control.ValueEvent;
import org.inspirenxe.strata.api.event.control.ValueRequestEvent;
import org.inspirenxe.strata.api.state.PropertyKey;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Predicate;

/**
 * A month grid to pick a day from, with a year view and a decade view to move far quickly.
 *
 * <h2>What it shows</h2>
 * <p>A header with a title between a previous and a next arrow. In the month view the title is the month's name and
 * the year, from the translation key {@code strata.calendar.title}; below it are the short names of the weekdays,
 * starting at the {@linkplain #firstDayOfWeek() first day of the week}, and six rows of seven days. There are always
 * six rows, so the calendar keeps its height from month to month. Days of the months before and after fill the rows
 * and draw in the outside style, unless {@linkplain #setOutsideDaysShown hidden}. The year view shows the twelve months
 * of the year in a grid four across and three down, and the decade view twelve years: the decade, with the year before
 * and the year after in the outside style. Month and weekday names, the first day of the week and the order of the
 * title follow the language (see the translation keys reference).</p>
 *
 * <p>Today is marked, and read from the platform's wall clock at most once a minute, so a calendar left open past
 * midnight moves its mark. Days outside the range or refused by the filter draw disabled and cannot be picked. Days
 * the {@linkplain #setMarkFilter mark filter} accepts get a small square under the number.</p>
 *
 * <h2>Input</h2>
 * <ul>
 *     <li>A left click on a day picks it. A click on a day of the month before or after browses there first, then
 *     picks it.</li>
 *     <li>A left click on the title zooms out, from the month to the year and from the year to the decade. A click on
 *     a month or a year zooms back in on it.</li>
 *     <li>The arrows browse by a month, a year or a decade, and stop at the months of the range.</li>
 *     <li>The wheel is ignored, so scrolling over a calendar moves the panel it is in.</li>
 *     <li>While focused, a cursor marks one day: the picked date when it is in view, else today when it is in view,
 *     else the first of the month. In the month view the arrow keys move it by a day or a week, Page Up and Page Down
 *     by a month and, with Shift, by a year, and Home and End to the first and last day of the month. In the year
 *     view the arrow keys move it by a month across and four months up and down, and Page Up and Page Down by a year;
 *     in the decade view by a year across and four years up and down, and Page Up and Page Down by ten years. It
 *     passes over disabled days and stays inside the range. Moving past what is in view browses. Held keys repeat.
 *     In the month view, Enter, keypad Enter and Space pick the cursor's day (not repeated); in the year and decade
 *     views they zoom in on the cursor's month or year. Control and Up zooms out, and Control and Down zooms in.
 *     The calendar consumes these keys, and leaves other keys with Control or Alt alone.</li>
 *     <li>A disabled calendar takes no focus or clicks and draws every day disabled.</li>
 * </ul>
 *
 * <h2>Events</h2>
 * <ul>
 *     <li>{@link HasDate#PICK_DATE}: Asked before the user picks a day, with a click or with Enter or Space. The value
 *     is the day. A handler may cancel it or set another date, which applies as {@link #setDate} does. A day already
 *     picked fires nothing. Fires only for the user's actions.</li>
 *     <li>{@link HasDate#DATE_PICKED}: Told after the date changed, by the user or from code.</li>
 *     <li>{@link #BROWSE}: Asked before the user moves to another month: an arrow, the cursor leaving the month, a
 *     click on a day outside the month, or zooming in on another month. In the year and decade views the value is the
 *     month in view with its year moved. A handler may cancel it or set another month, which is kept inside the
 *     range's months. Fires only for the user's actions, and only when the month would change.</li>
 *     <li>{@link #BROWSED}: Told after the month in view changed, by the user or from code, including
 *     {@link #setDate} showing the new date's month.</li>
 * </ul>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, optional): behind the whole calendar. Nothing if missing.</li>
 *     <li>{@value #STYLE_HEADER_TEXT} (text style, optional): the title. Falls back to the default style.</li>
 *     <li>{@value #STYLE_HEADER_HOVERED} (paint, optional): behind the title or an arrow under the pointer. Falls back
 *     to {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_ROW_HOVERED}.</li>
 *     <li>{@value #STYLE_WEEKDAY_TEXT} (text style, optional): the weekday names. Falls back to
 *     {@value #STYLE_DAY_TEXT_OUTSIDE}.</li>
 *     <li>{@value #STYLE_DAY_TEXT} (text style, optional): days of the month in view, months and years. Falls back to
 *     the default style.</li>
 *     <li>{@value #STYLE_DAY_TEXT_OUTSIDE} (text style, optional): days of the months before and after, and the
 *     years outside the decade. Falls back to {@value #STYLE_DAY_TEXT_DISABLED}.</li>
 *     <li>{@value #STYLE_DAY_TEXT_DISABLED} (text style, optional): days that cannot be picked, and every day while
 *     disabled. Falls back to {@value TextBox#STYLE_TEXT_DISABLED}, then to the default style.</li>
 *     <li>{@value #STYLE_DAY_TEXT_SELECTED} (text style, optional): the picked day. Falls back to
 *     {@value #STYLE_DAY_TEXT}.</li>
 *     <li>{@value #STYLE_DAY_HOVERED} (paint, optional): behind the day under the pointer. Falls back to
 *     {@value org.inspirenxe.strata.api.control.collection.ListView#STYLE_ROW_HOVERED}.</li>
 *     <li>{@value #STYLE_DAY_SELECTED} (paint, required): behind the picked day.</li>
 *     <li>{@value #STYLE_DAY_TODAY} (paint, optional): behind today. Without it, today has a one-unit outline in the
 *     day text's color.</li>
 *     <li>{@value #STYLE_DAY_MARKER} (paint, optional): the square under a marked day. Falls back to the day text's
 *     color.</li>
 *     <li>{@value #STYLE_ARROW} (paint, optional): the arrows. Without it they draw in the header text's color, and in
 *     the disabled text's color at the ends of the range.</li>
 *     <li>{@value #STYLE_FOCUS} (paint, optional): the ring around the cursor's cell while focused. Falls back to
 *     {@value org.inspirenxe.strata.api.control.button.Button#STYLE_FOCUS}.</li>
 *     <li>{@value #STYLE_PADDING} (insets, optional): between the edges and the grid. Defaults to 4 on every side.</li>
 *     <li>{@value #STYLE_CELL_PADDING} (insets, optional): around each day number. Defaults to 3 at the sides and 2 at
 *     the top and bottom.</li>
 * </ul>
 * <p>A day cell draws, from the bottom: today, the picked day, the hover, the number, the marker, and the focus
 * ring.</p>
 *
 * <h2>Size</h2>
 * <p>A cell is a line plus the cell padding's top and bottom tall, and as wide as the widest day number or weekday
 * name plus the padding's sides, but at least as wide as it is tall. The calendar asks for seven cells across, and
 * eight down for the header, the weekday names and the six rows, plus its padding. Given more room, the cells grow to
 * share it.</p>
 *
 * <pre>{@code
 * CalendarView calendar = factory.calendarView()
 *         .setDateRange(LocalDate.now(), null)
 *         .setMarkFilter(bookings::hasBookingOn)
 *         .onEvent(HasDate.DATE_PICKED, event -> this.showBookings(event.value()));
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#calendarView()}. Do not implement this
 * interface.</p>
 */
public interface CalendarView extends Component<CalendarView>, Focusable<CalendarView>, HasCursor<CalendarView>,
        HasTooltip<CalendarView>, HasContextMenu<CalendarView>, HasDate<CalendarView> {
    /**
     * Style key for behind the whole calendar (paint, optional).
     */
    String STYLE_BACKGROUND = "calendar.background";
    /**
     * Style key for the title (text style, optional).
     */
    String STYLE_HEADER_TEXT = "calendar.header.text";
    /**
     * Style key for behind the title or an arrow under the pointer (paint, optional).
     */
    String STYLE_HEADER_HOVERED = "calendar.header.hovered";
    /**
     * Style key for the weekday names (text style, optional).
     */
    String STYLE_WEEKDAY_TEXT = "calendar.weekday.text";
    /**
     * Style key for the days of the month in view, the months and the years (text style, optional).
     */
    String STYLE_DAY_TEXT = "calendar.day.text";
    /**
     * Style key for the days of the months before and after, and the years outside the decade (text style,
     * optional).
     */
    String STYLE_DAY_TEXT_OUTSIDE = "calendar.day.text.outside";
    /**
     * Style key for days that cannot be picked (text style, optional).
     */
    String STYLE_DAY_TEXT_DISABLED = "calendar.day.text.disabled";
    /**
     * Style key for the picked day (text style, optional).
     */
    String STYLE_DAY_TEXT_SELECTED = "calendar.day.text.selected";
    /**
     * Style key for behind the day under the pointer (paint, optional).
     */
    String STYLE_DAY_HOVERED = "calendar.day.hovered";
    /**
     * Style key for behind the picked day (paint, required).
     */
    String STYLE_DAY_SELECTED = "calendar.day.selected";
    /**
     * Style key for behind today (paint, optional).
     */
    String STYLE_DAY_TODAY = "calendar.day.today";
    /**
     * Style key for the square under a marked day (paint, optional).
     */
    String STYLE_DAY_MARKER = "calendar.day.marker";
    /**
     * Style key for the previous and next arrows (paint, optional).
     */
    String STYLE_ARROW = "calendar.arrow";
    /**
     * Style key for the ring around the cursor's cell while focused (paint, optional).
     */
    String STYLE_FOCUS = "calendar.focus";
    /**
     * Style key for the space between the calendar's edges and its grid (insets, optional). Defaults to 4 on every
     * side.
     */
    String STYLE_PADDING = "calendar.padding";
    /**
     * Style key for the space around each day number (insets, optional). Defaults to 3 at the sides and 2 at the top
     * and bottom.
     */
    String STYLE_CELL_PADDING = "calendar.cell.padding";

    /**
     * Asked before the user moves to another month, year or decade. The value is the month about to show. A handler
     * may cancel it or set another month, which is kept inside the range's months. Fires only for the user's actions,
     * and only when the month would change.
     */
    EventType<ValueRequestEvent<CalendarView, YearMonth>> BROWSE = EventType.valueRequest("calendar_view.browse");
    /**
     * Told after the month in view changed, by the user or from code. The value is the new month and the previous
     * value the old one.
     */
    EventType<ValueEvent<CalendarView, YearMonth>> BROWSED = EventType.valueReport("calendar_view.browsed");
    /**
     * The month in view, as {@link #shownMonth()} and {@link #setShownMonth} say. Binds two ways through
     * {@link #BROWSED}. The picked date has its own key, {@link HasDate#DATE}.
     */
    PropertyKey<CalendarView, YearMonth> SHOWN_MONTH = PropertyKey.of("calendar_view.shown_month",
            CalendarView::shownMonth, CalendarView::setShownMonth, BROWSED);

    /**
     * Returns the month in view. In the year and decade views, its year is the one shown or the one the decade holds.
     *
     * @return the month; today's month for a new calendar
     */
    YearMonth shownMonth();

    /**
     * Shows a month, kept inside the range's months and the dates {@link HasDate} supports. Fires {@link #BROWSED} if
     * the month changed, and never
     * {@link #BROWSE}. {@link #setDate} also shows the new date's month.
     *
     * @param month the month
     * @return this calendar
     */
    CalendarView setShownMonth(YearMonth month);

    /**
     * Returns what the calendar shows.
     *
     * @return the zoom; {@link Zoom#MONTH} by default
     */
    Zoom zoom();

    /**
     * Shows the month in view, the twelve months of its year, or twelve years around its decade. Fires no event.
     *
     * @param zoom the zoom
     * @return this calendar
     */
    CalendarView setZoom(Zoom zoom);

    /**
     * Returns whether the days of the months before and after fill the rows.
     *
     * @return true by default
     */
    boolean isOutsideDaysShown();

    /**
     * Shows or hides the days of the months before and after. Hidden days leave their cells empty and cannot be
     * clicked.
     *
     * @param shown true to show them
     * @return this calendar
     */
    CalendarView setOutsideDaysShown(boolean shown);

    /**
     * Sets which days get a small square under their number, such as days with an event. The calendar asks the filter
     * each time it draws, so it may read state that changes.
     *
     * @param marked returns true for a day to mark; no day by default
     * @return this calendar
     */
    CalendarView setMarkFilter(Predicate<? super LocalDate> marked);

    /**
     * What a calendar shows.
     */
    enum Zoom {
        /**
         * The days of one month.
         */
        MONTH,
        /**
         * The twelve months of one year.
         */
        YEAR,
        /**
         * The years of one decade, with the year before and the year after.
         */
        DECADE
    }

    /**
     * A calendar's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param date the picked {@linkplain #date() date}, or null when none is picked
     * @param shownMonth the {@linkplain #shownMonth() month in view}
     * @param zoom the {@linkplain #zoom() zoom}
     */
    record State(LocalDate date, YearMonth shownMonth, Zoom zoom) {
    }
}
