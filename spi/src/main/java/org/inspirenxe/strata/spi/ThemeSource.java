package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;

import java.util.List;
import java.util.Optional;

/**
 * Where the engine keeps themes: the themes the adapter builds in code, and the theme files the engine's packs
 * provide. Core resolves a {@link org.inspirenxe.strata.api.theme.ThemeRef} against it and owns the file format,
 * the parent chain and the merge. The source only finds things.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #base} returns themes built in code, such as an engine theme drawn with the engine's own widget
 *     images. Core registers {@code strata:flat} itself, so a source need not return it.</li>
 *     <li>{@link #documents} returns every theme file for an id in the order the engine stacks its packs, lowest
 *     priority first. Later documents win per key.</li>
 *     <li>{@link #version()} changes whenever anything the other two methods return may have changed, such as
 *     after the engine reloads its assets. Hosts compare it on every layout and render, so it must be cheap.</li>
 *     <li>Calls come from the UI thread. {@link #version()} may be changed from another thread, so store it in a
 *     volatile or atomic field.</li>
 *     <li>Never throw for a missing id: return empty. A file that cannot be read is left out.</li>
 * </ul>
 */
public interface ThemeSource {
    /**
     * A source with no themes of its own and no files. Only {@code strata:flat} resolves against it.
     */
    ThemeSource NONE = new ThemeSource() {
        @Override
        public Optional<Theme> base(final ThemeId id) {
            return Optional.empty();
        }

        @Override
        public List<ThemeDocument> documents(final ThemeId id) {
            return List.of();
        }

        @Override
        public long version() {
            return 0;
        }
    };

    /**
     * Returns the theme the adapter builds in code for an id.
     *
     * @param id the theme
     * @return the theme, or empty if the adapter has none under that id
     */
    Optional<Theme> base(ThemeId id);

    /**
     * Returns the theme files for an id, lowest priority first.
     *
     * @param id the theme
     * @return the files; empty if there are none
     */
    List<ThemeDocument> documents(ThemeId id);

    /**
     * Returns a number that changes whenever {@link #base} or {@link #documents} may return something new.
     *
     * @return the current version
     */
    long version();
}
