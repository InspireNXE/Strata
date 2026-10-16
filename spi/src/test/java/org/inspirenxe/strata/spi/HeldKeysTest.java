package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldKeysTest {
    private final HeldKeys keys = new HeldKeys();

    @Test
    void firstPressIsAPressAndLaterOnesRepeat() {
        assertEquals(KeyAction.PRESS, this.keys.press(Key.A));
        assertEquals(KeyAction.REPEAT, this.keys.press(Key.A));
        assertEquals(KeyAction.REPEAT, this.keys.press(Key.A));
        assertTrue(this.keys.isHeld(Key.A));
    }

    @Test
    void releaseStartsOver() {
        this.keys.press(Key.A);
        this.keys.release(Key.A);

        assertFalse(this.keys.isHeld(Key.A));
        assertEquals(KeyAction.PRESS, this.keys.press(Key.A));
    }

    @Test
    void keysAreTrackedSeparately() {
        this.keys.press(Key.A);

        assertEquals(KeyAction.PRESS, this.keys.press(Key.B));
        assertEquals(KeyAction.REPEAT, this.keys.press(Key.A));
    }

    @Test
    void releasingAnUnheldKeyDoesNothing() {
        this.keys.release(Key.ESCAPE);
        assertEquals(KeyAction.PRESS, this.keys.press(Key.ESCAPE));
    }

    @Test
    void clearForgetsEverything() {
        this.keys.press(Key.A);
        this.keys.press(Key.LEFT_SHIFT);
        this.keys.clear();

        assertEquals(KeyAction.PRESS, this.keys.press(Key.A));
        assertEquals(KeyAction.PRESS, this.keys.press(Key.LEFT_SHIFT));
    }
}
