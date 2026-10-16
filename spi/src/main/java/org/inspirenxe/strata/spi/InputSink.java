package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.input.MouseButton;
import org.inspirenxe.strata.api.input.ScrollKind;

/**
 * Where an adapter sends input, in UI coordinates. Each method returns true when Strata consumed the input, so the
 * engine can pass the rest to its own UI.
 */
public interface InputSink {

    boolean pointerMoved(float x, float y, Modifiers modifiers);

    boolean pointerButton(float x, float y, MouseButton button, boolean pressed, Modifiers modifiers);

    boolean scroll(float x, float y, float scrollX, float scrollY, ScrollKind kind, Modifiers modifiers);

    /**
     * The pointer left the window or the area Strata owns.
     */
    void pointerLeft();

    /**
     * @param action {@code REPEAT} for engine-sent repeats; an adapter whose engine reports repeats as presses must
     *               tell them apart itself
     */
    boolean key(Key key, KeyAction action, Modifiers modifiers);

    boolean charTyped(int codePoint, Modifiers modifiers);
}
