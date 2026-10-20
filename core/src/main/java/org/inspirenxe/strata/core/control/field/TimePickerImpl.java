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

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.collection.ListView;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.field.TimePicker;
import org.inspirenxe.strata.api.layout.ScrollPanel;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.DateFormats;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The core {@link TimePicker}: a field of hour, minute, second and half of the day parts over a
 * {@link SegmentedField}, with {@link TimeColumns} in its popup.
 *
 * <p>The columns are made the first time the popup opens, and again when the clock, the seconds or the minute step
 * changed since. Focus stays in the field while the popup is open, and a press on a row takes it back. A row picked
 * from an empty value starts the other parts at zero, which is 12 in the first half of the day on a 12 hour
 * clock.</p>
 */
public final class TimePickerImpl extends PickerBase<TimePicker, LocalTime> implements TimePicker {
    private static final Set<String> EXTRA = Set.of(ListView.STYLE_ROW_SELECTED, ScrollPanel.STYLE_THUMB);

    private LocalTime time;
    private ClockFormat clockFormat;
    private boolean seconds;
    private int minuteStep;
    private TimeColumns columns;
    private TextStyle rowStyle;

    /**
     * Creates an empty picker.
     *
     * @param ui the factory that owns the picker
     */
    public TimePickerImpl(final ComponentFactoryImpl ui) {
        super(ui, Text.translatable("strata.time.placeholder"));
        this.minuteStep = 1;
        this.rowStyle = TextStyle.DEFAULT;
    }

    // ------------------------------------------------------------------
    // HasTime
    // ------------------------------------------------------------------

    @Override
    public Optional<LocalTime> time() {
        return Optional.ofNullable(this.time);
    }

    @Override
    public TimePicker setTime(final LocalTime time) {
        final LocalTime next = time == null ? null : time.truncatedTo(this.seconds ? ChronoUnit.SECONDS : ChronoUnit.MINUTES);
        final LocalTime previous = this.time;
        if (!Objects.equals(next, previous)) {
            this.time = next;
            this.valueChanged();
            this.showColumns();
            this.report(TIME_PICKED, next, previous);
        }
        return this;
    }

    @Override
    public TimePicker clearTime() {
        return this.setTime(null);
    }

    @Override
    public Optional<ClockFormat> clockFormat() {
        return Optional.ofNullable(this.clockFormat);
    }

    @Override
    public TimePicker setClockFormat(final ClockFormat format) {
        this.clockFormat = format;
        this.relayoutField();
        return this;
    }

    @Override
    public boolean isSecondsShown() {
        return this.seconds;
    }

    @Override
    public TimePicker setSecondsShown(final boolean shown) {
        if (shown != this.seconds) {
            this.seconds = shown;
            this.relayoutField();
            this.setTime(this.time);
        }
        return this;
    }

    @Override
    public int minuteStep() {
        return this.minuteStep;
    }

    @Override
    public TimePicker setMinuteStep(final int minutes) {
        if (minutes < 1 || minutes > 60 || 60 % minutes != 0) {
            throw new IllegalArgumentException("The minute step must divide 60: " + minutes);
        }
        this.minuteStep = minutes;
        this.field.setMinuteStep(minutes);
        return this;
    }

    private boolean twelveHour(final DateFormats formats) {
        return this.clockFormat == null ? formats.isTwelveHour() : this.clockFormat == ClockFormat.H12;
    }

    // ------------------------------------------------------------------
    // PickerBase
    // ------------------------------------------------------------------

    @Override
    LocalTime value() {
        return this.time;
    }

    @Override
    LocalTime draftValue() {
        return this.field.time();
    }

    @Override
    void userPick(final LocalTime value) {
        if (!Objects.equals(value, this.time)) {
            this.request(PICK_TIME, value, this::setTime);
        }
    }

    @Override
    void layoutField(final DateFormats formats) {
        this.field.layout(formats, false, true, this.twelveHour(formats), this.seconds);
    }

    @Override
    void loadField() {
        this.field.load(null, this.time);
    }

    @Override
    void fillForPopup() {
        this.field.fill(null, LocalTime.MIDNIGHT);
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        super.onApplyTheme(theme);
        this.rowStyle = theme.textStyleOr(TextBox.STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    Component<?> popupContent() {
        if (this.columns == null) {
            this.columns = new TimeColumns(this.ui, this::pickPart);
        }
        final DateFormats formats = this.field.formats() != null ? this.field.formats() : DateFormats.current();
        this.columns.build(this.twelveHour(formats), this.seconds, this.minuteStep, formats, this.rowStyle,
                this.environment().text().lineHeight(this.rowStyle));
        this.columns.show(this.time, this.twelveHour(formats));
        return this.columns.panel();
    }

    private void showColumns() {
        if (this.columns != null && this.isOpen() && this.field.formats() != null) {
            this.columns.show(this.time, this.twelveHour(this.field.formats()));
        }
    }

    /**
     * Sets one part of the time from a row the user pressed, starting an empty time at zero, and takes focus back.
     */
    private void pickPart(final SegmentedField.Kind kind, final int value) {
        this.field.load(null, this.time);
        if (this.time == null) {
            this.fillForPopup();
        }
        this.field.setPart(kind, value);
        final int index = this.field.indexOf(kind);
        final LocalTime picked = this.field.time();
        if (picked != null) {
            this.userPick(picked);
        }
        this.loadField();
        this.showColumns();
        if (!this.isFocused()) {
            this.requestFocus();
        }
        if (index >= 0) {
            this.field.setFocus(index);
        }
    }

    @Override
    String iconKey() {
        return STYLE_ICON;
    }

    @Override
    Set<String> extraRequiredKeys() {
        return EXTRA;
    }

    @Override
    protected Object snapshotState() {
        return new TimePicker.State(this.time, this.isOpen());
    }
}
