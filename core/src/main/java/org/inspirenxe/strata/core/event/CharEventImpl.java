package org.inspirenxe.strata.core.event;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.event.input.CharEvent;
import org.inspirenxe.strata.api.input.Modifiers;

/**
 * The core {@link CharEvent}. The host creates one for each code point the engine reports as typed.
 */
public final class CharEventImpl extends EventImpl implements CharEvent {
    private final int codePoint;
    private final Modifiers modifiers;

    /**
     * Creates the event.
     *
     * @param target the focused component, or the screen's root when nothing has focus
     * @param codePoint the typed Unicode code point
     * @param modifiers the modifiers held while typing, or {@link Modifiers#NONE} when the engine does not report
     *     them
     */
    public CharEventImpl(final Component<?> target, final int codePoint, final Modifiers modifiers) {
        super(target);
        this.codePoint = codePoint;
        this.modifiers = modifiers;
    }

    @Override
    public int codePoint() {
        return this.codePoint;
    }

    @Override
    public Modifiers modifiers() {
        return this.modifiers;
    }
}
