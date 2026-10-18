package org.inspirenxe.strata.spi;

import java.util.Objects;

/**
 * One theme file, read by a {@link ThemeSource}.
 *
 * @param origin where the file came from, such as a pack name and path; used only in error messages
 * @param content the file's JSON text
 */
public record ThemeDocument(String origin, String content) {
    /**
     * Checks the arguments.
     */
    public ThemeDocument {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(content, "content");
    }
}
