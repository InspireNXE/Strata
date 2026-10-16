package org.inspirenxe.strata.api.input;

/**
 * How a keyboard shortcut treats the modifiers held when its key is pressed. See
 * {@link org.inspirenxe.strata.api.event.KeyEvent#is}.
 */
public enum ModifierPolicy {
    /**
     * Matches whatever modifiers are held.
     */
    IGNORE,
    /**
     * Matches only when exactly the required modifiers are held: Ctrl+S does not fire for Ctrl+Shift+S.
     */
    EXACT,
    /**
     * Matches when the required modifiers are held, whatever else is held too: Ctrl+S also fires for Ctrl+Shift+S.
     */
    AT_LEAST,
    /**
     * Matches only when no modifier is held. The required modifiers are ignored.
     */
    FORBID;

    /**
     * Returns whether the held modifiers satisfy this policy.
     *
     * @param required the modifiers the shortcut asks for
     * @param held the modifiers held when the key was pressed
     * @return true if the shortcut should fire
     */
    public boolean matches(final Modifiers required, final Modifiers held) {
        return switch (this) {
            case IGNORE -> true;
            case EXACT -> held.equals(required);
            case AT_LEAST -> held.containsAll(required);
            case FORBID -> held.isEmpty();
        };
    }
}
