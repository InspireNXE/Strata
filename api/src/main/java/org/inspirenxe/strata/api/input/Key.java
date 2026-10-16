package org.inspirenxe.strata.api.input;

/**
 * Physical keys, independent of any engine.
 *
 * <p>Each adapter maps its engine's key codes (GLFW, SDL, AWT) to these constants, so no engine number reaches a
 * component. Keys name physical positions on a US layout: {@link #A} is the key labeled A there, whatever the
 * user's layout prints on it. Use {@link org.inspirenxe.strata.api.event.CharEvent} for the characters a user types.
 * Adapters map any key they cannot identify to {@link #UNKNOWN}.</p>
 *
 * <p>Ordinals carry no meaning and may change between versions; store keys by {@link #name()}.</p>
 */
public enum Key {
    /** A key the adapter could not map. */
    UNKNOWN,

    /** Letter A. */
    A,
    /** Letter B. */
    B,
    /** Letter C. */
    C,
    /** Letter D. */
    D,
    /** Letter E. */
    E,
    /** Letter F. */
    F,
    /** Letter G. */
    G,
    /** Letter H. */
    H,
    /** Letter I. */
    I,
    /** Letter J. */
    J,
    /** Letter K. */
    K,
    /** Letter L. */
    L,
    /** Letter M. */
    M,
    /** Letter N. */
    N,
    /** Letter O. */
    O,
    /** Letter P. */
    P,
    /** Letter Q. */
    Q,
    /** Letter R. */
    R,
    /** Letter S. */
    S,
    /** Letter T. */
    T,
    /** Letter U. */
    U,
    /** Letter V. */
    V,
    /** Letter W. */
    W,
    /** Letter X. */
    X,
    /** Letter Y. */
    Y,
    /** Letter Z. */
    Z,

    /** Digit 0 on the number row. */
    NUM_0,
    /** Digit 1 on the number row. */
    NUM_1,
    /** Digit 2 on the number row. */
    NUM_2,
    /** Digit 3 on the number row. */
    NUM_3,
    /** Digit 4 on the number row. */
    NUM_4,
    /** Digit 5 on the number row. */
    NUM_5,
    /** Digit 6 on the number row. */
    NUM_6,
    /** Digit 7 on the number row. */
    NUM_7,
    /** Digit 8 on the number row. */
    NUM_8,
    /** Digit 9 on the number row. */
    NUM_9,

    /** Space bar. */
    SPACE,
    /** Apostrophe and quote key. */
    APOSTROPHE,
    /** Comma key. */
    COMMA,
    /** Minus key on the main keyboard. */
    MINUS,
    /** Period key on the main keyboard. */
    PERIOD,
    /** Slash key on the main keyboard. */
    SLASH,
    /** Semicolon key. */
    SEMICOLON,
    /** Equals key on the main keyboard. */
    EQUALS,
    /** Left bracket key. */
    LEFT_BRACKET,
    /** Backslash key. */
    BACKSLASH,
    /** Right bracket key. */
    RIGHT_BRACKET,
    /** Grave accent (backtick) key. */
    GRAVE,

    /** Escape. */
    ESCAPE,
    /** Enter (Return) on the main keyboard. */
    ENTER,
    /** Tab. */
    TAB,
    /** Backspace. */
    BACKSPACE,
    /** Insert. */
    INSERT,
    /** Delete (forward delete). */
    DELETE,
    /** Left arrow. */
    LEFT,
    /** Right arrow. */
    RIGHT,
    /** Up arrow. */
    UP,
    /** Down arrow. */
    DOWN,
    /** Page Up. */
    PAGE_UP,
    /** Page Down. */
    PAGE_DOWN,
    /** Home. */
    HOME,
    /** End. */
    END,

    /** Caps Lock. */
    CAPS_LOCK,
    /** Num Lock. */
    NUM_LOCK,
    /** Scroll Lock. */
    SCROLL_LOCK,
    /** Print Screen. */
    PRINT_SCREEN,
    /** Pause. */
    PAUSE,

    /** Function key F1. */
    F1,
    /** Function key F2. */
    F2,
    /** Function key F3. */
    F3,
    /** Function key F4. */
    F4,
    /** Function key F5. */
    F5,
    /** Function key F6. */
    F6,
    /** Function key F7. */
    F7,
    /** Function key F8. */
    F8,
    /** Function key F9. */
    F9,
    /** Function key F10. */
    F10,
    /** Function key F11. */
    F11,
    /** Function key F12. */
    F12,

    /** Keypad 0. */
    KP_0,
    /** Keypad 1. */
    KP_1,
    /** Keypad 2. */
    KP_2,
    /** Keypad 3. */
    KP_3,
    /** Keypad 4. */
    KP_4,
    /** Keypad 5. */
    KP_5,
    /** Keypad 6. */
    KP_6,
    /** Keypad 7. */
    KP_7,
    /** Keypad 8. */
    KP_8,
    /** Keypad 9. */
    KP_9,
    /** Keypad decimal point. */
    KP_DECIMAL,
    /** Keypad divide. */
    KP_DIVIDE,
    /** Keypad multiply. */
    KP_MULTIPLY,
    /** Keypad subtract. */
    KP_SUBTRACT,
    /** Keypad add. */
    KP_ADD,
    /** Keypad Enter. */
    KP_ENTER,
    /** Keypad equals. */
    KP_EQUALS,

    /** Left Shift. */
    LEFT_SHIFT,
    /** Left Control. */
    LEFT_CONTROL,
    /** Left Alt (Option on macOS). */
    LEFT_ALT,
    /** Left Super (Windows key, or Command on macOS). */
    LEFT_SUPER,
    /** Right Shift. */
    RIGHT_SHIFT,
    /** Right Control. */
    RIGHT_CONTROL,
    /** Right Alt, including AltGr. */
    RIGHT_ALT,
    /** Right Super (Windows key, or Command on macOS). */
    RIGHT_SUPER,
    /** Menu (context menu) key. */
    MENU;

    /**
     * Returns whether this is one of the eight left or right Shift, Control, Alt or Super keys.
     *
     * @return true for a modifier key
     */
    public boolean isModifier() {
        return switch (this) {
            case LEFT_SHIFT, LEFT_CONTROL, LEFT_ALT, LEFT_SUPER,
                 RIGHT_SHIFT, RIGHT_CONTROL, RIGHT_ALT, RIGHT_SUPER -> true;
            default -> false;
        };
    }

    /**
     * Returns whether this is either Enter key.
     *
     * @return true for {@link #ENTER} and {@link #KP_ENTER}
     */
    public boolean isEnter() {
        return this == ENTER || this == KP_ENTER;
    }

    /**
     * Returns whether pressing this key usually types a character: letters, number-row digits, punctuation, space,
     * and keypad digits and operators. A text field uses this to keep such keys from also triggering engine
     * shortcuts while it has focus.
     *
     * @return true for a key that types a character
     */
    public boolean isPrintable() {
        return (this.compareTo(A) >= 0 && this.compareTo(GRAVE) <= 0)
                || (this.compareTo(KP_0) >= 0 && this.compareTo(KP_ADD) <= 0)
                || this == KP_EQUALS;
    }
}
