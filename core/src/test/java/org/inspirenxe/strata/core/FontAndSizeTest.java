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
package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.text.FontId;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.text.TextStyle.Property;
import org.inspirenxe.strata.api.text.TextStylePatch;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.core.ThemeResolverTest.MapSource;
import org.inspirenxe.strata.core.theme.ThemeRegistryImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Text styles name a font family and a size, themes and patches set them, and screens lay out again when the
 * engine's fonts change.
 */
class FontAndSizeTest {
    private static final FontId UBUNTU = FontId.of("app", "ubuntu");
    private static final ThemeId THEME = ThemeId.of("app", "fonts");

    @Test
    void aFontIdReadsBackAsWritten() {
        assertEquals(UBUNTU, FontId.parse("app:ubuntu"));
        assertEquals("app:ubuntu", UBUNTU.toString());
        assertThrows(IllegalArgumentException.class, () -> FontId.parse("ubuntu"), "no namespace");
        assertThrows(IllegalArgumentException.class, () -> FontId.of("a:b", "c"), "a colon in the namespace");
    }

    @Test
    void theDefaultStyleUsesThePlatformsFontAndSize() {
        assertNull(TextStyle.DEFAULT.font());
        assertEquals(0f, TextStyle.DEFAULT.size());
    }

    @Test
    void copiesKeepTheFontAndSize() {
        final TextStyle style = TextStyle.DEFAULT.withFont(UBUNTU).withSize(15f);
        final TextStyle copy = style.withBold(true).withColor(0xFF00FF00).withMonospace(true).withShadow(true)
                .withItalic(true).withUnderline(true).withStrikethrough(true);
        assertEquals(UBUNTU, copy.font());
        assertEquals(15f, copy.size());
        assertNull(style.withFont(null).font(), "null goes back to the platform's font");
    }

    @Test
    void aSizeMustBeFiniteAndNotNegative() {
        assertThrows(IllegalArgumentException.class, () -> TextStyle.DEFAULT.withSize(-1f));
        assertThrows(IllegalArgumentException.class, () -> TextStyle.DEFAULT.withSize(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> TextStyle.DEFAULT.withSize(Float.POSITIVE_INFINITY));
    }

    @Test
    void mergesAndPatchesTakeTheFontAndSize() {
        final TextStyle sized = TextStyle.DEFAULT.withFont(UBUNTU).withSize(15f);
        assertEquals(TextStyle.DEFAULT.withSize(15f), TextStyle.DEFAULT.merge(sized, Property.SIZE));
        assertEquals(TextStyle.DEFAULT.withFont(UBUNTU), TextStyle.DEFAULT.merge(sized, Property.FONT));
        assertEquals(sized, TextStyle.DEFAULT.merge(sized, EnumSet.allOf(Property.class)));

        final TextStylePatch patch = TextStylePatch.of().font(UBUNTU).size(15f);
        assertEquals(sized.withBold(true), patch.apply(TextStyle.DEFAULT.withBold(true)));
        assertEquals(TextStyle.DEFAULT, TextStylePatch.of().font(null).size(0f).apply(sized), "a patch can undo both");
    }

    @Test
    void themeFilesSetTheFontAndSize() {
        final Theme theme = resolve("""
                { "parent": "strata:flat", "text": {
                    "title": { "font": "app:ubuntu", "size": 15 },
                    "plain": { "font": null, "size": 0 } } }""");
        assertEquals(TextStyle.DEFAULT.withFont(UBUNTU).withSize(15f), theme.textStyle("title").orElseThrow());
        assertEquals(TextStyle.DEFAULT, theme.textStyle("plain").orElseThrow());
    }

    @Test
    void aThemeFileWithABadFontOrSizeIsSkipped() {
        // A broken file is left out, as for any other error, so the key keeps what the file before it set
        final String good = """
                { "parent": "strata:flat", "text": { "title": { "size": 3 } } }""";
        final Theme badFont = resolve(good, """
                { "text": { "title": { "font": "ubuntu" } } }""");
        assertEquals(TextStyle.DEFAULT.withSize(3f), badFont.textStyle("title").orElseThrow(), "no namespace");
        final Theme badSize = resolve(good, """
                { "text": { "title": { "size": -2 } } }""");
        assertEquals(TextStyle.DEFAULT.withSize(3f), badSize.textStyle("title").orElseThrow(), "a negative size");
    }

    @Test
    void aLargerSizeMeasuresLarger() {
        final TestHost host = new TestHost(400, 200);
        final Label plain = host.factory().label("Strata");
        final Label large = host.factory().label("Strata");
        host.show(host.factory().stackPanel()
                .add(plain)
                .add(host.factory().stackPanel().setTextStyle(TextStylePatch.of().size(TestPlatform.TEXT_SIZE * 2))
                        .add(large)));
        assertEquals(plain.bounds().width() * 2, large.bounds().width(), 0.01f);
        assertEquals(plain.bounds().height() * 2, large.bounds().height(), 0.01f);
    }

    @Test
    void screensLayOutAgainWhenTheEnginesFontsChange() {
        final TestHost host = new TestHost(400, 200);
        final Label label = host.factory().label("Strata");
        host.show(host.factory().stackPanel().add(label));
        final float before = label.bounds().width();

        host.platform().setMetricsScale(2f);
        host.host().layout();
        assertEquals(before, label.bounds().width(), "the same version keeps the measured layout");

        host.platform().changeMetrics();
        host.host().layout();
        assertEquals(before * 2, label.bounds().width(), 0.01f);
    }

    private static Theme resolve(final String... files) {
        final MapSource source = new MapSource();
        for (final String file : files) {
            source.file(THEME, file);
        }
        return new ThemeResolver(source, new ThemeRegistryImpl()).resolve(ThemeRef.of(THEME));
    }
}
