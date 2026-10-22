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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.CalendarView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.FocusEvent;
import org.inspirenxe.strata.api.event.input.KeyEvent;
import org.inspirenxe.strata.api.event.input.PointerButtonEvent;
import org.inspirenxe.strata.api.event.input.PointerEvent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.control.Rings;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.DateFormats;
import org.inspirenxe.strata.core.text.TextLine;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The core {@link CalendarView}. It paints everything itself, with no children, as the color picker and the menu do.
 *
 * <p>The day numbers, month names and weekday names are resolved and measured into {@link TextLine}s during measure,
 * in each of the four day styles. The cell rectangles and the 42 dates of the month grid are worked out when the
 * bounds, the month in view or the first day of the week change, and the title and the twelve years of a decade when
 * they change, so a frame allocates nothing beyond the rings it outlines. Hover is tracked from pointer moves against
 * the cached rectangles. Today is read from the platform's wall clock when the calendar is made, and again while it
 * paints once a minute has passed since the last reading, timed by {@code Platform.clock().millis()}.</p>
 *
 * <p>The cells share the room they are given: each is a seventh of the width less the padding and an eighth of the
 * height, rounded down to whole units, and the grid is centered across. The year and decade grids fill the area of
 * the weekday row and the six rows of days, four cells across and three down. The arrows are 4 by 7 units, centered in
 * the first and last cell of the header, and a marker is a 2 unit square centered under the day number.</p>
 *
 * <p>A picker shows a calendar in its popup and {@linkplain #route routes} the user's picks to itself, so the private
 * calendar never asks {@code PICK_DATE}.</p>
 */
public final class CalendarViewImpl extends BuiltInComponent<CalendarView> implements CalendarView {
    /**
     * The padding when the theme sets no {@value #STYLE_PADDING}: 4 on every side.
     */
    public static final Insets DEFAULT_PADDING = Insets.of(4f);
    /**
     * The cell padding when the theme sets no {@value #STYLE_CELL_PADDING}: 3 at the sides and 2 at the top and
     * bottom.
     */
    public static final Insets DEFAULT_CELL_PADDING = Insets.of(3f, 2f);
    /**
     * The side of the square under a marked day, in UI units.
     */
    public static final float MARKER_SIZE = 2f;
    /**
     * How often today is read from the wall clock, in milliseconds.
     */
    public static final long TODAY_CHECK_MILLIS = 60_000L;
    private static final Set<String> REQUIRED = Set.of(STYLE_DAY_SELECTED);
    private static final int NORMAL = 0;
    private static final int OUTSIDE = 1;
    private static final int DISABLED = 2;
    private static final int SELECTED = 3;
    private static final int NONE = 0;
    private static final int PREVIOUS = 1;
    private static final int NEXT = 2;
    private static final int TITLE = 3;
    private static final int CELL = 4;

    private final ComponentFactoryImpl ui;
    private LocalDate date;
    private DateRange range;
    private Predicate<? super LocalDate> filter;
    private Predicate<? super LocalDate> marks;
    private DayOfWeek firstDayOverride;
    private YearMonth shown;
    private Zoom zoom;
    private boolean outsideShown;
    private LocalDate cursor;
    private Consumer<LocalDate> route;

    // Today, and when it was last read
    private LocalDate today;
    private long todayCheckedAt;

    // Theme
    private Paint background;
    private Paint headerHovered;
    private Paint dayHovered;
    private Paint daySelected;
    private Paint dayToday;
    private Paint todayOutline;
    private Paint marker;
    private Paint arrow;
    private Paint focus;
    private TextStyle headerStyle;
    private TextStyle weekdayStyle;
    private final TextStyle[] dayStyles;
    private Insets padding;
    private Insets cellPadding;

    // Made during measure
    private DateFormats formats;
    private float lineHeight;
    private final TextLine[][] dayLines;
    private final TextLine[][] monthLines;
    private final TextLine[] weekdayLines;

    // Made when what they show changes
    private TextLine title;
    private YearMonth titleMonth;
    private Zoom titleZoom;
    private DateFormats titleFormats;
    private final TextLine[][] yearLines;
    private int yearsFrom;
    private final LocalDate[] cells;
    private YearMonth cellsMonth;
    private DayOfWeek cellsFirstDay;

    // Worked out when the bounds change
    private Rect laidOutFor;
    private Rect previousRect;
    private Rect nextRect;
    private Rect titleRect;
    private final Rect[] weekdayRects;
    private final Rect[] dayRects;
    private final Rect[] markerRects;
    private final Rect[] gridRects;

    // Hover
    private int hoverPart;
    private int hoverIndex;

    /**
     * Creates a calendar with no date picked, showing today's month.
     *
     * @param ui the factory that owns the calendar
     */
    @SuppressWarnings("this-escape")
    public CalendarViewImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.ui = ui;
        this.range = DateRange.ALL;
        this.filter = day -> true;
        this.marks = day -> false;
        this.zoom = Zoom.MONTH;
        this.outsideShown = true;
        this.todayCheckedAt = Long.MIN_VALUE;
        this.today = this.readToday();
        this.shown = YearMonth.from(this.today);
        this.background = Paint.NONE;
        this.headerHovered = Paint.NONE;
        this.dayHovered = Paint.NONE;
        this.daySelected = Paint.NONE;
        this.dayToday = Paint.NONE;
        this.todayOutline = Paint.NONE;
        this.marker = Paint.NONE;
        this.arrow = Paint.NONE;
        this.focus = Paint.NONE;
        this.headerStyle = TextStyle.DEFAULT;
        this.weekdayStyle = TextStyle.DEFAULT;
        this.dayStyles = new TextStyle[] {TextStyle.DEFAULT, TextStyle.DEFAULT, TextStyle.DEFAULT, TextStyle.DEFAULT};
        this.padding = DEFAULT_PADDING;
        this.cellPadding = DEFAULT_CELL_PADDING;
        this.dayLines = new TextLine[4][32];
        this.monthLines = new TextLine[4][12];
        this.weekdayLines = new TextLine[7];
        this.yearLines = new TextLine[4][12];
        this.yearsFrom = Integer.MIN_VALUE;
        this.cells = new LocalDate[42];
        this.weekdayRects = new Rect[7];
        this.dayRects = new Rect[42];
        this.markerRects = new Rect[42];
        this.gridRects = new Rect[12];
        this.title = TextLine.EMPTY;
    }

    /**
     * Sends the user's picks to the control that owns this calendar instead of asking {@code PICK_DATE}. Picking a
     * day that is already the date still calls it.
     *
     * @param pick takes the day the user picked
     */
    public void route(final Consumer<LocalDate> pick) {
        this.route = Objects.requireNonNull(pick, "pick");
    }

    // ------------------------------------------------------------------
    // HasDate
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalDate> date() {
        return Optional.ofNullable(this.date);
    }

    @Override
    public CalendarView setDate(final LocalDate date) {
        final LocalDate next = this.range.clamp(date);
        final LocalDate previous = this.date;
        if (Objects.equals(next, previous)) {
            return this;
        }
        this.date = next;
        this.report(DATE_PICKED, next, previous);
        if (next != null) {
            this.setShownMonth(YearMonth.from(next));
        }
        return this;
    }

    @Override
    public CalendarView clearDate() {
        return this.setDate(null);
    }

    @Override
    public Optional<LocalDate> minDate() {
        return Optional.ofNullable(this.range.min());
    }

    @Override
    public Optional<LocalDate> maxDate() {
        return Optional.ofNullable(this.range.max());
    }

    @Override
    public CalendarView setDateRange(final LocalDate min, final LocalDate max) {
        this.range = DateRange.of(min, max);
        this.setDate(this.date);
        return this.setShownMonth(this.shown);
    }

    @Override
    public CalendarView setDateFilter(final Predicate<? super LocalDate> allowed) {
        this.filter = Objects.requireNonNull(allowed, "allowed");
        return this;
    }

    @Override
    public Optional<DayOfWeek> firstDayOfWeek() {
        return Optional.ofNullable(this.firstDayOverride);
    }

    @Override
    public CalendarView setFirstDayOfWeek(final DayOfWeek day) {
        this.firstDayOverride = day;
        this.invalidateLayout();
        return this;
    }

    // ------------------------------------------------------------------
    // CalendarView
    // ------------------------------------------------------------------

    @Override
    public YearMonth shownMonth() {
        return this.shown;
    }

    @Override
    public CalendarView setShownMonth(final YearMonth month) {
        final YearMonth next = this.range.clamp(Objects.requireNonNull(month, "month"));
        final YearMonth previous = this.shown;
        if (!next.equals(previous)) {
            this.shown = next;
            if (this.cursor != null && !this.inView(this.cursor)) {
                this.cursor = this.cursorFor(next);
            }
            this.report(BROWSED, next, previous);
        }
        return this;
    }

    @Override
    public Zoom zoom() {
        return this.zoom;
    }

    @Override
    public CalendarView setZoom(final Zoom zoom) {
        this.zoom = Objects.requireNonNull(zoom, "zoom");
        return this;
    }

    @Override
    public boolean isOutsideDaysShown() {
        return this.outsideShown;
    }

    @Override
    public CalendarView setOutsideDaysShown(final boolean shown) {
        this.outsideShown = shown;
        return this;
    }

    @Override
    public CalendarView setMarkFilter(final Predicate<? super LocalDate> marked) {
        this.marks = Objects.requireNonNull(marked, "marked");
        return this;
    }

    /**
     * Returns the day the keyboard cursor marks.
     *
     * @return the cursor's day, or empty before the calendar first had focus
     */
    public Optional<LocalDate> cursorDate() {
        return Optional.ofNullable(this.cursor);
    }

    /**
     * Returns whether the user may pick a day: inside the range and allowed by the filter.
     *
     * @param day the day
     * @return true if the day can be picked
     */
    public boolean isAllowed(final LocalDate day) {
        return this.range.contains(day) && this.filter.test(day);
    }

    // ------------------------------------------------------------------
    // Rules
    // ------------------------------------------------------------------

    private LocalDate readToday() {
        return LocalDate.now(this.ui.platform().clock().wall());
    }

    /**
     * Reads today again once a minute has passed since the last reading.
     */
    private void refreshToday() {
        final long now = this.ui.platform().clock().millis();
        if (this.todayCheckedAt == Long.MIN_VALUE || now - this.todayCheckedAt >= TODAY_CHECK_MILLIS) {
            this.todayCheckedAt = now;
            this.today = this.readToday();
        }
    }


    private DayOfWeek firstDay() {
        if (this.firstDayOverride != null) {
            return this.firstDayOverride;
        }
        return (this.formats != null ? this.formats : DateFormats.current()).firstDayOfWeek();
    }

    private static int decadeStart(final int year) {
        return year - Math.floorMod(year, 10);
    }

    /**
     * Returns whether a day is in the month, year or decade in view.
     */
    private boolean inView(final LocalDate day) {
        return switch (this.zoom) {
            case MONTH -> YearMonth.from(day).equals(this.shown);
            case YEAR -> day.getYear() == this.shown.getYear();
            case DECADE -> decadeStart(day.getYear()) == decadeStart(this.shown.getYear());
        };
    }

    /**
     * Returns where the cursor starts in a month: the date, else today, else the first, kept inside the range.
     */
    private LocalDate cursorFor(final YearMonth month) {
        final LocalDate start;
        if (this.date != null && YearMonth.from(this.date).equals(month)) {
            start = this.date;
        } else if (YearMonth.from(this.today).equals(month)) {
            start = this.today;
        } else if (this.cursor != null) {
            start = month.atDay(Math.min(this.cursor.getDayOfMonth(), month.lengthOfMonth()));
        } else {
            start = month.atDay(1);
        }
        return this.range.clamp(start);
    }

    private boolean canBrowse(final int direction) {
        final YearMonth target = switch (this.zoom) {
            case MONTH -> this.shown.plusMonths(direction);
            case YEAR -> this.shown.plusYears(direction);
            case DECADE -> this.shown.plusYears(10L * direction);
        };
        return switch (this.zoom) {
            case MONTH -> this.range.contains(target);
            case YEAR -> this.range.containsYear(target.getYear());
            case DECADE -> this.range.containsDecade(decadeStart(target.getYear()));
        };
    }

    // ------------------------------------------------------------------
    // The user's actions
    // ------------------------------------------------------------------

    /**
     * Asks to show another month, kept inside the range. Does nothing if it is the month in view.
     */
    private void userBrowse(final YearMonth month) {
        final YearMonth target = this.range.clamp(month);
        if (!target.equals(this.shown)) {
            this.request(BROWSE, target, value -> {
                if (value != null) {
                    this.setShownMonth(value);
                }
            });
        }
    }

    private void browseBy(final int direction) {
        if (!this.canBrowse(direction)) {
            return;
        }
        this.userBrowse(switch (this.zoom) {
            case MONTH -> this.shown.plusMonths(direction);
            case YEAR -> this.shown.plusYears(direction);
            case DECADE -> this.shown.plusYears(10L * direction);
        });
    }

    /**
     * Picks a day for the user: through the route, or by asking {@code PICK_DATE} when it would change the date.
     */
    private void userPick(final LocalDate day) {
        if (!this.isAllowed(day)) {
            return;
        }
        if (this.route != null) {
            this.route.accept(day);
        } else if (!day.equals(this.date)) {
            this.request(PICK_DATE, day, this::setDate);
        }
    }

    private void zoomOut() {
        if (this.zoom != Zoom.DECADE) {
            this.zoom = this.zoom == Zoom.MONTH ? Zoom.YEAR : Zoom.DECADE;
            this.hoverPart = NONE;
        }
    }

    /**
     * Zooms in on a month of the year view or a year of the decade view, browsing there first.
     */
    private void zoomIn(final YearMonth target) {
        if (this.zoom == Zoom.MONTH) {
            return;
        }
        this.userBrowse(target);
        this.zoom = this.zoom == Zoom.DECADE ? Zoom.YEAR : Zoom.MONTH;
        this.hoverPart = NONE;
        if (this.cursor != null && !this.inView(this.cursor)) {
            this.cursor = this.cursorFor(this.shown);
        }
    }

    /**
     * Moves the cursor, browsing when it leaves the month, year or decade in view.
     */
    private void moveCursor(final LocalDate target) {
        final LocalDate next = this.range.clamp(target);
        if (next == null || next.equals(this.cursor)) {
            return;
        }
        if (!this.inView(next)) {
            this.userBrowse(this.zoom == Zoom.MONTH ? YearMonth.from(next) : this.shown.withYear(next.getYear()));
            if (!this.inView(next)) {
                return;
            }
        }
        this.cursor = next;
    }

    // ------------------------------------------------------------------
    // Theme and layout
    // ------------------------------------------------------------------

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.background = theme.paintOr(STYLE_BACKGROUND, Paint.NONE);
        final Paint rowHovered = theme.paintOr(ListView.STYLE_ROW_HOVERED, Paint.NONE);
        this.headerHovered = theme.paintOr(STYLE_HEADER_HOVERED, rowHovered);
        this.dayHovered = theme.paintOr(STYLE_DAY_HOVERED, rowHovered);
        this.daySelected = theme.paintOr(STYLE_DAY_SELECTED, Paint.NONE);
        this.dayToday = theme.paintOr(STYLE_DAY_TODAY, Paint.NONE);
        this.arrow = theme.paintOr(STYLE_ARROW, Paint.NONE);
        this.focus = theme.paintOr(STYLE_FOCUS, theme.paintOr(Button.STYLE_FOCUS, Paint.NONE));
        this.padding = theme.insetsOr(STYLE_PADDING, DEFAULT_PADDING);
        this.cellPadding = theme.insetsOr(STYLE_CELL_PADDING, DEFAULT_CELL_PADDING);
        final TextStyle day = theme.textStyleOr(STYLE_DAY_TEXT, TextStyle.DEFAULT);
        final TextStyle disabled = theme.textStyleOr(STYLE_DAY_TEXT_DISABLED,
                theme.textStyleOr(TextBox.STYLE_TEXT_DISABLED, TextStyle.DEFAULT));
        final TextStyle outside = theme.textStyleOr(STYLE_DAY_TEXT_OUTSIDE, disabled);
        this.dayStyles[NORMAL] = day;
        this.dayStyles[OUTSIDE] = outside;
        this.dayStyles[DISABLED] = disabled;
        this.dayStyles[SELECTED] = theme.textStyleOr(STYLE_DAY_TEXT_SELECTED, day);
        this.headerStyle = theme.textStyleOr(STYLE_HEADER_TEXT, TextStyle.DEFAULT);
        this.weekdayStyle = theme.textStyleOr(STYLE_WEEKDAY_TEXT, outside);
        this.todayOutline = Paint.solid(day.color());
        this.marker = theme.paintOr(STYLE_DAY_MARKER, Paint.solid(day.color()));
        this.titleMonth = null;
        this.yearsFrom = Integer.MIN_VALUE;
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        final TextMetrics metrics = context.text();
        this.formats = DateFormats.current();
        float height = 0f;
        for (final TextStyle style : this.dayStyles) {
            height = Math.max(height, metrics.lineHeight(style));
        }
        height = Math.max(height, Math.max(metrics.lineHeight(this.headerStyle), metrics.lineHeight(this.weekdayStyle)));
        this.lineHeight = height;

        float widestDay = 0f;
        for (int style = 0; style < 4; style++) {
            for (int day = 1; day <= 31; day++) {
                final TextLine line = TextLine.of(Text.of(Integer.toString(day)), this.dayStyles[style], metrics);
                this.dayLines[style][day] = line;
                widestDay = Math.max(widestDay, line.width());
            }
        }
        float widestMonth = 0f;
        for (int style = 0; style < 4; style++) {
            for (final Month month : Month.values()) {
                final TextLine line = TextLine.of(Text.of(this.formats.shortMonthName(month)), this.dayStyles[style],
                        metrics);
                this.monthLines[style][month.ordinal()] = line;
                widestMonth = Math.max(widestMonth, line.width());
            }
        }
        final DayOfWeek first = this.firstDay();
        float widestWeekday = 0f;
        for (int i = 0; i < 7; i++) {
            final TextLine line = TextLine.of(Text.of(this.formats.shortWeekdayName(first.plus(i))), this.weekdayStyle,
                    metrics);
            this.weekdayLines[i] = line;
            widestWeekday = Math.max(widestWeekday, line.width());
        }
        float widestTitle = 0f;
        for (final Month month : Month.values()) {
            widestTitle = Math.max(widestTitle, TextLine.of(this.titleText(Zoom.MONTH, YearMonth.of(2000, month)),
                    this.headerStyle, metrics).width());
        }
        final float widestYear = TextLine.of(Text.of("0000"), this.dayStyles[NORMAL], metrics).width();

        final Insets cell = this.cellPadding;
        final float cellHeight = this.lineHeight + cell.vertical();
        final float cellWidth = Math.max(cellHeight, Math.max(widestDay, widestWeekday) + cell.horizontal());
        final float gridCell = Math.max(widestMonth, widestYear) + cell.horizontal();
        final float width = Math.max(Math.max(7f * cellWidth, 4f * gridCell), 2f * cellWidth + widestTitle);
        this.titleMonth = null;
        this.yearsFrom = Integer.MIN_VALUE;
        this.cellsMonth = null;
        this.laidOutFor = null;
        return new Size((float) Math.ceil(width) + this.padding.horizontal(), 8f * cellHeight + this.padding.vertical());
    }

    private Text titleText(final Zoom zoom, final YearMonth month) {
        return switch (zoom) {
            case MONTH -> Text.translatable("strata.calendar.title", this.formats.monthName(month.getMonth()),
                    Integer.toString(month.getYear()));
            case YEAR -> Text.of(Integer.toString(month.getYear()));
            case DECADE -> {
                final int start = decadeStart(month.getYear());
                yield Text.translatable("strata.calendar.decade", Integer.toString(start), Integer.toString(start + 9));
            }
        };
    }

    /**
     * Works out every rectangle for the bounds. Called when the bounds differ from the last ones.
     */
    private void layOut(final Rect bounds) {
        final Rect inner = bounds.shrink(this.padding);
        final float cellWidth = (float) Math.floor(inner.width() / 7f);
        final float cellHeight = (float) Math.floor(inner.height() / 8f);
        final float x = inner.x() + (float) Math.floor((inner.width() - 7f * cellWidth) * 0.5f);
        final float y = inner.y();
        this.previousRect = new Rect(x, y, cellWidth, cellHeight);
        this.nextRect = new Rect(x + 6f * cellWidth, y, cellWidth, cellHeight);
        this.titleRect = new Rect(x + cellWidth, y, 5f * cellWidth, cellHeight);
        for (int i = 0; i < 7; i++) {
            this.weekdayRects[i] = new Rect(x + i * cellWidth, y + cellHeight, cellWidth, cellHeight);
        }
        for (int i = 0; i < 42; i++) {
            final Rect cell = new Rect(x + (i % 7) * cellWidth, y + (2 + i / 7) * cellHeight, cellWidth, cellHeight);
            this.dayRects[i] = cell;
            final float textY = cell.y() + (float) Math.floor((cell.height() - this.lineHeight) * 0.5f);
            this.markerRects[i] = new Rect(cell.x() + (float) Math.floor((cell.width() - MARKER_SIZE) * 0.5f),
                    Math.min(textY + this.lineHeight, cell.bottom() - MARKER_SIZE), MARKER_SIZE, MARKER_SIZE);
        }
        final float gridWidth = (float) Math.floor(7f * cellWidth / 4f);
        final float gridHeight = (float) Math.floor(7f * cellHeight / 3f);
        final float gridX = x + (float) Math.floor((7f * cellWidth - 4f * gridWidth) * 0.5f);
        for (int i = 0; i < 12; i++) {
            this.gridRects[i] = new Rect(gridX + (i % 4) * gridWidth, y + cellHeight + (i / 4) * gridHeight, gridWidth,
                    gridHeight);
        }
        this.laidOutFor = bounds;
    }

    /**
     * Brings the cached rectangles, dates, title and years up to the bounds and what the calendar shows.
     */
    private void sync(final Rect bounds) {
        if (!bounds.equals(this.laidOutFor)) {
            this.layOut(bounds);
        }
        final DayOfWeek first = this.firstDay();
        if (!this.shown.equals(this.cellsMonth) || first != this.cellsFirstDay) {
            final LocalDate start = this.shown.atDay(1);
            final int offset = Math.floorMod(start.getDayOfWeek().getValue() - first.getValue(), 7);
            LocalDate day = start.minusDays(offset);
            for (int i = 0; i < 42; i++) {
                this.cells[i] = day;
                day = day.plusDays(1);
            }
            this.cellsMonth = this.shown;
            this.cellsFirstDay = first;
        }
        if (this.formats != null && (!this.shown.equals(this.titleMonth) || this.zoom != this.titleZoom
                || this.formats != this.titleFormats)) {
            this.title = TextLine.of(this.titleText(this.zoom, this.shown), this.headerStyle, this.environment().text());
            this.titleMonth = this.shown;
            this.titleZoom = this.zoom;
            this.titleFormats = this.formats;
        }
        final int from = decadeStart(this.shown.getYear()) - 1;
        if (this.zoom == Zoom.DECADE && from != this.yearsFrom) {
            final TextMetrics metrics = this.environment().text();
            for (int style = 0; style < 4; style++) {
                for (int i = 0; i < 12; i++) {
                    this.yearLines[style][i] = TextLine.of(Text.of(Integer.toString(from + i)), this.dayStyles[style],
                            metrics);
                }
            }
            this.yearsFrom = from;
        }
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (this.formats == null) {
            return;
        }
        this.refreshToday();
        this.sync(bounds);
        this.background.paint(painter, bounds);
        final boolean enabled = this.isEnabled();

        // Header
        if (enabled && this.hoverPart == TITLE && this.zoom != Zoom.DECADE) {
            this.headerHovered.paint(painter, this.titleRect);
        }
        this.drawCentered(painter, this.title, this.titleRect);
        this.drawArrow(painter, this.previousRect, -1, enabled && this.canBrowse(-1), this.hoverPart == PREVIOUS);
        this.drawArrow(painter, this.nextRect, 1, enabled && this.canBrowse(1), this.hoverPart == NEXT);

        switch (this.zoom) {
            case MONTH -> this.paintMonth(painter, enabled);
            case YEAR -> this.paintYear(painter, enabled);
            case DECADE -> this.paintDecade(painter, enabled);
        }
    }

    private void paintMonth(final Painter painter, final boolean enabled) {
        for (int i = 0; i < 7; i++) {
            this.drawCentered(painter, this.weekdayLines[i], this.weekdayRects[i]);
        }
        final boolean focused = this.isFocused() && enabled;
        for (int i = 0; i < 42; i++) {
            final LocalDate day = this.cells[i];
            final boolean outside = day.getMonthValue() != this.shown.getMonthValue();
            if (outside && !this.outsideShown) {
                continue;
            }
            final Rect cell = this.dayRects[i];
            final boolean allowed = enabled && this.isAllowed(day);
            final boolean selected = day.equals(this.date);
            if (day.equals(this.today)) {
                if (this.dayToday == Paint.NONE) {
                    Rings.paint(painter, this.todayOutline, cell);
                } else {
                    this.dayToday.paint(painter, cell);
                }
            }
            if (selected) {
                this.daySelected.paint(painter, cell);
            }
            if (allowed && this.hoverPart == CELL && this.hoverIndex == i) {
                this.dayHovered.paint(painter, cell);
            }
            final int style = !allowed ? DISABLED : selected ? SELECTED : outside ? OUTSIDE : NORMAL;
            this.drawCentered(painter, this.dayLines[style][day.getDayOfMonth()], cell);
            if (this.marks.test(day)) {
                this.marker.paint(painter, this.markerRects[i]);
            }
            if (focused && day.equals(this.cursor)) {
                Rings.paint(painter, this.focus, cell);
            }
        }
    }

    private void paintYear(final Painter painter, final boolean enabled) {
        final boolean focused = this.isFocused() && enabled;
        final int year = this.shown.getYear();
        for (int i = 0; i < 12; i++) {
            final YearMonth month = YearMonth.of(year, i + 1);
            final Rect cell = this.gridRects[i];
            final boolean allowed = enabled && this.range.contains(month);
            final boolean selected = this.date != null && this.date.getYear() == year && this.date.getMonthValue() == i + 1;
            this.paintGridCell(painter, cell, i, allowed, selected,
                    this.today.getYear() == year && this.today.getMonthValue() == i + 1);
            final int style = !allowed ? DISABLED : selected ? SELECTED : NORMAL;
            this.drawCentered(painter, this.monthLines[style][i], cell);
            if (focused && this.cursor != null && this.cursor.getYear() == year && this.cursor.getMonthValue() == i + 1) {
                Rings.paint(painter, this.focus, cell);
            }
        }
    }

    private void paintDecade(final Painter painter, final boolean enabled) {
        final boolean focused = this.isFocused() && enabled;
        for (int i = 0; i < 12; i++) {
            final int year = this.yearsFrom + i;
            final Rect cell = this.gridRects[i];
            final boolean allowed = enabled && this.range.containsYear(year);
            final boolean selected = this.date != null && this.date.getYear() == year;
            this.paintGridCell(painter, cell, i, allowed, selected, this.today.getYear() == year);
            final boolean outside = i == 0 || i == 11;
            final int style = !allowed ? DISABLED : selected ? SELECTED : outside ? OUTSIDE : NORMAL;
            this.drawCentered(painter, this.yearLines[style][i], cell);
            if (focused && this.cursor != null && this.cursor.getYear() == year) {
                Rings.paint(painter, this.focus, cell);
            }
        }
    }

    private void paintGridCell(final Painter painter, final Rect cell, final int index, final boolean allowed,
                               final boolean selected, final boolean current) {
        if (current) {
            if (this.dayToday == Paint.NONE) {
                Rings.paint(painter, this.todayOutline, cell);
            } else {
                this.dayToday.paint(painter, cell);
            }
        }
        if (selected) {
            this.daySelected.paint(painter, cell);
        }
        if (allowed && this.hoverPart == CELL && this.hoverIndex == index) {
            this.dayHovered.paint(painter, cell);
        }
    }

    private void drawCentered(final Painter painter, final TextLine line, final Rect cell) {
        if (line == null) {
            return;
        }
        line.draw(painter, cell.x() + (float) Math.floor((cell.width() - line.width()) * 0.5f),
                cell.y() + (float) Math.floor((cell.height() - this.lineHeight) * 0.5f));
    }

    private void drawArrow(final Painter painter, final Rect cell, final int direction, final boolean usable,
                           final boolean hovered) {
        if (usable && hovered) {
            this.headerHovered.paint(painter, cell);
        }
        final float x = cell.x() + (float) Math.floor((cell.width() - Rings.ARROW_SHORT) * 0.5f);
        final float y = cell.y() + (float) Math.floor((cell.height() - Rings.ARROW_LONG) * 0.5f);
        if (this.arrow != Paint.NONE && usable) {
            if (direction < 0) {
                Rings.left(painter, this.arrow, x, y);
            } else {
                Rings.right(painter, this.arrow, x, y);
            }
            return;
        }
        final int color = usable ? this.headerStyle.color() : this.dayStyles[DISABLED].color();
        if (direction < 0) {
            Rings.left(painter, color, x, y);
        } else {
            Rings.right(painter, color, x, y);
        }
    }

    // ------------------------------------------------------------------
    // Input
    // ------------------------------------------------------------------

    @Override
    protected void onPointerMove(final PointerEvent event) {
        this.hoverAt(event.x(), event.y());
    }

    @Override
    protected void onPointerEnter(final PointerEvent event) {
        this.hoverAt(event.x(), event.y());
    }

    @Override
    protected void onPointerExit(final PointerEvent event) {
        this.hoverPart = NONE;
    }

    private void hoverAt(final float x, final float y) {
        if (this.formats == null) {
            return;
        }
        this.sync(this.bounds());
        this.hoverPart = NONE;
        this.hoverIndex = -1;
        if (this.previousRect.contains(x, y)) {
            this.hoverPart = PREVIOUS;
        } else if (this.nextRect.contains(x, y)) {
            this.hoverPart = NEXT;
        } else if (this.titleRect.contains(x, y)) {
            this.hoverPart = TITLE;
        } else {
            final Rect[] rects = this.zoom == Zoom.MONTH ? this.dayRects : this.gridRects;
            for (int i = 0; i < rects.length; i++) {
                if (rects[i].contains(x, y)) {
                    this.hoverPart = CELL;
                    this.hoverIndex = i;
                    return;
                }
            }
        }
    }

    @Override
    protected void onClick(final PointerButtonEvent event) {
        if (event.button() != MouseButton.LEFT || !this.isEnabled() || this.formats == null) {
            return;
        }
        this.hoverAt(event.x(), event.y());
        switch (this.hoverPart) {
            case PREVIOUS -> this.browseBy(-1);
            case NEXT -> this.browseBy(1);
            case TITLE -> this.zoomOut();
            case CELL -> this.clickCell(this.hoverIndex);
            default -> {
                return;
            }
        }
        event.consume();
    }

    private void clickCell(final int index) {
        switch (this.zoom) {
            case MONTH -> {
                final LocalDate day = this.cells[index];
                final boolean outside = day.getMonthValue() != this.shown.getMonthValue();
                if (outside && !this.outsideShown || !this.isAllowed(day)) {
                    return;
                }
                if (outside) {
                    this.userBrowse(YearMonth.from(day));
                }
                this.cursor = day;
                this.userPick(day);
            }
            case YEAR -> {
                final YearMonth month = YearMonth.of(this.shown.getYear(), index + 1);
                if (this.range.contains(month)) {
                    this.zoomIn(month);
                }
            }
            case DECADE -> {
                final int year = this.yearsFrom + index;
                if (this.range.containsYear(year)) {
                    this.zoomIn(this.range.clamp(this.shown.withYear(year)));
                }
            }
        }
    }

    @Override
    protected void onFocusGained(final FocusEvent event) {
        if (this.cursor == null || !this.inView(this.cursor)) {
            this.cursor = this.cursorFor(this.shown);
        }
    }

    @Override
    protected void onKey(final KeyEvent event) {
        if (!event.action().isDown() || !this.isEnabled()) {
            return;
        }
        if (this.cursor == null) {
            this.cursor = this.cursorFor(this.shown);
        }
        final Key key = event.key();
        final boolean control = event.modifiers().control();
        final boolean shift = event.modifiers().shift();
        if (control && key == Key.UP) {
            this.zoomOut();
            event.consume();
            return;
        }
        if (control && key == Key.DOWN || this.zoom != Zoom.MONTH && (key.isEnter() || key == Key.SPACE)) {
            this.zoomIn(this.zoom == Zoom.YEAR ? YearMonth.from(this.cursor) : this.shown.withYear(this.cursor.getYear()));
            event.consume();
            return;
        }
        if (control || event.modifiers().alt()) {
            return;
        }
        final LocalDate at = this.cursor;
        final LocalDate target = switch (this.zoom) {
            case MONTH -> switch (key) {
                case LEFT -> at.minusDays(1);
                case RIGHT -> at.plusDays(1);
                case UP -> at.minusWeeks(1);
                case DOWN -> at.plusWeeks(1);
                case PAGE_UP -> shift ? at.minusYears(1) : at.minusMonths(1);
                case PAGE_DOWN -> shift ? at.plusYears(1) : at.plusMonths(1);
                case HOME -> at.withDayOfMonth(1);
                case END -> at.withDayOfMonth(at.lengthOfMonth());
                default -> null;
            };
            case YEAR -> switch (key) {
                case LEFT -> at.minusMonths(1);
                case RIGHT -> at.plusMonths(1);
                case UP -> at.minusMonths(4);
                case DOWN -> at.plusMonths(4);
                case PAGE_UP -> at.minusYears(1);
                case PAGE_DOWN -> at.plusYears(1);
                default -> null;
            };
            case DECADE -> switch (key) {
                case LEFT -> at.minusYears(1);
                case RIGHT -> at.plusYears(1);
                case UP -> at.minusYears(4);
                case DOWN -> at.plusYears(4);
                case PAGE_UP -> at.minusYears(10);
                case PAGE_DOWN -> at.plusYears(10);
                default -> null;
            };
        };
        if (target != null) {
            this.moveCursor(target);
            event.consume();
            return;
        }
        if (this.zoom == Zoom.MONTH && event.action() == KeyAction.PRESS
                && (key.isEnter() || key == Key.SPACE)) {
            this.userPick(this.cursor);
            event.consume();
        }
    }

    @Override
    protected Object snapshotState() {
        return new CalendarView.State(this.date, this.shown, this.zoom);
    }
}
