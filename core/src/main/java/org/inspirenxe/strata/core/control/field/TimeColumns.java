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

import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.layout.StackPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.core.control.collection.ListViewImpl;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.DateFormats;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * The columns of a time popup: private lists of hours, minutes, seconds and the halves of the day in a row. Each list
 * is routed, not focusable, and fires no events; a press on a row hands the column's part and the row's number to the
 * owner at once. Rows are a line plus 4 units tall, and at most {@value TimePicker#VISIBLE_ROWS} show.
 */
final class TimeColumns {
    private final ComponentFactoryImpl ui;
    private final Pick pick;
    private final StackPanel row;
    private final List<Column> columns;
    private String builtFor;

    /**
     * Told the part and number of a row the user pressed.
     */
    @FunctionalInterface
    interface Pick {
        void pick(SegmentedField.Kind kind, int value);
    }

    private record Column(SegmentedField.Kind kind, ListViewImpl<Integer> list, List<Integer> values) {
    }

    TimeColumns(final ComponentFactoryImpl ui, final Pick pick) {
        this.ui = ui;
        this.pick = Objects.requireNonNull(pick, "pick");
        this.row = ui.stackPanel(Orientation.HORIZONTAL);
        this.columns = new ArrayList<>();
    }

    /**
     * Returns the row the columns sit in.
     */
    StackPanel panel() {
        return this.row;
    }

    /**
     * Makes the columns again when the clock, the seconds, the minute step, the language or the style changed.
     */
    void build(final boolean twelveHour, final boolean seconds, final int minuteStep, final DateFormats formats,
               final TextStyle style, final float lineHeight) {
        final String key = twelveHour + "/" + seconds + "/" + minuteStep + "/" + formats.am() + "/" + formats.pm() + "/"
                + style + "/" + lineHeight;
        if (key.equals(this.builtFor)) {
            return;
        }
        this.builtFor = key;
        this.row.clear();
        this.columns.clear();
        final List<Integer> hours = new ArrayList<>();
        if (twelveHour) {
            hours.add(12);
            for (int hour = 1; hour <= 11; hour++) {
                hours.add(hour);
            }
        } else {
            for (int hour = 0; hour <= 23; hour++) {
                hours.add(hour);
            }
        }
        this.add(SegmentedField.Kind.HOUR, hours, style, lineHeight, null);
        final List<Integer> minutes = new ArrayList<>();
        for (int minute = 0; minute < 60; minute += minuteStep) {
            minutes.add(minute);
        }
        this.add(SegmentedField.Kind.MINUTE, minutes, style, lineHeight, null);
        if (seconds) {
            final List<Integer> all = new ArrayList<>();
            for (int second = 0; second < 60; second++) {
                all.add(second);
            }
            this.add(SegmentedField.Kind.SECOND, all, style, lineHeight, null);
        }
        if (twelveHour) {
            this.add(SegmentedField.Kind.HALF, List.of(0, 1), style, lineHeight, formats);
        }
    }

    private void add(final SegmentedField.Kind kind, final List<Integer> values, final TextStyle style,
                     final float lineHeight, final DateFormats halves) {
        final float rowHeight = lineHeight + 4f;
        final ListViewImpl<Integer> list = new ListViewImpl<>(this.ui, value -> {
            final String text = halves == null ? String.format(Locale.ROOT, "%02d", value)
                    : value == 0 ? halves.am() : halves.pm();
            final Label label = this.ui.cellPadded(this.ui.label(Text.of(text)).setStyle(style));
            label.setAnchor(Anchor.MIDDLE_LEFT);
            return label;
        });
        list.route(index -> {
            list.applySelect(index);
            if (index >= 0) {
                this.pick.pick(kind, values.get(index));
            }
        }, index -> { }, (index, previous) -> { });
        list.setFocusable(false);
        list.setTabbable(false);
        list.setRowHeight(rowHeight);
        list.setItems(values);
        list.setHeight(rowHeight * Math.min(TimePicker.VISIBLE_ROWS, values.size()));
        this.columns.add(new Column(kind, list, values));
        this.row.add(list);
    }

    /**
     * Selects the rows of a time, or none for null, and brings each into the middle of its column.
     */
    void show(final LocalTime time, final boolean twelveHour) {
        for (final Column column : this.columns) {
            int index = -1;
            if (time != null) {
                final int value = switch (column.kind()) {
                    case HOUR -> twelveHour ? (time.getHour() % 12 == 0 ? 12 : time.getHour() % 12) : time.getHour();
                    case MINUTE -> time.getMinute();
                    case SECOND -> time.getSecond();
                    default -> time.getHour() >= 12 ? 1 : 0;
                };
                index = column.values().indexOf(value);
            }
            column.list().select(index);
            if (index >= 0) {
                column.list().scrollToIndex(Math.min(column.values().size() - 1, index + TimePicker.VISIBLE_ROWS / 2));
            }
        }
    }
}
