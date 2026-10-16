package org.inspirenxe.strata.core.input;

import org.inspirenxe.strata.api.input.MouseButton;

/**
 * Counts presses into single, double and triple clicks.
 *
 * <p>A press continues the sequence when it hits the same target with the same button, comes within
 * {@link #MULTI_CLICK_MILLIS} of the previous press, and lands within {@link #MULTI_CLICK_DISTANCE} of it. After a
 * triple click the next press starts again at one.</p>
 *
 * <p>Unlike Wintergreen's recognizer this never holds a release back waiting for a possible second click. Every
 * press and release is delivered at once with its count, and widgets react to the count they care about (a text box
 * selects a word on two, everything on three). Holding releases back added up to half a second of lag to every
 * single click.</p>
 */
public final class ClickCounter {
    public static final long MULTI_CLICK_MILLIS = 500L;
    public static final float MULTI_CLICK_DISTANCE = 4f;
    public static final int MAX_CLICKS = 3;

    private Object lastTarget;
    private MouseButton lastButton;
    private long lastTime;
    private float lastX;
    private float lastY;
    private int count;

    /**
     * Records a press and returns its click count, from 1 to {@link #MAX_CLICKS}.
     */
    public int press(final Object target, final MouseButton button, final float x, final float y, final long timeMillis) {
        final boolean continues = this.count > 0
                && this.count < MAX_CLICKS
                && target == this.lastTarget
                && button == this.lastButton
                && timeMillis - this.lastTime <= MULTI_CLICK_MILLIS
                && Math.abs(x - this.lastX) <= MULTI_CLICK_DISTANCE
                && Math.abs(y - this.lastY) <= MULTI_CLICK_DISTANCE;

        this.count = continues ? this.count + 1 : 1;
        this.lastTarget = target;
        this.lastButton = button;
        this.lastTime = timeMillis;
        this.lastX = x;
        this.lastY = y;
        return this.count;
    }

    /**
     * The count of the most recent press, or 0 if there has been none since the last reset.
     */
    public int current() {
        return this.count;
    }

    public void reset() {
        this.count = 0;
        this.lastTarget = null;
        this.lastButton = null;
    }
}
