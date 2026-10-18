package org.inspirenxe.strata.api.theme;

import java.util.Set;

/**
 * Thrown when a theme lacks style keys that components in a tree require, or when a theme named by id cannot be
 * built. For missing keys, the message and {@link #missingKeys()} list every missing key, not just the first, so
 * one run shows all that needs adding.
 */
public final class ThemeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * The missing keys; serializable because {@link Set#copyOf} returns an immutable serializable set.
     */
    private final Set<String> missingKeys;

    /**
     * Creates the exception.
     *
     * @param theme the theme that was checked
     * @param missingKeys the keys it lacks
     */
    public ThemeException(final Theme theme, final Set<String> missingKeys) {
        super("Theme '" + theme.name() + "' is missing style keys: " + String.join(", ", missingKeys));
        this.missingKeys = Set.copyOf(missingKeys);
    }

    /**
     * Creates the exception for a theme that could not be built at all, such as an unknown id, a cycle of
     * parents, or a theme file that cannot be read. {@link #missingKeys()} is empty.
     *
     * @param message what went wrong
     */
    public ThemeException(final String message) {
        super(message);
        this.missingKeys = Set.of();
    }

    /**
     * Returns the keys the theme lacks.
     *
     * @return an unmodifiable set of the missing keys
     */
    public Set<String> missingKeys() {
        return this.missingKeys;
    }
}
