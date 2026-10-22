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

import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.attribute.value.Toggle;
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.button.CheckState;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.spi.Canvas;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Image, checkbox and slider. Text in the test platform is 6 units per code point and 9 units per line.
 */
class BasicControlsTest {
    private static final SpriteId ICON = SpriteId.of("test", "icon");

    @Test
    void theFlatThemeDefinesEveryKey() {
        final List<String> keys = List.of(Button.STYLE_NORMAL, Button.STYLE_HOVERED, Button.STYLE_PRESSED,
                Button.STYLE_DISABLED, Button.STYLE_FOCUS, Button.STYLE_TEXT, Button.STYLE_TEXT_DISABLED, Label.STYLE_TEXT,
                CheckBox.STYLE_NORMAL, CheckBox.STYLE_HOVERED, CheckBox.STYLE_CHECKED, CheckBox.STYLE_CHECKED_HOVERED,
                CheckBox.STYLE_DISABLED, CheckBox.STYLE_CHECKED_DISABLED, CheckBox.STYLE_MARK, CheckBox.STYLE_MARK_DISABLED,
                CheckBox.STYLE_FOCUS, CheckBox.STYLE_TEXT, CheckBox.STYLE_TEXT_DISABLED, Slider.STYLE_TRACK,
                Slider.STYLE_TRACK_FOCUSED, Slider.STYLE_TRACK_DISABLED, Slider.STYLE_THUMB, Slider.STYLE_THUMB_HOVERED,
                Slider.STYLE_THUMB_DISABLED, Slider.STYLE_TEXT, Slider.STYLE_TEXT_DISABLED, TextBox.STYLE_NORMAL,
                TextBox.STYLE_FOCUSED, TextBox.STYLE_DISABLED, TextBox.STYLE_TEXT, TextBox.STYLE_TEXT_DISABLED,
                TextBox.STYLE_SELECTION, TextBox.STYLE_CARET);
        assertEquals(List.of(), List.copyOf(Themes.flat().missing(keys)));
    }

    /**
     * Records sprite draws.
     */
    static final class SpriteCanvas implements Canvas {
        final List<Rect> sprites = new ArrayList<>();
        final List<Integer> tints = new ArrayList<>();
        final List<String> texts = new ArrayList<>();

        @Override
        public void fill(final Rect rect, final int argb) {
        }

        @Override
        public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        }

        @Override
        public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
            this.sprites.add(rect);
            this.tints.add(tint);
        }

        @Override
        public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        }

        @Override
        public void text(final TextSpan span, final float x, final float y) {
            this.texts.add(span.text());
        }

        @Override
        public void pushClip(final Rect rect) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslation(final float dx, final float dy) {
        }

        @Override
        public void pushScale(final float scale) {
        }

        @Override
        public void popTransform() {
        }

        @Override
        public void visual(final Visual visual, final Rect rect, final int tint) {
        }

        @Override
        public void nextLayer() {
        }

        @Override
        public void requestCursor(final CursorKind cursor) {
        }
    }

    @Nested
    class Images {
        private final TestHost test = new TestHost(200, 100);

        @Test
        void desiredSizeIsTheImageSize() {
            final Image image = this.test.factory().image(ICON);
            this.test.show(this.test.factory().absolutePanel().add(image));
            assertEquals(new Rect(0, 0, 16, 16), image.bounds());

            image.setImageSize(9, 4);
            this.test.host().layout();
            assertEquals(new Rect(0, 0, 9, 4), image.bounds());
        }

        @Test
        void stretchCoversTheBoundsAndFitKeepsTheAspectRatio() {
            final Image image = this.test.factory().image(ICON).setImageSize(10, 5).setTint(0x80FFFFFF);
            image.setSize(40, 40);
            this.test.show(this.test.factory().absolutePanel().add(image));

            final SpriteCanvas stretched = new SpriteCanvas();
            this.test.host().render(stretched);
            assertEquals(List.of(new Rect(0, 0, 40, 40)), stretched.sprites);
            assertEquals(List.of(0x80FFFFFF), stretched.tints);

            image.setScaling(Image.Scaling.FIT);
            final SpriteCanvas fitted = new SpriteCanvas();
            this.test.host().render(fitted);
            assertEquals(List.of(new Rect(0, 10, 40, 20)), fitted.sprites);
        }

        @Test
        void rejectsBadSizes() {
            final Image image = this.test.factory().image(ICON);
            assertThrows(IllegalArgumentException.class, () -> image.setImageSize(-1, 4));
            assertThrows(IllegalArgumentException.class, () -> image.setImageSize(Float.NaN, 4));
            assertThrows(IllegalArgumentException.class, () -> image.setImageSize(4, Float.POSITIVE_INFINITY));
        }
    }

    @Nested
    class CheckBoxes {
        private final TestHost test = new TestHost(200, 100);
        private final CheckBox box = this.test.factory().checkBox("Music");
        private final List<Boolean> seen = new ArrayList<>();

        CheckBoxes() {
            this.box.onEvent(Toggle.TOGGLED, event -> this.seen.add(event.value()));
            this.test.show(this.test.factory().absolutePanel().add(this.box));
        }

        @Test
        void sizeIsTheBoxTheGapAndTheCaption() {
            // Box 9 + 8 = 17, gap 4, caption 5 * 6 = 30
            assertEquals(new Rect(0, 0, 51, 17), this.box.bounds());
            assertEquals(17f, this.box.boxSize());

            this.box.setBoxSize(10);
            this.test.host().layout();
            assertEquals(new Rect(0, 0, 44, 10), this.box.bounds());
        }

        @Test
        void clickingAnywhereToggles() {
            this.test.click(45, 8);
            assertTrue(this.box.isSelected());
            this.test.click(2, 2);
            assertFalse(this.box.isSelected());
            assertEquals(List.of(true, false), this.seen);
        }

        @Test
        void spaceAndEnterToggleButNotOnRepeat() {
            this.box.requestFocus();
            assertTrue(this.test.host().key(Key.SPACE, KeyAction.PRESS, Modifiers.NONE));
            this.test.host().key(Key.SPACE, KeyAction.REPEAT, Modifiers.NONE);
            this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);
            assertEquals(List.of(true, false), this.seen);
        }

        @Test
        void disabledIgnoresInputButSetCheckedStillWorks() {
            this.box.setEnabled(false);
            this.test.click(45, 8);
            this.box.toggle();
            assertFalse(this.box.isSelected());

            this.box.setSelected(true).setSelected(true);
            assertEquals(List.of(true), this.seen);
        }

        @Test
        void aClickIsConsumed() {
            final List<Object> bubbled = new ArrayList<>();
            this.box.parent().orElseThrow().onEvent(InputEvents.CLICK, bubbled::add);
            this.test.click(45, 8);
            assertEquals(List.of(), bubbled);
        }

        @Test
        void snapshotsRecordTheStateBeforeTheClick() {
            final List<CheckBox.State> before = new ArrayList<>();
            this.box.onEvent(InputEvents.CLICK, event -> event.snapshot().state(CheckBox.State.class).ifPresent(before::add));
            this.test.click(45, 8);
            assertEquals(List.of(new CheckBox.State(CheckState.UNCHECKED, Text.of("Music"))), before);
        }
    }

    @Nested
    class Sliders {
        // 150 wide: the thumb's center travels from x = 4 to x = 146, 142 units for the whole range
        private final TestHost test = new TestHost(200, 100);
        private final Slider slider = this.test.factory().slider().setRange(0, 142);
        private final List<Double> seen = new ArrayList<>();

        Sliders() {
            this.slider.onEvent(Ranged.CHANGED, event -> this.seen.add(event.value()));
            this.test.show(this.test.factory().absolutePanel().add(this.slider));
        }

        private void press(final float x) {
            this.test.host().pointerButton(x, 10, MouseButton.LEFT, true, Modifiers.NONE);
        }

        private void key(final Key key) {
            this.test.host().key(key, KeyAction.PRESS, Modifiers.NONE);
        }

        @Test
        void defaultsAndSize() {
            final Slider plain = this.test.factory().slider();
            assertEquals(0d, plain.value());
            assertEquals(0d, plain.min());
            assertEquals(1d, plain.max());
            assertEquals(0d, plain.step());
            assertEquals(new Rect(0, 0, 150, 20), this.slider.bounds());
        }

        @Test
        void aPressMovesTheThumbCenterToThePointer() {
            this.press(54);
            assertEquals(50d, this.slider.value(), 1e-9);
            // Inside the slider but beyond the thumb's travel: clamped to the ends
            this.press(1);
            assertEquals(0d, this.slider.value());
            this.press(149);
            assertEquals(142d, this.slider.value());
        }

        @Test
        void draggingFollowsThePointerOutsideTheSlider() {
            this.press(4);
            this.test.host().pointerMoved(30, 10, Modifiers.NONE);
            assertEquals(26d, this.slider.value(), 1e-9);
            this.test.host().pointerMoved(60, 80, Modifiers.NONE);
            assertEquals(56d, this.slider.value(), 1e-9);
            this.test.host().pointerButton(60, 80, MouseButton.LEFT, false, Modifiers.NONE);
            assertEquals(2, this.seen.size());
            assertEquals(56d, this.seen.get(1), 1e-9);
        }

        @Test
        void valuesSnapToTheStepButTheMaximumStaysReachable() {
            this.slider.setRange(0, 1).setStep(0.3);
            this.slider.setValue(0.5);
            assertEquals(0.6d, this.slider.value(), 1e-9);
            this.slider.setValue(0.98);
            assertEquals(1d, this.slider.value());
            this.slider.setValue(0.92);
            assertEquals(0.9d, this.slider.value(), 1e-9);
        }

        @Test
        void keysMoveByTheKeyStep() {
            this.slider.setRange(0, 100).setStep(5).requestFocus();
            this.key(Key.RIGHT);
            this.key(Key.UP);
            assertEquals(10d, this.slider.value());
            this.key(Key.PAGE_UP);
            assertEquals(60d, this.slider.value());
            this.key(Key.LEFT);
            assertEquals(55d, this.slider.value());
            this.key(Key.END);
            assertEquals(100d, this.slider.value());
            this.key(Key.HOME);
            assertEquals(0d, this.slider.value());
            this.slider.setStep(0);
            assertEquals(1d, this.slider.keyStep());
        }

        @Test
        void listenersRunOnlyOnChange() {
            this.slider.setValue(10).setValue(10).setValue(-5).setValue(-5);
            assertEquals(List.of(10d, 0d), this.seen);
        }

        @Test
        void narrowingTheRangeClampsTheValue() {
            this.slider.setValue(100).setRange(0, 50);
            assertEquals(50d, this.slider.value());
        }

        @Test
        void rejectsBadArguments() {
            assertThrows(IllegalArgumentException.class, () -> this.slider.setRange(1, 1));
            assertThrows(IllegalArgumentException.class, () -> this.slider.setRange(0, Double.POSITIVE_INFINITY));
            assertThrows(IllegalArgumentException.class, () -> this.slider.setStep(-1));
            assertThrows(IllegalArgumentException.class, () -> this.slider.setValue(Double.NaN));
        }

        @Test
        void theLabelFollowsTheValue() {
            this.slider.setLabel(value -> "Volume: " + (int) value);
            assertEquals("Volume: 0", this.slider.labelText());
            this.slider.setValue(42);
            assertEquals("Volume: 42", this.slider.labelText());

            final SpriteCanvas canvas = new SpriteCanvas();
            this.test.host().render(canvas);
            assertEquals(List.of("Volume: 42"), canvas.texts);
        }

        @Test
        void disabledIgnoresInput() {
            this.slider.setEnabled(false);
            this.press(54);
            assertEquals(0d, this.slider.value());
        }
    }
}
