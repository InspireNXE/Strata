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
package org.inspirenxe.strata.core.theme;

import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeOverrides;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Turns a {@link ThemeRef} into a {@link Theme} using a {@link ThemeSource} and the overrides in a
 * {@link ThemeRegistryImpl}.
 *
 * <p>A theme is built from layers, lowest first, each applied per key over the ones below it:</p>
 * <ol>
 *     <li>The base. {@link Themes#FLAT} starts from {@link Themes#flat()}. Any other id starts from the source's
 *     {@link ThemeSource#base base theme}, or from its {@link ThemeSource#bases base files}, which start from the
 *     theme the last of them to name a {@code parent} names, or from the theme defined in code with
 *     {@link ThemeRegistryImpl#define}, which starts from its parent. An id with none of these starts from the
 *     theme its theme files name as {@code parent} (the last file that names one wins), and from nothing if no file
 *     names one. A parent is resolved in full, its own layers included.</li>
 *     <li>The source's {@link ThemeSource#documents theme files}, in the order the source returns them. Their
 *     {@code parent} is ignored whenever the id has a base.</li>
 *     <li>The overrides added for the id with {@link ThemeRegistryImpl#override}, in the order they were added.</li>
 *     <li>The ref's own overrides.</li>
 * </ol>
 * <p>The result is named after the ref's id, and records for each key the layer that set it last:
 * {@value #BASE_LAYER} for a base theme built in code, a theme file's {@link ThemeDocument#layer}, base files
 * included, {@value #CODE_LAYER} for overrides and definitions from the registry, and {@value #REF_LAYER} for
 * the ref's overrides.</p>
 *
 * <p>A theme file is a JSON object with four optional members:</p>
 * <pre>{@code
 * {
 *   "parent": "strata:flat",
 *   "paints": {
 *     "button.normal": { "nine_slice": "mymod:button", "border": [3, 3, 3, 3], "tint": "#FFFFFFFF" },
 *     "button.hovered": { "sprite": "mymod:button_hover" },
 *     "list.row": "#FF1F2024",
 *     "window.background": { "gradient": ["#F0101010", "#F0202020"] },
 *     "button.focus": "none"
 *   },
 *   "text": {
 *     "button.text": { "color": "#FFFFFF", "shadow": true }
 *   },
 *   "insets": {
 *     "tab.padding": [8, 5, 8, 2]
 *   }
 * }
 * }</pre>
 * <p>Colors are {@code #RRGGBB}, which is opaque, or {@code #AARRGGBB}. A paint is a color, {@code "none"} for
 * {@link Paint#NONE}, or an object holding exactly one of {@code sprite}, {@code nine_slice} or {@code gradient}.
 * {@code tint} is optional and defaults to opaque white. A nine-slice {@code border} is one number,
 * {@code [horizontal, vertical]} or {@code [left, top, right, bottom]}. A text style sets any of {@code color},
 * {@code bold}, {@code italic}, {@code underline}, {@code strikethrough} and {@code shadow}; the rest keep the
 * inherited style for that key, or {@link TextStyle#DEFAULT} if there is none. Insets take the same forms as a
 * border, in UI units. Unknown members are errors. Overrides from code and from the ref replace a key's whole text
 * style.</p>
 *
 * <p>A theme file from {@link ThemeSource#documents} that cannot be parsed is skipped and logged, so one broken
 * pack does not hide the others. A base file that cannot be parsed throws, since it ships with the adapter. The
 * resolver reads the source and the registry on every call and keeps no cache.</p>
 */
public final class ThemeResolver {
    /**
     * The layer name of a base theme built in code, such as {@link Themes#flat()}.
     */
    public static final String BASE_LAYER = "base";
    /**
     * The layer name of overrides and definitions added to the registry.
     */
    public static final String CODE_LAYER = "code";
    /**
     * The layer name of a ref's own overrides.
     */
    public static final String REF_LAYER = "ref";

    private static final System.Logger LOGGER = System.getLogger(ThemeResolver.class.getName());

    private final ThemeSource source;
    private final ThemeRegistryImpl registry;

    /**
     * Creates a resolver that reads the process's registry, {@link ThemeRegistryImpl#INSTANCE}.
     *
     * @param source where to find base themes and theme files
     */
    public ThemeResolver(final ThemeSource source) {
        this(source, ThemeRegistryImpl.INSTANCE);
    }

    /**
     * Creates a resolver that reads a given registry.
     *
     * @param source where to find base themes and theme files
     * @param registry where to find the themes and overrides added in code
     */
    public ThemeResolver(final ThemeSource source, final ThemeRegistryImpl registry) {
        this.source = Objects.requireNonNull(source, "source");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    /**
     * Resolves a ref.
     *
     * @param ref the ref
     * @return the theme
     * @throws ThemeException if an id in the chain has no base and no files, the parents form a cycle, an id has a
     *     base theme and base files both, an id defined in code also has a base from the source, or a base file
     *     cannot be parsed
     */
    public Theme resolve(final ThemeRef ref) {
        final Keys keys = this.resolve(ref.id(), new LinkedHashSet<>());
        keys.set(new ThemeOverrides(ref.paints(), ref.textStyles(), ref.insets()), REF_LAYER);
        final Theme.Builder builder = Theme.builder(ref.id().toString());
        keys.paints.forEach(builder::paint);
        keys.text.forEach(builder::textStyle);
        keys.insets.forEach(builder::insets);
        keys.layers.forEach(builder::layer);
        return builder.build();
    }

    private Keys resolve(final ThemeId id, final Set<ThemeId> visiting) {
        if (!visiting.add(id)) {
            final List<String> chain = new ArrayList<>();
            visiting.forEach(seen -> chain.add(seen.toString()));
            chain.add(id.toString());
            throw new ThemeException("Theme parents form a cycle: " + String.join(" -> ", chain));
        }
        final Optional<Theme> code = id.equals(Themes.FLAT) ? Optional.of(Themes.flat()) : this.source.base(id);
        final List<ThemeDocument> baseDocuments = this.source.bases(id);
        final Optional<ThemeRegistryImpl.Definition> definition = this.registry.definition(id);
        if (code.isPresent() && !baseDocuments.isEmpty()) {
            throw new ThemeException("Theme " + id + " has both a base theme built in code and base files");
        }
        if (definition.isPresent() && (code.isPresent() || !baseDocuments.isEmpty())) {
            throw new ThemeException("Theme " + id + " is defined in code but already has a base from the engine"
                    + " adapter");
        }
        final List<ThemeFile> files = this.files(id);
        final Keys keys;
        if (code.isPresent()) {
            keys = new Keys();
            keys.set(new ThemeOverrides(code.get().paints(), code.get().textStyles(), code.get().insets()),
                    BASE_LAYER);
        } else if (!baseDocuments.isEmpty()) {
            final List<ThemeFile> bases = new ArrayList<>();
            for (final ThemeDocument document : baseDocuments) {
                // A base file ships with the adapter, so a mistake in one is thrown rather than skipped
                bases.add(ThemeFile.parse(document));
            }
            keys = this.parentOf(bases, visiting).orElseGet(Keys::new);
            bases.forEach(keys::apply);
        } else if (definition.isPresent()) {
            keys = this.resolve(definition.get().parent(), visiting);
            keys.set(definition.get().overrides(), CODE_LAYER);
        } else {
            final Optional<Keys> parent = this.parentOf(files, visiting);
            if (parent.isPresent()) {
                keys = parent.get();
            } else if (files.isEmpty()) {
                throw new ThemeException("No theme is registered or defined as " + id
                        + " and no theme file defines it");
            } else {
                keys = new Keys();
            }
        }
        files.forEach(keys::apply);
        for (final ThemeOverrides overrides : this.registry.overrides(id)) {
            keys.set(overrides, CODE_LAYER);
        }
        visiting.remove(id);
        return keys;
    }

    /**
     * Resolves the parent the last of some files names, if any names one.
     */
    private Optional<Keys> parentOf(final List<ThemeFile> files, final Set<ThemeId> visiting) {
        Optional<ThemeId> parent = Optional.empty();
        for (final ThemeFile file : files) {
            if (file.parent().isPresent()) {
                parent = file.parent();
            }
        }
        return parent.map(id -> this.resolve(id, visiting));
    }

    private List<ThemeFile> files(final ThemeId id) {
        final List<ThemeFile> files = new ArrayList<>();
        for (final ThemeDocument document : this.source.documents(id)) {
            try {
                files.add(ThemeFile.parse(document));
            } catch (final ThemeException e) {
                LOGGER.log(System.Logger.Level.WARNING, e.getMessage() + "; skipping it");
            }
        }
        return files;
    }

    /**
     * The keys built up so far, with the layer that set each one last.
     */
    private static final class Keys {
        private final Map<String, Paint> paints = new HashMap<>();
        private final Map<String, TextStyle> text = new HashMap<>();
        private final Map<String, Insets> insets = new HashMap<>();
        private final Map<String, String> layers = new HashMap<>();

        /**
         * Applies a theme file: paints and insets replace, text entries change only the fields they set.
         */
        private void apply(final ThemeFile file) {
            file.paints().forEach((key, paint) -> {
                this.paints.put(key, paint);
                this.layers.put(key, file.layer());
            });
            file.insets().forEach((key, value) -> {
                this.insets.put(key, value);
                this.layers.put(key, file.layer());
            });
            file.text().forEach((key, patch) -> {
                this.text.put(key, patch.apply(this.text.getOrDefault(key, TextStyle.DEFAULT)));
                this.layers.put(key, file.layer());
            });
        }

        /**
         * Sets whole values, as overrides and base themes built in code do.
         */
        private void set(final ThemeOverrides overrides, final String layer) {
            overrides.paints().forEach((key, paint) -> {
                this.paints.put(key, paint);
                this.layers.put(key, layer);
            });
            overrides.textStyles().forEach((key, style) -> {
                this.text.put(key, style);
                this.layers.put(key, layer);
            });
            overrides.insets().forEach((key, value) -> {
                this.insets.put(key, value);
                this.layers.put(key, layer);
            });
        }
    }
}
