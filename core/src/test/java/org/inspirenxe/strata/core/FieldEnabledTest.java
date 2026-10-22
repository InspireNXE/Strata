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
import org.inspirenxe.strata.api.control.field.ColorPicker;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.field.Spinner;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.TextSpan;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * How fields follow their enabled flag: the text takes the disabled style on the next frame after a change, and the
 * private field of a spinner or color picker follows its owner at once, without waiting for a frame.
 */
class FieldEnabledTest {
    private static final int TEXT = 0xFFE0E0E0;
    private static final int TEXT_DISABLED = 0xFF707070;

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();

    private List<TextSpan> drawnText() {
        final NullCanvas canvas = new NullCanvas();
        this.test.host().render(canvas);
        return canvas.texts();
    }

    @Nested
    class TextStyles {

        @Test
        void aTextBoxDisabledAfterItsFirstFrameDrawsItsTextDisabled() {
            final TextBox box = FieldEnabledTest.this.ui.textBox().setText("ab").setAnchor(Anchor.TOP_LEFT);
            FieldEnabledTest.this.test.show(box);
            assertEquals(TEXT, FieldEnabledTest.this.drawnText().getFirst().style().color());

            box.setEnabled(false);
            assertEquals(TEXT_DISABLED, FieldEnabledTest.this.drawnText().getFirst().style().color());

            box.setEnabled(true);
            assertEquals(TEXT, FieldEnabledTest.this.drawnText().getFirst().style().color());
        }

        @Test
        void aPasswordBoxDisabledAfterItsFirstFrameDrawsItsMaskDisabled() {
            final PasswordTextBox box = FieldEnabledTest.this.ui.passwordTextBox().setPassword("ab".toCharArray());
            box.setAnchor(Anchor.TOP_LEFT);
            FieldEnabledTest.this.test.show(box);
            assertEquals(TEXT, FieldEnabledTest.this.drawnText().getFirst().style().color());

            box.setEnabled(false);
            assertEquals(TEXT_DISABLED, FieldEnabledTest.this.drawnText().getFirst().style().color());
        }
    }

    @Nested
    class PrivateFields {

        @Test
        void aSpinnersFieldIsDisabledAsSoonAsTheSpinnerIs() {
            final Spinner spinner = FieldEnabledTest.this.ui.spinner().setAnchor(Anchor.TOP_LEFT);
            FieldEnabledTest.this.test.show(spinner);
            FieldEnabledTest.this.test.click(10, 9);
            final TextBox field = (TextBox) FieldEnabledTest.this.test.focused();

            spinner.setEnabled(false);
            assertFalse(field.isEnabled(), "the field follows before any frame is drawn");

            spinner.setEnabled(true);
            assertTrue(field.isEnabled());
        }

        @Test
        void aColorPickersFieldIsDisabledAsSoonAsThePickerIs() {
            final ColorPicker picker = FieldEnabledTest.this.ui.colorPicker().setAnchor(Anchor.TOP_LEFT);
            FieldEnabledTest.this.test.show(picker);
            FieldEnabledTest.this.test.host().key(Key.TAB, KeyAction.PRESS, Modifiers.NONE);
            final TextBox field = (TextBox) FieldEnabledTest.this.test.focused();

            picker.setEnabled(false);
            assertFalse(field.isEnabled(), "the field follows before any frame is drawn");
        }
    }
}
