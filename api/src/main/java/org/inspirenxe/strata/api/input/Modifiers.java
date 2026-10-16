package org.inspirenxe.strata.api.input;

import java.util.StringJoiner;

/**
 * An immutable set of held modifiers, packed into an int so input events allocate nothing for it.
 */
public record Modifiers(int bits) {
    public static final Modifiers NONE = new Modifiers(0);

    public static Modifiers of(final Modifier... modifiers) {
        int bits = 0;
        for (final Modifier modifier : modifiers) {
            bits |= modifier.bit();
        }
        return new Modifiers(bits);
    }

    public boolean has(final Modifier modifier) {
        return (this.bits & modifier.bit()) != 0;
    }

    public boolean shift() {
        return this.has(Modifier.SHIFT);
    }

    public boolean control() {
        return this.has(Modifier.CONTROL);
    }

    public boolean alt() {
        return this.has(Modifier.ALT);
    }

    public boolean superKey() {
        return this.has(Modifier.SUPER);
    }

    public boolean isEmpty() {
        return this.bits == 0;
    }

    public Modifiers with(final Modifier modifier) {
        return new Modifiers(this.bits | modifier.bit());
    }

    public Modifiers without(final Modifier modifier) {
        return new Modifiers(this.bits & ~modifier.bit());
    }

    /**
     * True if every modifier in {@code other} is also in this set.
     */
    public boolean containsAll(final Modifiers other) {
        return (this.bits & other.bits) == other.bits;
    }

    @Override
    public String toString() {
        final StringJoiner joiner = new StringJoiner("+", "Modifiers[", "]");
        for (final Modifier modifier : Modifier.values()) {
            if (this.has(modifier)) {
                joiner.add(modifier.name());
            }
        }
        return joiner.toString();
    }
}
