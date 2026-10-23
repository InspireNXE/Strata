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
import org.inspirenxe.strata.api.animation.Animation;
import org.inspirenxe.strata.api.animation.Easing;
import org.inspirenxe.strata.api.control.attribute.value.Ranged;
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.testing.ScreenTester;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The test kit's {@code settle()}, which runs animations to their end by the tester's clock.
 */
class ScreenTesterSettleTest {
    private final AtomicReference<ComponentFactory> factory = new AtomicReference<>();
    private final AtomicReference<ProgressBar> bar = new AtomicReference<>();
    private final ScreenTester tester = ScreenTester.open(ui -> {
        this.factory.set(ui);
        final ProgressBar progress = ui.progressBar();
        this.bar.set(progress);
        return progress;
    });

    @Test
    void settleRunsAnAnimationToItsEnd() {
        final Animation animation = this.factory.get().animator().animate(this.bar.get(), Ranged.VALUE, 1.0, 250L,
                Easing.EASE_IN_OUT);
        this.tester.settle();
        assertFalse(animation.isRunning());
        assertEquals(1.0, this.bar.get().value());
    }

    @Test
    void settleWithNothingMovingDoesNothing() {
        this.tester.settle();
        assertEquals(0.0, this.bar.get().value());
    }

    @Test
    void settleGivesUpOnMotionThatNeverEnds() {
        final Runnable[] again = new Runnable[1];
        again[0] = () -> this.factory.get().animator().run(100L, Easing.LINEAR, fraction -> { })
                .onFinished(again[0]);
        again[0].run();
        assertThrows(AssertionError.class, this.tester::settle);
    }
}
