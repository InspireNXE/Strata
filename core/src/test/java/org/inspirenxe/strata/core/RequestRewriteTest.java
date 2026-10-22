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
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.control.window.MessageBox;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Requests whose value a handler rewrote to null or NaN.
 */
class RequestRewriteTest {
    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void aChangeRewrittenToNanOrNullLeavesTheValue() {
        final Slider slider = this.ui.slider();
        this.test.show(this.ui.stackPanel().add(slider));
        slider.setRange(0, 10).setValue(5);
        slider.requestFocus();
        slider.onEvent(Ranged.CHANGE, event -> event.setValue(Double.NaN));

        this.test.host().key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
        assertEquals(5, slider.value());

        slider.onEvent(Ranged.CHANGE, 1, event -> event.setValue(null));
        this.test.host().key(Key.LEFT, KeyAction.PRESS, Modifiers.NONE);
        assertEquals(5, slider.value());
    }

    @Test
    void anAnswerRewrittenToNullKeepsTheBoxOpen() {
        this.test.show(this.ui.absolutePanel());
        final MessageBox box = this.ui.messageBox("Title", "Message", MessageBox.Buttons.OK);
        final Overlay dialog = this.ui.overlays().dialog(box);
        final List<MessageBox.Result> answered = new ArrayList<>();
        box.onEvent(MessageBox.ANSWER, event -> event.setValue(null));
        box.onEvent(MessageBox.ANSWERED, event -> answered.add(event.value()));

        this.test.host().key(Key.ENTER, KeyAction.PRESS, Modifiers.NONE);

        assertTrue(dialog.isOpen());
        assertEquals(List.of(), answered);
    }
}
