/*
 * Copyright (c) InspireNXE <https://github.com/InspireNXE>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.Themes;

import java.util.List;
import java.util.Optional;

/**
 * Where the engine keeps themes: the themes the adapter builds in code or ships as base files, and the theme files
 * the engine's packs provide. Core resolves a {@link org.inspirenxe.strata.api.theme.ThemeRef} against it and owns
 * the file format, the parent chain, the layers and the merge. The source only finds things.
 *
 * <p>Core builds a theme from layers, lowest first: the base, the documents in the order {@link #documents}
 * returns them, the overrides added in code with {@link org.inspirenxe.strata.api.theme.ThemeRegistry}, and the
 * ref's own overrides. The order of the documents is the adapter's to choose, so it decides where the user's packs
 * and the files that other libraries ship fall.</p>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #base} returns themes built in code, such as an engine theme drawn with the engine's own widget
 *     images. Core registers {@code strata:flat} itself, so a source need not return it.</li>
 *     <li>{@link #bases} returns the files that make up a theme's base, for an engine theme written as JSON. An id
 *     must not have both a base theme and base files; core throws
 *     {@link org.inspirenxe.strata.api.theme.ThemeException} when it resolves one that has.</li>
 *     <li>{@link #documents} returns every theme file for an id, lowest priority first. Later documents win per
 *     key. Each document names its layer, usually the id of the pack it came from.</li>
 *     <li>{@link #version()} changes whenever anything the other methods return may have changed, such as after
 *     the engine reloads its assets. Hosts compare it on every layout and render, so it must be cheap.</li>
 *     <li>Calls come from the UI thread. {@link #version()} may be changed from another thread, so store it in a
 *     volatile or atomic field.</li>
 *     <li>Never throw for a missing id: return empty. A theme file that cannot be read is left out.</li>
 * </ul>
 */
public interface ThemeSource {
    /**
     * A source with no themes of its own and no files. Only {@code strata:flat} and themes defined in code resolve
     * against it.
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
     * Returns the files that make up the theme's base, applied in order before theme files and overrides. Core
     * starts from the theme the last of them to name a {@code parent} names, or from nothing if none does, then
     * applies each of them per key. The files from {@link #documents} apply on top, and their {@code parent} is
     * ignored, so a pack can change keys but never swap the whole base out.
     *
     * <p>Base files ship with the adapter, so a base file that cannot be parsed is a bug: core throws
     * {@link org.inspirenxe.strata.api.theme.ThemeException} naming its origin, where it skips a broken theme file
     * from {@link #documents} and logs it.</p>
     *
     * @param id the theme
     * @return the base files, lowest priority first; empty by default
     */
    default List<ThemeDocument> bases(final ThemeId id) {
        return List.of();
    }

    /**
     * Returns the theme files for an id, lowest priority first.
     *
     * @param id the theme
     * @return the files; empty if there are none
     */
    List<ThemeDocument> documents(ThemeId id);

    /**
     * Returns the platform's default theme: the theme screens open in when no theme is given, which
     * {@link Themes#PLATFORM} and {@code ThemeRef.platform()} stand for. Core reads it each time it resolves a ref
     * to {@link Themes#PLATFORM}, so a change shows on open screens once {@link #version()} changes too.
     *
     * <p>It must name a theme that resolves against this source, and must not be {@link Themes#PLATFORM} itself,
     * which core refuses with {@link org.inspirenxe.strata.api.theme.ThemeException}.</p>
     *
     * @return the default theme's id; {@link Themes#FLAT} by default
     */
    default ThemeId defaultTheme() {
        return Themes.FLAT;
    }

    /**
     * Returns a number that changes whenever {@link #base}, {@link #bases}, {@link #documents} or
     * {@link #defaultTheme()} may return something new.
     *
     * @return the current version
     */
    long version();
}
