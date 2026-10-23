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
package org.inspirenxe.strata.api.transfer;

import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.visual.PixelBuffer;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A kind of data a {@link Transfer} can hold, with the type of its value.
 *
 * <p>Formats are compared by identity, as a {@code CauseKey} is: each call to {@link #local} makes a format equal
 * only to itself, so keep each in a constant. {@link #mime} is the exception: it returns the same format for the same
 * MIME type, ignoring case, so two libraries that name {@code text/csv} share it, and {@code mime("text/plain")} is
 * {@link #TEXT}.</p>
 *
 * <p>Every format but a local one may leave the process: an adapter puts it on the system clipboard or offers it to
 * another application, if its engine can. A {@linkplain #local local} format never reaches the engine.</p>
 *
 * @param <T> the type of the value
 */
public final class DataFormat<T> {
    private static final Map<String, DataFormat<String>> MIME = new ConcurrentHashMap<>();

    /**
     * Plain text, the MIME type {@code text/plain}. Every clipboard holds it.
     */
    public static final DataFormat<String> TEXT = DataFormat.mime("text/plain");
    /**
     * Text with its styles, which Strata's own fields and labels read. An engine that cannot hold it keeps
     * {@link #TEXT} only.
     */
    public static final DataFormat<Text> STYLED_TEXT = new DataFormat<>("strata:styled_text", Text.class, null, false);
    /**
     * A list of files, such as files dragged from the desktop. The list is never empty and holds no null.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final DataFormat<List<Path>> FILES = new DataFormat<>("strata:files", (Class) List.class, null,
            false);
    /**
     * An image, as pixels.
     */
    public static final DataFormat<PixelBuffer> IMAGE = new DataFormat<>("strata:image", PixelBuffer.class, null,
            false);

    private final String name;
    private final Class<T> type;
    private final String mimeType;
    private final boolean local;

    private DataFormat(final String name, final Class<T> type, final String mimeType, final boolean local) {
        this.name = name;
        this.type = type;
        this.mimeType = mimeType;
        this.local = local;
    }

    /**
     * Returns the format for text in a MIME type, such as {@code text/csv} for rows a spreadsheet takes or
     * {@code text/html}. The value is the text itself.
     *
     * @param mimeType a MIME type, a type and a subtype joined by a slash, without parameters; case is ignored
     * @return the format, the same one for every call with the same type
     * @throws IllegalArgumentException if the text is not a type and a subtype joined by one slash, or holds
     *     whitespace or a semicolon
     */
    public static DataFormat<String> mime(final String mimeType) {
        Objects.requireNonNull(mimeType, "mimeType");
        final String lower = mimeType.toLowerCase(Locale.ROOT);
        final int slash = lower.indexOf('/');
        if (slash <= 0 || slash == lower.length() - 1 || lower.indexOf('/', slash + 1) >= 0
                || lower.chars().anyMatch(c -> Character.isWhitespace(c) || c == ';')) {
            throw new IllegalArgumentException("Not a MIME type: " + mimeType);
        }
        return MIME.computeIfAbsent(lower, key -> new DataFormat<>(key, String.class, key, false));
    }

    /**
     * Makes a format that never leaves the process, for objects that only this application reads, such as the rows
     * a table copies so that a paste inside the application gets them whole.
     *
     * @param <T> the type of the value
     * @param name a readable name, such as {@code myapp:listings}, for debugging
     * @param type the type of the value
     * @return a new format, equal only to itself
     */
    public static <T> DataFormat<T> local(final String name, final Class<T> type) {
        return new DataFormat<>(Objects.requireNonNull(name, "name"), Objects.requireNonNull(type, "type"), null,
                true);
    }

    /**
     * Returns the format's name: the MIME type for a MIME format, the name given to {@link #local}, or a
     * {@code strata:} name for the other built-in formats.
     *
     * @return the name
     */
    public String name() {
        return this.name;
    }

    /**
     * Returns the type of the format's value. For {@link #FILES} it is {@code List}.
     *
     * @return the type
     */
    public Class<T> type() {
        return this.type;
    }

    /**
     * Returns the MIME type of a text format.
     *
     * @return the MIME type for {@link #TEXT} and the formats {@link #mime} makes, or empty for the others
     */
    public Optional<String> mimeType() {
        return Optional.ofNullable(this.mimeType);
    }

    /**
     * Returns whether the format never leaves the process.
     *
     * @return true for a format {@link #local} made
     */
    public boolean isLocal() {
        return this.local;
    }

    /**
     * Returns the format's name, for debugging.
     *
     * @return for example {@code DataFormat[text/plain]}
     */
    @Override
    public String toString() {
        return "DataFormat[" + this.name + "]";
    }
}
