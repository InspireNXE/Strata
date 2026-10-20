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

import org.inspirenxe.strata.core.text.DateFormats;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.Year;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The parts of a date and time field and the draft the user types into them, shared by the date and time pickers.
 * It knows nothing of components: the picker hands it keys and characters and draws the texts it returns.
 *
 * <p>Each part holds a number, or -1 while empty. Hours are held as shown: 1 to 12 with the half of the day (0 for
 * the first half, 1 for the second) on a 12 hour clock, 0 to 23 otherwise. Digits typed into a part build its number
 * until no further digit could fit, then focus moves on. A number that would pass the part's largest starts again
 * from the digit. The draft is dirty from the first edit until {@link #load} replaces it.</p>
 */
final class SegmentedField {
    private final List<Kind> kinds;
    private final List<String> literals;
    private final int[] values;
    private int typed;
    private int yearDigits;
    private boolean dirty;
    private int focus;
    private boolean twelveHour;
    private int minuteStep;
    private DateFormats formats;
    private long version;

    SegmentedField() {
        this.kinds = new ArrayList<>();
        this.literals = new ArrayList<>();
        this.values = new int[Kind.values().length];
        Arrays.fill(this.values, -1);
        this.yearDigits = 4;
        this.focus = -1;
        this.minuteStep = 1;
    }

    /**
     * A part of the field.
     */
    enum Kind {
        DAY(2), MONTH(2), YEAR(4), HOUR(2), MINUTE(2), SECOND(2), HALF(0);

        final int digits;

        Kind(final int digits) {
            this.digits = digits;
        }
    }

    // ------------------------------------------------------------------
    // Layout
    // ------------------------------------------------------------------

    /**
     * Lays the parts out for a language, keeping the draft's numbers.
     *
     * @param formats the language's formats
     * @param date whether the field has date parts
     * @param time whether the field has time parts
     * @param twelveHour whether hours count to 12
     * @param seconds whether seconds show
     */
    void layout(final DateFormats formats, final boolean date, final boolean time, final boolean twelveHour,
                final boolean seconds) {
        final Kind focused = this.focusedKind();
        this.formats = formats;
        this.twelveHour = twelveHour;
        this.kinds.clear();
        this.literals.clear();
        if (date) {
            for (final DateFormats.Part part : formats.order()) {
                this.add(switch (part) {
                    case DAY -> Kind.DAY;
                    case MONTH -> Kind.MONTH;
                    case YEAR -> Kind.YEAR;
                }, formats.separator());
            }
        }
        if (time) {
            if (date) {
                this.literals.set(this.literals.size() - 1, " ");
            }
            this.add(Kind.HOUR, formats.timeSeparator());
            this.add(Kind.MINUTE, formats.timeSeparator());
            if (seconds) {
                this.add(Kind.SECOND, formats.timeSeparator());
            }
            if (twelveHour) {
                this.literals.set(this.literals.size() - 1, " ");
                this.add(Kind.HALF, "");
            }
        }
        this.literals.set(this.literals.size() - 1, "");
        this.focus = focused == null ? (this.focus < 0 ? -1 : 0) : Math.max(0, this.kinds.indexOf(focused));
        this.typed = 0;
        this.version++;
    }

    private void add(final Kind kind, final String literal) {
        this.kinds.add(kind);
        this.literals.add(literal);
    }

    /**
     * Returns the language the field was laid out for.
     *
     * @return the formats, or null before the first layout
     */
    DateFormats formats() {
        return this.formats;
    }

    int count() {
        return this.kinds.size();
    }

    Kind kind(final int index) {
        return this.kinds.get(index);
    }

    /**
     * Returns the text after a part.
     *
     * @return the separator, or an empty string after the last part
     */
    String literal(final int index) {
        return this.literals.get(index);
    }

    int indexOf(final Kind kind) {
        return this.kinds.indexOf(kind);
    }

    /**
     * Returns a number that changes whenever anything the field shows changes.
     */
    long version() {
        return this.version;
    }

    void setMinuteStep(final int minuteStep) {
        this.minuteStep = minuteStep;
    }

    // ------------------------------------------------------------------
    // The draft
    // ------------------------------------------------------------------

    /**
     * Replaces the draft with a value and marks it clean. Either part may be null for empty.
     */
    void load(final LocalDate date, final LocalTime time) {
        Arrays.fill(this.values, -1);
        if (date != null) {
            this.values[Kind.DAY.ordinal()] = date.getDayOfMonth();
            this.values[Kind.MONTH.ordinal()] = date.getMonthValue();
            this.values[Kind.YEAR.ordinal()] = date.getYear();
        }
        if (time != null) {
            this.setTimeParts(time);
        }
        this.typed = 0;
        this.yearDigits = 4;
        this.dirty = false;
        this.version++;
    }

    /**
     * Fills the empty parts from a value, marking the draft dirty if any was empty.
     */
    void fill(final LocalDate date, final LocalTime time) {
        boolean changed = false;
        if (date != null) {
            changed |= this.fillPart(Kind.DAY, date.getDayOfMonth());
            changed |= this.fillPart(Kind.MONTH, date.getMonthValue());
            if (this.values[Kind.YEAR.ordinal()] < 0) {
                this.yearDigits = 4;
            }
            changed |= this.fillPart(Kind.YEAR, date.getYear());
        }
        if (time != null) {
            changed |= this.fillPart(Kind.HOUR, this.shownHour(time.getHour()));
            changed |= this.fillPart(Kind.MINUTE, time.getMinute());
            changed |= this.fillPart(Kind.SECOND, time.getSecond());
            changed |= this.fillPart(Kind.HALF, time.getHour() >= 12 ? 1 : 0);
        }
        if (changed) {
            this.dirty = true;
            this.version++;
        }
    }

    private boolean fillPart(final Kind kind, final int value) {
        if (this.values[kind.ordinal()] >= 0) {
            return false;
        }
        this.values[kind.ordinal()] = value;
        return true;
    }

    private void setTimeParts(final LocalTime time) {
        this.values[Kind.HOUR.ordinal()] = this.shownHour(time.getHour());
        this.values[Kind.MINUTE.ordinal()] = time.getMinute();
        this.values[Kind.SECOND.ordinal()] = time.getSecond();
        this.values[Kind.HALF.ordinal()] = time.getHour() >= 12 ? 1 : 0;
    }

    private int shownHour(final int hour) {
        if (!this.twelveHour) {
            return hour;
        }
        final int twelve = hour % 12;
        return twelve == 0 ? 12 : twelve;
    }

    /**
     * Sets one part of the draft as a popup does, marking it dirty.
     */
    void setPart(final Kind kind, final int value) {
        this.values[kind.ordinal()] = value;
        if (kind == Kind.YEAR) {
            this.yearDigits = 4;
        }
        this.typed = 0;
        this.dirty = true;
        this.version++;
    }

    int part(final Kind kind) {
        return this.values[kind.ordinal()];
    }

    boolean isDirty() {
        return this.dirty;
    }

    /**
     * Returns whether every part the field shows holds a number.
     */
    boolean isComplete() {
        for (final Kind kind : this.kinds) {
            if (this.values[kind.ordinal()] < 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the date the draft makes, cutting the day to the month's length and widening a year typed with one or
     * two digits to the hundred years around {@code today}.
     *
     * @return the date, or null when a date part is empty or out of its range
     */
    LocalDate date(final LocalDate today) {
        final int day = this.values[Kind.DAY.ordinal()];
        final int month = this.values[Kind.MONTH.ordinal()];
        int year = this.values[Kind.YEAR.ordinal()];
        if (day < 1 || day > 31 || month < 1 || month > 12 || year < 0) {
            return null;
        }
        if (this.yearDigits <= 2 && year < 100) {
            final int start = today.getYear() - 50;
            year = start + Math.floorMod(year - start, 100);
        }
        if (year < 1 || year > Year.MAX_VALUE) {
            return null;
        }
        final YearMonth yearMonth = YearMonth.of(year, month);
        return yearMonth.atDay(Math.min(day, yearMonth.lengthOfMonth()));
    }

    /**
     * Returns the time the draft makes, with seconds only when the field shows them.
     *
     * @return the time, or null when a time part is empty or out of its range
     */
    LocalTime time() {
        int hour = this.values[Kind.HOUR.ordinal()];
        final int minute = this.values[Kind.MINUTE.ordinal()];
        final boolean seconds = this.kinds.contains(Kind.SECOND);
        final int second = seconds ? this.values[Kind.SECOND.ordinal()] : 0;
        if (minute < 0 || minute > 59 || second < 0 || second > 59) {
            return null;
        }
        if (this.twelveHour) {
            final int half = this.values[Kind.HALF.ordinal()];
            if (hour < 1 || hour > 12 || half < 0) {
                return null;
            }
            hour = hour % 12 + (half == 1 ? 12 : 0);
        } else if (hour < 0 || hour > 23) {
            return null;
        }
        return LocalTime.of(hour, minute, second);
    }

    // ------------------------------------------------------------------
    // Focus and editing
    // ------------------------------------------------------------------

    int focus() {
        return this.focus;
    }

    Kind focusedKind() {
        return this.focus >= 0 && this.focus < this.kinds.size() ? this.kinds.get(this.focus) : null;
    }

    /**
     * Focuses a part, or none for -1, ending any typing.
     */
    void setFocus(final int index) {
        final int next = index < 0 || this.kinds.isEmpty() ? -1 : Math.min(index, this.kinds.size() - 1);
        if (next != this.focus || this.typed != 0) {
            this.finishTyping();
            this.focus = next;
            this.version++;
        }
    }

    /**
     * Moves focus by some parts.
     *
     * @return true if focus moved
     */
    boolean moveFocus(final int by) {
        if (this.focus < 0) {
            return false;
        }
        final int next = this.focus + by;
        if (next < 0 || next >= this.kinds.size()) {
            return false;
        }
        this.setFocus(next);
        return true;
    }

    private void finishTyping() {
        if (this.typed > 0 && this.focusedKind() == Kind.YEAR) {
            this.yearDigits = this.typed;
        }
        this.typed = 0;
    }

    /**
     * Types a digit into the focused part.
     *
     * @return true if the field took it
     */
    boolean typeDigit(final int digit) {
        final Kind kind = this.focusedKind();
        if (kind == null || kind == Kind.HALF) {
            return false;
        }
        final int max = this.max(kind);
        int value = this.typed == 0 || this.values[kind.ordinal()] < 0 ? digit : this.values[kind.ordinal()] * 10 + digit;
        this.typed = this.typed == 0 ? 1 : this.typed + 1;
        if (value > max) {
            value = digit;
            this.typed = 1;
        }
        this.values[kind.ordinal()] = value;
        if (kind == Kind.YEAR) {
            this.yearDigits = this.typed;
        }
        this.dirty = true;
        this.version++;
        if (this.typed >= kind.digits || value * 10 > max) {
            this.typed = 0;
            this.moveFocus(1);
        }
        return true;
    }

    /**
     * Picks the half of the day whose name starts with a letter, or flips it when both names start with it.
     *
     * @return true if the field took the letter
     */
    boolean typeLetter(final int codePoint) {
        if (this.focusedKind() != Kind.HALF) {
            return false;
        }
        final int letter = Character.toLowerCase(codePoint);
        final boolean am = this.startsWith(this.formats.am(), letter);
        final boolean pm = this.startsWith(this.formats.pm(), letter);
        if (!am && !pm) {
            return false;
        }
        final int current = this.values[Kind.HALF.ordinal()];
        final int next = am && pm ? (current == 0 ? 1 : 0) : (am ? 0 : 1);
        this.values[Kind.HALF.ordinal()] = next;
        this.dirty = true;
        this.version++;
        return true;
    }

    private boolean startsWith(final String name, final int letter) {
        return !name.isEmpty() && Character.toLowerCase(name.codePointAt(0)) == letter;
    }

    /**
     * Moves on from the focused part when a separator is typed and the part holds a number.
     *
     * @return true if the code point is one of the field's separators
     */
    boolean typeSeparator(final int codePoint) {
        if (Character.isWhitespace(codePoint) && codePoint != ' ') {
            return false;
        }
        boolean separator = false;
        for (final String literal : this.literals) {
            if (literal.indexOf(codePoint) >= 0) {
                separator = true;
                break;
            }
        }
        if (!separator) {
            return false;
        }
        final Kind kind = this.focusedKind();
        if (kind != null && this.values[kind.ordinal()] >= 0) {
            this.finishTyping();
            this.moveFocus(1);
            this.version++;
        }
        return true;
    }

    /**
     * Empties the focused part.
     *
     * @return true if a part is focused
     */
    boolean clearFocused() {
        final Kind kind = this.focusedKind();
        if (kind == null) {
            return false;
        }
        this.values[kind.ordinal()] = -1;
        this.typed = 0;
        if (kind == Kind.YEAR) {
            this.yearDigits = 4;
        }
        this.dirty = true;
        this.version++;
        return true;
    }

    /**
     * Steps the focused part up or down, wrapping within it. An empty part starts from {@code now}'s.
     *
     * @return true if a part is focused
     */
    boolean step(final int direction, final LocalDateTime now) {
        final Kind kind = this.focusedKind();
        if (kind == null) {
            return false;
        }
        final int current = this.values[kind.ordinal()];
        final int next;
        if (current < 0) {
            next = switch (kind) {
                case DAY -> now.getDayOfMonth();
                case MONTH -> now.getMonthValue();
                case YEAR -> now.getYear();
                case HOUR -> this.shownHour(now.getHour());
                case MINUTE -> now.getMinute() - now.getMinute() % this.minuteStep;
                case SECOND -> now.getSecond();
                case HALF -> now.getHour() >= 12 ? 1 : 0;
            };
        } else {
            next = switch (kind) {
                case DAY -> SegmentedField.wrap(current + direction, 1, this.daysInMonth());
                case MONTH -> SegmentedField.wrap(current + direction, 1, 12);
                case YEAR -> Math.max(1, Math.min(Year.MAX_VALUE, current + direction));
                case HOUR -> this.twelveHour ? SegmentedField.wrap(current + direction, 1, 12)
                        : SegmentedField.wrap(current + direction, 0, 23);
                case MINUTE -> this.stepMinute(current, direction);
                case SECOND -> SegmentedField.wrap(current + direction, 0, 59);
                case HALF -> current == 0 ? 1 : 0;
            };
        }
        this.values[kind.ordinal()] = next;
        this.typed = 0;
        if (kind == Kind.YEAR) {
            this.yearDigits = 4;
        }
        this.dirty = true;
        this.version++;
        return true;
    }

    private int stepMinute(final int current, final int direction) {
        final int step = this.minuteStep;
        if (direction > 0) {
            return (current / step + 1) * step % 60;
        }
        final int down = current % step != 0 ? current - current % step : current - step;
        return down < 0 ? 60 - step : down;
    }

    private int daysInMonth() {
        final int month = this.values[Kind.MONTH.ordinal()];
        if (month < 1 || month > 12) {
            return 31;
        }
        final int year = this.values[Kind.YEAR.ordinal()];
        return year >= 1 && this.yearDigits > 2 ? YearMonth.of(year, month).lengthOfMonth() : Month.of(month).maxLength();
    }

    private static int wrap(final int value, final int min, final int max) {
        if (value > max) {
            return min;
        }
        return value < min ? max : value;
    }

    private int max(final Kind kind) {
        return switch (kind) {
            case DAY -> 31;
            case MONTH -> 12;
            case YEAR -> Year.MAX_VALUE;
            case HOUR -> this.twelveHour ? 12 : 23;
            case MINUTE, SECOND -> 59;
            case HALF -> 1;
        };
    }

    // ------------------------------------------------------------------
    // Text
    // ------------------------------------------------------------------

    /**
     * Returns what a part shows: the number padded to its digits, the digits typed so far while typing, dashes while
     * empty, or the name of the half of the day.
     */
    String text(final int index) {
        final Kind kind = this.kinds.get(index);
        final int value = this.values[kind.ordinal()];
        if (kind == Kind.HALF) {
            if (value < 0) {
                return "--";
            }
            return value == 0 ? this.formats.am() : this.formats.pm();
        }
        if (value < 0) {
            return "-".repeat(kind.digits);
        }
        if (index == this.focus && this.typed > 0) {
            return Integer.toString(value);
        }
        return String.format(Locale.ROOT, "%0" + kind.digits + "d", value);
    }
}
