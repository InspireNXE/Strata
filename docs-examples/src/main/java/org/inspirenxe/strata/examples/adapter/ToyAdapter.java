package org.inspirenxe.strata.examples.adapter;

// --8<-- [start:adapter]
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.StrataCore;
import org.inspirenxe.strata.spi.ScreenStack;

import java.util.List;

/**
 * Glue between the imaginary engine's callbacks and a Strata screen stack. The engine works in pixels; this
 * adapter uses one UI unit per {@code scale} pixels and converts both drawing and input by the same factor.
 */
public final class ToyAdapter {
    private final ScreenStack screens;
    private final RecordingCanvas canvas;
    private final float scale;

    public ToyAdapter(final Theme theme, final float scale) {
        this.screens = StrataCore.createScreenStack(new ToyPlatform(), theme);
        this.canvas = new RecordingCanvas();
        this.scale = scale;
    }

    public void open(final Screen screen) {
        this.screens.push(screen);
    }

    // Engine callbacks. All of them arrive on the engine's UI thread.

    public void onResize(final int widthPixels, final int heightPixels) {
        this.screens.resize(widthPixels / this.scale, heightPixels / this.scale);
    }

    /** Called once per frame. Returns the commands to draw, in order. */
    public List<String> onFrame() {
        this.canvas.begin();
        this.screens.render(this.canvas);
        return this.canvas.commands();
    }

    /** Returns true when Strata used the input, so the engine should not act on it too. */
    public boolean onMouseMove(final int x, final int y) {
        return this.screens.pointerMoved(x / this.scale, y / this.scale, Modifiers.NONE);
    }

    public boolean onMouseButton(final int x, final int y, final int engineButton, final boolean down,
                                 final int engineMods) {
        return this.screens.pointerButton(x / this.scale, y / this.scale, button(engineButton), down,
                modifiers(engineMods));
    }

    public boolean onWheel(final int x, final int y, final int notchesTowardUser) {
        // Strata counts a wheel turned away from the user as positive
        return this.screens.scroll(x / this.scale, y / this.scale, 0f, -notchesTowardUser, ScrollKind.WHEEL,
                Modifiers.NONE);
    }

    public void onMouseLeft() {
        this.screens.pointerLeft();
    }

    public boolean onKey(final int engineKey, final boolean down, final int engineMods) {
        // This engine sends no repeats (see ToyPlatform), so every down is a press
        return this.screens.key(key(engineKey), down ? KeyAction.PRESS : KeyAction.RELEASE, modifiers(engineMods));
    }

    public boolean onChar(final int codePoint) {
        // Characters arrive separately from keys, after the keyboard layout; control characters are not sent
        if (Character.isISOControl(codePoint)) {
            return false;
        }
        return this.screens.charTyped(codePoint, Modifiers.NONE);
    }

    // Mapping the engine's numbers to Strata's types. No engine number ever reaches a component.

    private static MouseButton button(final int engineButton) {
        return switch (engineButton) {
            case 0 -> MouseButton.LEFT;
            case 1 -> MouseButton.RIGHT;
            case 2 -> MouseButton.MIDDLE;
            default -> MouseButton.UNKNOWN;
        };
    }

    private static Modifiers modifiers(final int engineMods) {
        Modifiers modifiers = Modifiers.NONE;
        if ((engineMods & 1) != 0) {
            modifiers = modifiers.with(Modifier.SHIFT);
        }
        if ((engineMods & 2) != 0) {
            modifiers = modifiers.with(Modifier.CONTROL);
        }
        if ((engineMods & 4) != 0) {
            modifiers = modifiers.with(Modifier.ALT);
        }
        return modifiers;
    }

    private static Key key(final int engineKey) {
        return switch (engineKey) {
            case 8 -> Key.BACKSPACE;
            case 9 -> Key.TAB;
            case 13 -> Key.ENTER;
            case 27 -> Key.ESCAPE;
            case 37 -> Key.LEFT;
            case 39 -> Key.RIGHT;
            default -> engineKey >= 'A' && engineKey <= 'Z' ? Key.valueOf(String.valueOf((char) engineKey)) : Key.UNKNOWN;
        };
    }
}
// --8<-- [end:adapter]
