package org.inspirenxe.strata.api.layout;

/**
 * The size rule for one grid row or column.
 */
public sealed interface GridLength {
    GridLength AUTO = new Auto();

    static GridLength auto() {
        return AUTO;
    }

    static GridLength fixed(final float size) {
        return new Fixed(size);
    }

    static GridLength weighted(final float weight) {
        return new Weighted(weight);
    }

    /**
     * As large as the largest child in the track.
     */
    record Auto() implements GridLength {
    }

    /**
     * A fixed size in UI units.
     */
    record Fixed(float size) implements GridLength {
        public Fixed {
            if (size < 0f) {
                throw new IllegalArgumentException("Fixed size must not be negative: " + size);
            }
        }
    }

    /**
     * A share of the space left after fixed and auto tracks, proportional to the weight.
     */
    record Weighted(float weight) implements GridLength {
        public Weighted {
            if (weight <= 0f) {
                throw new IllegalArgumentException("Weight must be positive: " + weight);
            }
        }
    }
}
