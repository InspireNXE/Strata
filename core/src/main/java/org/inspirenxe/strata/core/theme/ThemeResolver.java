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
 * Turns a {@link ThemeRef} into a {@link Theme} using a {@link ThemeSource}.
 *
 * <p>For each id the resolver starts from a base: {@link Themes#flat()} for {@link Themes#FLAT}, otherwise the
 * source's {@link ThemeSource#base base theme}. An id with no base starts from the theme named as {@code parent}
 * by its files (the last file that names one wins), and from nothing if no file names one. A file's
 * {@code parent} is ignored when the id has a base. The source's files are then applied in order, each one per
 * key, and the ref's overrides last. The result is named after the ref's id.</p>
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
 * border, in UI units. Unknown members are errors.</p>
 *
 * <p>A file that cannot be parsed is skipped and logged, so one broken pack does not hide the others. The
 * resolver reads the source on every call and keeps no cache.</p>
 */
public final class ThemeResolver {
    private static final System.Logger LOGGER = System.getLogger(ThemeResolver.class.getName());

    private final ThemeSource source;

    /**
     * Creates a resolver.
     *
     * @param source where to find base themes and theme files
     */
    public ThemeResolver(final ThemeSource source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    /**
     * Resolves a ref.
     *
     * @param ref the ref
     * @return the theme
     * @throws ThemeException if an id in the chain has no base and no files, or the parents form a cycle
     */
    public Theme resolve(final ThemeRef ref) {
        final Layers layers = this.resolve(ref.id(), new LinkedHashSet<>());
        layers.paints.putAll(ref.paints());
        layers.text.putAll(ref.textStyles());
        layers.insets.putAll(ref.insets());
        final Theme.Builder builder = Theme.builder(ref.id().toString());
        layers.paints.forEach(builder::paint);
        layers.text.forEach(builder::textStyle);
        layers.insets.forEach(builder::insets);
        return builder.build();
    }

    private Layers resolve(final ThemeId id, final Set<ThemeId> visiting) {
        if (!visiting.add(id)) {
            final List<String> chain = new ArrayList<>();
            visiting.forEach(seen -> chain.add(seen.toString()));
            chain.add(id.toString());
            throw new ThemeException("Theme parents form a cycle: " + String.join(" -> ", chain));
        }
        final List<ThemeFile> files = this.files(id);
        final Optional<Theme> base = id.equals(Themes.FLAT) ? Optional.of(Themes.flat()) : this.source.base(id);
        final Layers layers;
        if (base.isPresent()) {
            layers = new Layers(new HashMap<>(base.get().paints()), new HashMap<>(base.get().textStyles()),
                    new HashMap<>(base.get().insets()));
        } else {
            Optional<ThemeId> parent = Optional.empty();
            for (final ThemeFile file : files) {
                if (file.parent().isPresent()) {
                    parent = file.parent();
                }
            }
            if (parent.isPresent()) {
                layers = this.resolve(parent.get(), visiting);
            } else if (files.isEmpty()) {
                throw new ThemeException("No theme is registered as " + id + " and no theme file defines it");
            } else {
                layers = new Layers(new HashMap<>(), new HashMap<>(), new HashMap<>());
            }
        }
        for (final ThemeFile file : files) {
            layers.paints.putAll(file.paints());
            layers.insets.putAll(file.insets());
            file.text().forEach((key, patch) ->
                    layers.text.put(key, patch.apply(layers.text.getOrDefault(key, TextStyle.DEFAULT))));
        }
        visiting.remove(id);
        return layers;
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
     * The keys built up so far.
     */
    private record Layers(Map<String, Paint> paints, Map<String, TextStyle> text, Map<String, Insets> insets) {
    }
}
