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

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * One payload in one or more formats, such as rows copied as text for a spreadsheet and as objects for a paste
 * inside the application. The clipboard and drag and drop both carry one.
 *
 * <pre>{@code
 * Transfer rows = Transfer.builder()
 *         .add(DataFormat.TEXT, "Ana\t12\nBen\t7")
 *         .add(DataFormat.mime("text/csv"), "Ana,12\nBen,7")
 *         .add(LISTINGS, List.copyOf(selected))          // a DataFormat.local
 *         .build();
 * }</pre>
 *
 * <p>A transfer is immutable and equal only to itself. A value may be {@linkplain Builder#supply supplied} when it
 * is first read instead of when the transfer is built, as an adapter does for a drag from another application whose
 * data can be read only once it is dropped: such a value reads as empty from {@link #get} until it can be read, while
 * {@link #has} already answers for its format.</p>
 */
public final class Transfer {
    private static final Transfer EMPTY = new Transfer(Map.of());

    // Each value is the value itself or a Lazy
    private final Map<DataFormat<?>, Object> values;

    private Transfer(final Map<DataFormat<?>, Object> values) {
        this.values = values;
    }

    /**
     * Returns a transfer with no formats.
     *
     * @return the empty transfer
     */
    public static Transfer empty() {
        return EMPTY;
    }

    /**
     * Starts a transfer of several formats.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Makes a transfer of plain text.
     *
     * @param text the text
     * @return a transfer holding {@link DataFormat#TEXT}
     */
    public static Transfer text(final String text) {
        return Transfer.builder().add(DataFormat.TEXT, text).build();
    }

    /**
     * Makes a transfer of files.
     *
     * @param files the files; the transfer keeps a copy
     * @return a transfer holding {@link DataFormat#FILES}
     * @throws IllegalArgumentException if the list is empty
     */
    public static Transfer files(final List<Path> files) {
        return Transfer.builder().add(DataFormat.FILES, files).build();
    }

    /**
     * Reads the value of a format.
     *
     * @param <T> the type of the value
     * @param format the format
     * @return the value, or empty if the transfer does not hold the format, or holds a supplied value that cannot be
     *     read now
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(final DataFormat<T> format) {
        Objects.requireNonNull(format, "format");
        final Object value = this.values.get(format);
        if (value instanceof final Lazy<?> lazy) {
            return (Optional<T>) lazy.get(format);
        }
        return Optional.ofNullable((T) value);
    }

    /**
     * Returns whether the transfer holds a format.
     *
     * @param format the format
     * @return true if it does, whether or not a supplied value can be read yet
     */
    public boolean has(final DataFormat<?> format) {
        return this.values.containsKey(Objects.requireNonNull(format, "format"));
    }

    /**
     * Returns the formats the transfer holds, in the order they were added.
     *
     * @return an unmodifiable set, possibly empty
     */
    public Set<DataFormat<?>> formats() {
        return Collections.unmodifiableSet(this.values.keySet());
    }

    /**
     * Returns whether the transfer holds no format.
     *
     * @return true for an empty transfer
     */
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    /**
     * Returns a transfer with only the formats that pass a test, such as the formats that may leave the process.
     *
     * @param keep the test
     * @return this transfer if every format passes, else a new one with those that do
     */
    public Transfer only(final Predicate<? super DataFormat<?>> keep) {
        Objects.requireNonNull(keep, "keep");
        final Map<DataFormat<?>, Object> kept = new LinkedHashMap<>();
        for (final Map.Entry<DataFormat<?>, Object> entry : this.values.entrySet()) {
            if (keep.test(entry.getKey())) {
                kept.put(entry.getKey(), entry.getValue());
            }
        }
        if (kept.size() == this.values.size()) {
            return this;
        }
        return kept.isEmpty() ? EMPTY : new Transfer(Collections.unmodifiableMap(kept));
    }

    /**
     * Returns the formats, for debugging.
     *
     * @return for example {@code Transfer[text/plain, text/csv]}
     */
    @Override
    public String toString() {
        final StringBuilder out = new StringBuilder("Transfer[");
        boolean first = true;
        for (final DataFormat<?> format : this.values.keySet()) {
            if (!first) {
                out.append(", ");
            }
            out.append(format.name());
            first = false;
        }
        return out.append(']').toString();
    }

    private static Object check(final DataFormat<?> format, final Object value) {
        if (!format.type().isInstance(value)) {
            throw new IllegalArgumentException(format + " holds a " + format.type().getSimpleName() + ", not "
                    + value.getClass().getName());
        }
        if (format == DataFormat.FILES) {
            final List<?> files = List.copyOf((List<?>) value);
            if (files.isEmpty()) {
                throw new IllegalArgumentException("A list of files must not be empty");
            }
            for (final Object file : files) {
                if (!(file instanceof Path)) {
                    throw new IllegalArgumentException("Files must be paths, not " + file.getClass().getName());
                }
            }
            return files;
        }
        return value;
    }

    /**
     * A value read when it is first asked for, and kept once it was read.
     */
    private static final class Lazy<T> {
        private final Supplier<? extends T> supplier;
        private Object value;
        private boolean read;

        Lazy(final Supplier<? extends T> supplier) {
            this.supplier = supplier;
        }

        synchronized Optional<?> get(final DataFormat<?> format) {
            if (!this.read) {
                final Object next;
                try {
                    next = this.supplier.get();
                } catch (final RuntimeException e) {
                    return Optional.empty();
                }
                if (next == null) {
                    return Optional.empty();
                }
                try {
                    this.value = Transfer.check(format, next);
                } catch (final IllegalArgumentException e) {
                    return Optional.empty();
                }
                this.read = true;
            }
            return Optional.of(this.value);
        }
    }

    /**
     * Builds a {@link Transfer}. Adding a format again replaces its value and keeps its place.
     */
    public static final class Builder {
        private final Map<DataFormat<?>, Object> values = new LinkedHashMap<>();

        private Builder() {
        }

        /**
         * Adds a value.
         *
         * @param <T> the type of the value
         * @param format the format
         * @param value the value; a list of files is copied
         * @return this builder
         * @throws IllegalArgumentException if the value is not of the format's type, or is an empty list of files or
         *     one that holds something other than paths
         */
        public <T> Builder add(final DataFormat<T> format, final T value) {
            Objects.requireNonNull(format, "format");
            Objects.requireNonNull(value, "value");
            this.values.put(format, Transfer.check(format, value));
            return this;
        }

        /**
         * Adds a value read the first time {@link Transfer#get} asks for it. Until the supplier gives a value,
         * {@code get} reads as empty and asks again next time: when the supplier returns null, throws, or returns a
         * value that is not of the format's type. Once it gave one, the transfer keeps it. The supplier may be called
         * on the UI thread from inside an event, so it must not block.
         *
         * @param <T> the type of the value
         * @param format the format
         * @param supplier gives the value, or null while it cannot be read
         * @return this builder
         */
        public <T> Builder supply(final DataFormat<T> format, final Supplier<? extends T> supplier) {
            Objects.requireNonNull(format, "format");
            this.values.put(format, new Lazy<>(Objects.requireNonNull(supplier, "supplier")));
            return this;
        }

        /**
         * Builds the transfer. The builder can go on and build others without changing this one.
         *
         * @return the transfer, or {@link Transfer#empty()} if nothing was added
         */
        public Transfer build() {
            return this.values.isEmpty() ? EMPTY : new Transfer(Collections.unmodifiableMap(new LinkedHashMap<>(this.values)));
        }
    }
}
