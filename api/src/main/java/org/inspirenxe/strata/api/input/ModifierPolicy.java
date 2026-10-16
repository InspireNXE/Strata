package org.inspirenxe.strata.api.input;

/**
 * How a shortcut treats held modifiers.
 */
public enum ModifierPolicy {
    /**
     * Fires whatever modifiers are held.
     */
    IGNORE,
    /**
     * Fires only when exactly the required modifiers are held.
     */
    EXACT,
    /**
     * Fires when the required modifiers are held, whatever else is held too.
     */
    AT_LEAST,
    /**
     * Fires only when no modifier is held.
     */
    FORBID;

    public boolean matches(final Modifiers required, final Modifiers held) {
        return switch (this) {
            case IGNORE -> true;
            case EXACT -> held.equals(required);
            case AT_LEAST -> held.containsAll(required);
            case FORBID -> held.isEmpty();
        };
    }
}
