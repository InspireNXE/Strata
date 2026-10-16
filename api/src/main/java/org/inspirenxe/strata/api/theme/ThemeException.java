package org.inspirenxe.strata.api.theme;

import java.util.Set;

/**
 * Thrown when a theme lacks style keys that the components in a tree need. Lists every missing key at once.
 */
public final class ThemeException extends RuntimeException {
    private final Set<String> missingKeys;

    public ThemeException(final Theme theme, final Set<String> missingKeys) {
        super("Theme '" + theme.name() + "' is missing style keys: " + String.join(", ", missingKeys));
        this.missingKeys = Set.copyOf(missingKeys);
    }

    public Set<String> missingKeys() {
        return this.missingKeys;
    }
}
