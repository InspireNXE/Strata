package org.inspirenxe.strata.core.input;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * Synthesizes key repeats for engines that do not send them. Only navigation and delete keys repeat, which is what
 * text editing needs. Driven by {@link #poll(long)} once a frame.
 */
public final class KeyRepeater {
    public static final long INITIAL_DELAY_MILLIS = 400L;
    public static final long INTERVAL_MILLIS = 33L;

    private Key key;
    private Modifiers modifiers;
    private long nextFire;

    public KeyRepeater() {
        this.modifiers = Modifiers.NONE;
    }

    public static boolean isRepeatable(final Key key) {
        return switch (key) {
            case LEFT, RIGHT, UP, DOWN, BACKSPACE, DELETE, HOME, END, PAGE_UP, PAGE_DOWN -> true;
            default -> false;
        };
    }

    public void onKey(final Key key, final KeyAction action, final Modifiers modifiers, final long nowMillis) {
        if (action == KeyAction.PRESS && isRepeatable(key)) {
            this.key = key;
            this.modifiers = modifiers;
            this.nextFire = nowMillis + INITIAL_DELAY_MILLIS;
        } else if (action == KeyAction.RELEASE && key == this.key) {
            this.stop();
        }
    }

    /**
     * Returns the key to repeat now, or {@code null} if nothing is due.
     */
    public Key poll(final long nowMillis) {
        if (this.key == null || nowMillis < this.nextFire) {
            return null;
        }
        this.nextFire = nowMillis + INTERVAL_MILLIS;
        return this.key;
    }

    public Modifiers modifiers() {
        return this.modifiers;
    }

    public void stop() {
        this.key = null;
        this.modifiers = Modifiers.NONE;
    }
}
