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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Spinners, sliders and the color picker's hex field: how their values step, snap and parse.
 */
class NumericFieldsTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private void key(final Key key) {
        this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        this.test.host().key(key, KeyAction.RELEASE, Modifiers.NONE);
    }

    @Nested
    class SpinnerSteps {
        private final List<Double> heard = new ArrayList<>();
        private Spinner spinner;

        private void show(final double step, final int decimals) {
            this.spinner = NumericFieldsTest.this.ui.spinner().setAnchor(Anchor.TOP_LEFT);
            this.spinner.setStep(step).setDecimals(decimals);
            this.spinner.onEvent(Ranged.CHANGED, event -> this.heard.add(event.value()));
            NumericFieldsTest.this.test.show(this.spinner);
            NumericFieldsTest.this.test.click(10, 9);
        }

        @Test
        void aStepFinerThanTheDecimalsStillMovesTheValue() {
            this.show(0.25, 0);

            NumericFieldsTest.this.key(Key.UP);
            NumericFieldsTest.this.key(Key.UP);
            NumericFieldsTest.this.key(Key.DOWN);

            assertEquals(List.of(1.0, 2.0, 1.0), this.heard);
        }

        @Test
        void aHalfStepDoesNotStickGoingDown() {
            this.show(0.5, 0);
            this.spinner.setValue(1);
            this.heard.clear();

            NumericFieldsTest.this.key(Key.DOWN);

            assertEquals(List.of(0.0), this.heard);
        }

        @Test
        void codeStepsTheSameWay() {
            this.show(0.25, 0);

            this.spinner.increment();
            this.spinner.increment();
            this.spinner.decrement();

            assertEquals(List.of(1.0, 2.0, 1.0), this.heard);
        }

        @Test
        void aStepAtTheEndOfTheRangeFiresNothing() {
            this.show(0.25, 0);
            this.spinner.setRange(0, 1).setValue(1);
            this.heard.clear();

            NumericFieldsTest.this.key(Key.UP);

            assertEquals(List.of(), this.heard);
        }
    }

    @Nested
    class SliderSnapping {

        @Test
        void aSnappedValueIsTheDecimalOnTheGrid() {
            final Slider slider = NumericFieldsTest.this.ui.slider().setStep(0.1);

            slider.setValue(0.3);
            assertEquals(0.3, slider.value());

            slider.setRange(-1, 1).setValue(0.7);
            assertEquals(0.7, slider.value());
        }

        @Test
        void aKeyStepLandsOnTheDecimalToo() {
            final Slider slider = NumericFieldsTest.this.ui.slider().setStep(0.1).setValue(0.2);
            slider.setAnchor(Anchor.TOP_LEFT);
            NumericFieldsTest.this.test.show(slider);
            slider.requestFocus();

            NumericFieldsTest.this.key(Key.RIGHT);

            assertEquals(0.3, slider.value());
        }
    }

    @Nested
    class ColorPickerHex {
        private ColorPicker picker;

        private void show(final boolean alphaShown) {
            this.picker = NumericFieldsTest.this.ui.colorPicker().setAlphaShown(alphaShown);
            this.picker.setAnchor(Anchor.TOP_LEFT);
            NumericFieldsTest.this.test.show(this.picker);
            // The picker is a tab stop of its own, before its field
            NumericFieldsTest.this.key(Key.TAB);
            NumericFieldsTest.this.key(Key.TAB);
            NumericFieldsTest.this.test.host().key(Key.A, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        }

        private void typeAndEnter(final String text) {
            text.codePoints().forEach(code -> NumericFieldsTest.this.test.host().charTyped(code, Modifiers.NONE));
            NumericFieldsTest.this.key(Key.ENTER);
        }

        @Test
        void eightDigitsCannotSetTheAlphaWhileItsStripIsHidden() {
            this.show(false);

            this.typeAndEnter("80FF0000");

            assertEquals(0xFF, this.picker.color() >>> 24, "the alpha the user cannot see stays as it was");
        }

        @Test
        void eightDigitsSetTheAlphaWhileItsStripShows() {
            this.show(true);

            this.typeAndEnter("80FF0000");

            assertEquals(0x80FF0000, this.picker.color());
        }
    }
}
