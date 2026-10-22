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
package org.inspirenxe.strata.api.file;

import org.inspirenxe.strata.api.text.Text;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * What a file dialog asks: its title, where it starts, the name it suggests and the filters it offers.
 *
 * <pre>{@code
 * ui.fileDialogs().save(FileDialog.of(Text.of("Export orders"))
 *                 .withSuggestedName("orders.csv")
 *                 .withFilter(Text.of("Spreadsheets"), "csv", "xlsx"))
 *         .thenAccept(path -> path.ifPresent(orders::exportTo));
 * }</pre>
 *
 * @param title the dialog's title
 * @param start the folder or file the dialog starts at, or {@code null} for the engine's choice
 * @param suggestedName the file name a save dialog suggests, or empty for none; other dialogs may ignore it
 * @param filters the filters offered, in order, the first chosen at the start; empty to show every file
 */
public record FileDialog(Text title, Path start, String suggestedName, List<Filter> filters) {
    /**
     * Checks the values and copies the filters.
     *
     * @param title the title
     * @param start where it starts, or {@code null}
     * @param suggestedName the suggested name, or empty
     * @param filters the filters
     */
    public FileDialog {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(suggestedName, "suggestedName");
        filters = List.copyOf(filters);
    }

    /**
     * Makes a dialog with a title, starting where the engine chooses, with no suggested name and no filters.
     *
     * @param title the title
     * @return the dialog
     */
    public static FileDialog of(final Text title) {
        return new FileDialog(title, null, "", List.of());
    }

    /**
     * Returns this dialog starting at a folder or file.
     *
     * @param start where it starts
     * @return a new dialog
     */
    public FileDialog withStart(final Path start) {
        return new FileDialog(this.title, Objects.requireNonNull(start, "start"), this.suggestedName, this.filters);
    }

    /**
     * Returns this dialog suggesting a file name, for a save dialog.
     *
     * @param name the name, such as {@code orders.csv}
     * @return a new dialog
     */
    public FileDialog withSuggestedName(final String name) {
        return new FileDialog(this.title, this.start, Objects.requireNonNull(name, "name"), this.filters);
    }

    /**
     * Returns this dialog with one more filter, after the others.
     *
     * @param name what the filter is called, such as "Spreadsheets"
     * @param extensions the extensions it shows, such as {@code csv} and {@code xlsx}
     * @return a new dialog
     * @throws IllegalArgumentException as {@link Filter} does
     */
    public FileDialog withFilter(final Text name, final String... extensions) {
        final List<Filter> next = new ArrayList<>(this.filters);
        next.add(new Filter(name, List.of(extensions)));
        return new FileDialog(this.title, this.start, this.suggestedName, next);
    }

    /**
     * A filter a dialog offers: the files with one of a list of extensions.
     *
     * @param name what the filter is called
     * @param extensions the extensions without their dot, in lower case once made; never empty
     */
    public record Filter(Text name, List<String> extensions) {
        /**
         * Checks the extensions and keeps them in lower case.
         *
         * @param name what the filter is called
         * @param extensions the extensions
         * @throws IllegalArgumentException if there is no extension, or one is empty or holds a dot, a slash, a
         *     backslash or whitespace
         */
        public Filter {
            Objects.requireNonNull(name, "name");
            final List<String> checked = new ArrayList<>(extensions.size());
            for (final String extension : extensions) {
                Objects.requireNonNull(extension, "extension");
                if (extension.isEmpty() || extension.chars().anyMatch(c -> c == '.' || c == '/' || c == '\\'
                        || Character.isWhitespace(c))) {
                    throw new IllegalArgumentException("Not a file extension: \"" + extension + "\"");
                }
                checked.add(extension.toLowerCase(Locale.ROOT));
            }
            if (checked.isEmpty()) {
                throw new IllegalArgumentException("A filter needs at least one extension");
            }
            extensions = List.copyOf(checked);
        }

        /**
         * Returns whether a file's name ends with one of the extensions, ignoring case.
         *
         * @param file the file
         * @return true if the filter shows it
         */
        public boolean accepts(final Path file) {
            final Path name = file.getFileName();
            if (name == null) {
                return false;
            }
            final String lower = name.toString().toLowerCase(Locale.ROOT);
            for (final String extension : this.extensions) {
                if (lower.endsWith("." + extension)) {
                    return true;
                }
            }
            return false;
        }
    }
}
