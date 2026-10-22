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
import org.inspirenxe.strata.api.control.field.Slider;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.state.State;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The slider's committed value, its ticks and snapping to them (design 4.57). The slider is 108 wide at 10, 10, so
 * the thumb's center travels 100 units from x 14, one unit per value from 0 to 100.
 */
class SliderCommitTest {
    private final TestHost test = new TestHost(300, 100);
    private final ComponentFactory ui = this.test.factory();
    private final Slider slider = this.ui.slider().setRange(0, 100);
    private final List<String> heard = new ArrayList<>();

    SliderCommitTest() {
        this.slider.setSize(108, 20);
        this.slider.onEvent(Ranged.CHANGED, event -> this.heard.add("changed " + event.value().intValue()))
                .onEvent(Slider.COMMITTED, event -> this.heard.add("committed " + event.previous().intValue() + "->"
                        + event.value().intValue()));
        this.test.show(this.ui.absolutePanel().add(this.slider, 10, 10));
        this.test.host().layout();
    }

    private void drag(final float... xs) {
        this.test.host().pointerButton(xs[0], 20, MouseButton.LEFT, true, Modifiers.NONE);
        for (int i = 1; i < xs.length; i++) {
            this.test.host().pointerMoved(xs[i], 20, Modifiers.NONE);
            assertTrue(this.slider.isAdjusting());
        }
        this.test.host().pointerButton(xs[xs.length - 1], 20, MouseButton.LEFT, false, Modifiers.NONE);
    }

    @Test
    void aDragCommitsOnceWhenItEnds() {
        this.drag(34, 44, 54, 64);
        assertFalse(this.slider.isAdjusting());
        assertEquals(List.of("changed 20", "changed 30", "changed 40", "changed 50", "committed 0->50"), this.heard);
    }

    @Test
    void aDragThatEndsWhereItBeganCommitsNothing() {
        this.slider.setValue(20);
        this.heard.clear();
        this.drag(34, 64, 34);
        assertEquals(List.of("changed 50", "changed 20"), this.heard);
    }

    @Test
    void keysAndCodeCommitEachChange() {
        this.slider.setStep(1).requestFocus();
        this.test.host().key(Key.RIGHT, KeyAction.PRESS, Modifiers.NONE);
        this.slider.setValue(30);
        this.slider.setValue(30);
        assertEquals(List.of("changed 1", "committed 0->1", "changed 30", "committed 1->30"), this.heard);
    }

    @Test
    void aBindingThroughTheCommittedValueWritesOncePerDrag() {
        final State<Double> volume = State.of(0d);
        final List<Double> writes = new ArrayList<>();
        volume.subscribe(writes::add);
        this.slider.bind(Slider.COMMITTED_VALUE, volume);
        this.test.host().flush();
        this.drag(34, 44, 54);
        this.test.host().flush();
        assertEquals(40d, volume.get());
        assertEquals(List.of(40d), writes);
    }

    @Test
    void ticksAreDrawnAtTheThumbsCenterAndCloseOnesAreLeftOut() {
        this.slider.setTicks(25, 4);
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        final Slider check = this.slider;
        assertEquals(25d, check.majorTicks());
        assertEquals(4, check.minorTicks());
        final List<String> marks = log.lines("fill").stream().filter(line -> line.contains(" 1x")).toList();
        assertEquals(5 + 16, marks.size(), marks.toString());
        assertTrue(marks.contains("fill 14,26 1x4 #FF8A8F98"), marks.toString());
        assertTrue(marks.contains("fill 114,26 1x4 #FF8A8F98"), marks.toString());
        assertTrue(marks.contains("fill 19,28 1x2 #FF4E525A"), marks.toString());
        this.slider.setTicks(1, 0);
        final DrawLog none = new DrawLog();
        this.test.host().render(none);
        assertEquals(List.of(), none.lines("fill").stream().filter(line -> line.contains(" 1x")).toList());
    }

    @Test
    void aVerticalSliderDrawsItsTicksAtTheLeft() {
        this.slider.setOrientation(Orientation.VERTICAL).setSize(20, 108).setTicks(50, 0);
        this.test.host().layout();
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        assertTrue(log.lines().contains("fill 10,114 4x1 #FF8A8F98"), log.lines().toString());
        assertTrue(log.lines().contains("fill 10,14 4x1 #FF8A8F98"), log.lines().toString());
    }

    @Test
    void snappingToTicksUsesTheFinestTickInPlaceOfTheStep() {
        this.slider.setStep(1).setTicks(10, 1).setValue(33);
        assertEquals(33d, this.slider.value());
        this.slider.setSnapToTicks(true);
        assertTrue(this.slider.snapsToTicks());
        assertEquals(35d, this.slider.value());
        assertEquals(5d, this.slider.keyStep());
        this.slider.setValue(12.4);
        assertEquals(10d, this.slider.value());
        this.slider.setTicks(0, 0);
        this.slider.setValue(12.4);
        assertEquals(12d, this.slider.value());
        assertThrows(IllegalArgumentException.class, () -> this.slider.setTicks(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> this.slider.setTicks(1, -1));
    }
}
