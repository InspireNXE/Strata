package org.inspirenxe.strata.core;

import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.ThemeId;
import org.inspirenxe.strata.api.theme.ThemeRef;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.host.ScreenHostImpl;
import org.inspirenxe.strata.core.theme.ThemeResolver;
import org.inspirenxe.strata.spi.ThemeDocument;
import org.inspirenxe.strata.spi.ThemeSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Theme refs resolved against a source: bases, files, parents, overrides, the file format, and host reloads.
 */
class ThemeResolverTest {
    private static final ThemeId ENGINE = ThemeId.of("engine", "native");
    private static final ThemeId MOD = ThemeId.of("mod", "shop");

    private static Theme resolve(final MapSource source, final ThemeRef ref) {
        return new ThemeResolver(source).resolve(ref);
    }

    @Test
    void flatResolvesWithoutASource() {
        final Theme theme = new ThemeResolver(ThemeSource.NONE).resolve(ThemeRef.of(Themes.FLAT));
        assertEquals("strata:flat", theme.name());
        assertEquals(Themes.flat().paints(), theme.paints());
        assertEquals(Themes.flat().textStyles(), theme.textStyles());
    }

    @Test
    void filesOverrideTheBasePerKey() {
        final MapSource source = new MapSource()
                .base(ENGINE, Theme.builder("engine")
                        .paint("a", Paint.solid(0xFF000001))
                        .paint("b", Paint.solid(0xFF000002))
                        .textStyle("t", TextStyle.DEFAULT.withShadow(true).withColor(0xFF111111))
                        .build())
                .file(ENGINE, """
                        { "paints": { "b": "#000003" }, "text": { "t": { "color": "#FF222222" } } }""");
        final Theme theme = resolve(source, ThemeRef.of(ENGINE));
        assertEquals("engine:native", theme.name());
        assertEquals(Paint.solid(0xFF000001), theme.paint("a").orElseThrow());
        assertEquals(Paint.solid(0xFF000003), theme.paint("b").orElseThrow());
        assertEquals(TextStyle.DEFAULT.withShadow(true).withColor(0xFF222222), theme.textStyle("t").orElseThrow());
    }

    @Test
    void laterFilesWin() {
        final MapSource source = new MapSource()
                .file(MOD, """
                        { "parent": "strata:flat", "paints": { "a": "#000001", "b": "#000001" } }""")
                .file(MOD, """
                        { "paints": { "b": "#000002" } }""");
        final Theme theme = resolve(source, ThemeRef.of(MOD));
        assertEquals(Paint.solid(0xFF000001), theme.paint("a").orElseThrow());
        assertEquals(Paint.solid(0xFF000002), theme.paint("b").orElseThrow());
        assertEquals(Themes.flat().paint(Button.STYLE_NORMAL), theme.paint(Button.STYLE_NORMAL));
    }

    @Test
    void anIdWithoutABaseStartsFromItsParent() {
        final MapSource source = new MapSource()
                .base(ENGINE, Theme.builder("engine").paint("a", Paint.solid(0xFF000001)).build())
                .file(ENGINE, """
                        { "paints": { "b": "#000002" } }""")
                .file(MOD, """
                        { "parent": "engine:native", "paints": { "c": "#000003" } }""");
        final Theme theme = resolve(source, ThemeRef.of(MOD));
        assertEquals(Paint.solid(0xFF000001), theme.paint("a").orElseThrow());
        assertEquals(Paint.solid(0xFF000002), theme.paint("b").orElseThrow());
        assertEquals(Paint.solid(0xFF000003), theme.paint("c").orElseThrow());
    }

    @Test
    void aParentIsIgnoredWhenTheIdHasABase() {
        final MapSource source = new MapSource()
                .base(ENGINE, Theme.builder("engine").paint("a", Paint.solid(0xFF000001)).build())
                .file(ENGINE, """
                        { "parent": "strata:flat" }""");
        assertFalse(resolve(source, ThemeRef.of(ENGINE)).has(Button.STYLE_NORMAL));
    }

    @Test
    void overridesBeatFiles() {
        final MapSource source = new MapSource()
                .file(MOD, """
                        { "parent": "strata:flat", "paints": { "a": "#000001" } }""");
        final ThemeRef ref = ThemeRef.of(MOD)
                .override("a", Paint.solid(0xFF0000FF))
                .override("t", TextStyle.DEFAULT.withBold(true));
        final Theme theme = resolve(source, ref);
        assertEquals(Paint.solid(0xFF0000FF), theme.paint("a").orElseThrow());
        assertEquals(TextStyle.DEFAULT.withBold(true), theme.textStyle("t").orElseThrow());
    }

    @Test
    void anUnknownIdFails() {
        assertThrows(ThemeException.class, () -> resolve(new MapSource(), ThemeRef.of(MOD)));
    }

    @Test
    void aCycleOfParentsFails() {
        final ThemeId other = ThemeId.of("mod", "other");
        final MapSource source = new MapSource()
                .file(MOD, "{ \"parent\": \"mod:other\" }")
                .file(other, "{ \"parent\": \"mod:shop\" }");
        final ThemeException e = assertThrows(ThemeException.class, () -> resolve(source, ThemeRef.of(MOD)));
        assertTrue(e.getMessage().contains("mod:shop -> mod:other -> mod:shop"), e.getMessage());
    }

    @Test
    void aBrokenFileIsSkipped() {
        final MapSource source = new MapSource()
                .file(MOD, "{ \"parent\": \"strata:flat\", \"paints\": { \"a\": \"#000001\" } }")
                .file(MOD, "{ \"paints\": { \"a\": \"#00000Z\" } }")
                .file(MOD, "{ not json");
        assertEquals(Paint.solid(0xFF000001), resolve(source, ThemeRef.of(MOD)).paint("a").orElseThrow());
    }

    @Nested
    class Format {
        private Paint paint(final String json) {
            final MapSource source = new MapSource().file(MOD, "{ \"paints\": { \"k\": " + json + " } }");
            return resolve(source, ThemeRef.of(MOD)).paint("k").orElseThrow();
        }

        private Optional<Paint> tryPaint(final String json) {
            final MapSource source = new MapSource()
                    .file(MOD, "{ \"paints\": { \"ok\": \"#000000\" } }")
                    .file(MOD, "{ \"paints\": { \"k\": " + json + " } }");
            return resolve(source, ThemeRef.of(MOD)).paint("k");
        }

        @Test
        void colors() {
            assertEquals(Paint.solid(0xFF102030), this.paint("\"#102030\""));
            assertEquals(Paint.solid(0x80102030), this.paint("\"#80102030\""));
            assertSame(Paint.NONE, this.paint("\"none\""));
        }

        @Test
        void sprites() {
            assertEquals(new Paint.Sprite(SpriteId.of("a", "b"), 0xFFFFFFFF), this.paint("{ \"sprite\": \"a:b\" }"));
            assertEquals(new Paint.Sprite(SpriteId.of("a", "b"), 0x80FF0000),
                    this.paint("{ \"sprite\": \"a:b\", \"tint\": \"#80FF0000\" }"));
        }

        @Test
        void nineSlices() {
            final SpriteId sprite = SpriteId.of("a", "b");
            assertEquals(new Paint.NineSlice(sprite, Insets.of(3), 0xFFFFFFFF),
                    this.paint("{ \"nine_slice\": \"a:b\", \"border\": 3 }"));
            assertEquals(new Paint.NineSlice(sprite, Insets.of(2, 4), 0xFFFFFFFF),
                    this.paint("{ \"nine_slice\": \"a:b\", \"border\": [2, 4] }"));
            assertEquals(new Paint.NineSlice(sprite, new Insets(1, 2, 3, 4), 0xFFFFFFFF),
                    this.paint("{ \"nine_slice\": \"a:b\", \"border\": [1, 2, 3, 4] }"));
        }

        @Test
        void gradients() {
            assertEquals(Paint.gradient(0xFF000001, 0xFF000002), this.paint("{ \"gradient\": [\"#000001\", \"#000002\"] }"));
        }

        @Test
        void mistakesRejectTheFile() {
            assertTrue(this.tryPaint("{ \"sprite\": \"a:b\", \"tnit\": \"#FFFFFF\" }").isEmpty());
            assertTrue(this.tryPaint("{ \"nine_slice\": \"a:b\" }").isEmpty());
            assertTrue(this.tryPaint("{ \"nine_slice\": \"a:b\", \"border\": [1, 2, 3] }").isEmpty());
            assertTrue(this.tryPaint("{ \"gradient\": [\"#000001\"] }").isEmpty());
            assertTrue(this.tryPaint("\"red\"").isEmpty());
            assertTrue(this.tryPaint("{ \"sprite\": \"nocolon\" }").isEmpty());
            assertTrue(this.tryPaint("{}").isEmpty());
        }
    }

    @Nested
    class Reload {
        private static final String FLAT_WITH_RED = """
                { "parent": "strata:flat", "paints": { "button.normal": "#FF0000" } }""";

        @Test
        void aHostFollowsTheSource() {
            final MapSource source = new MapSource().file(MOD, FLAT_WITH_RED);
            final TestPlatform platform = new TestPlatform();
            platform.setThemes(source);
            final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.of(MOD), null);
            host.open(ui -> ui.button("Buy"));
            host.layout();
            assertEquals(Paint.solid(0xFFFF0000), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());

            source.clear(MOD).file(MOD, FLAT_WITH_RED.replace("#FF0000", "#00FF00")).bump();
            host.layout();
            assertEquals(Paint.solid(0xFF00FF00), host.theme().paint(Button.STYLE_NORMAL).orElseThrow());
            assertEquals(Optional.of(ThemeRef.of(MOD)), host.themeRef());
        }

        @Test
        void anIncompleteReloadKeepsTheOldTheme() {
            final MapSource source = new MapSource().file(MOD, FLAT_WITH_RED);
            final TestPlatform platform = new TestPlatform();
            platform.setThemes(source);
            final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.of(MOD), null);
            host.open(ui -> ui.button("Buy"));
            host.layout();
            final Theme before = host.theme();

            source.clear(MOD).file(MOD, "{ \"paints\": { \"button.normal\": \"#00FF00\" } }").bump();
            host.layout();
            assertSame(before, host.theme());

            source.clear(MOD).bump();
            host.layout();
            assertSame(before, host.theme());
        }

        @Test
        void aFixedThemeIgnoresTheSource() {
            final MapSource source = new MapSource().file(MOD, FLAT_WITH_RED);
            final TestPlatform platform = new TestPlatform();
            platform.setThemes(source);
            final ScreenHostImpl host = (ScreenHostImpl) StrataCore.createScreenHost(platform, ThemeRef.of(MOD), null);
            final Theme fixed = Themes.flat();
            host.setTheme(fixed);
            source.bump();
            host.layout();
            assertSame(fixed, host.theme());
            assertTrue(host.themeRef().isEmpty());
        }
    }

    /**
     * A theme source backed by maps.
     */
    static final class MapSource implements ThemeSource {
        private final Map<ThemeId, Theme> bases = new HashMap<>();
        private final Map<ThemeId, List<ThemeDocument>> files = new HashMap<>();
        private long version;

        MapSource base(final ThemeId id, final Theme theme) {
            this.bases.put(id, theme);
            return this;
        }

        MapSource file(final ThemeId id, final String json) {
            final List<ThemeDocument> list = this.files.computeIfAbsent(id, key -> new ArrayList<>());
            list.add(new ThemeDocument(id + "#" + list.size(), json));
            return this;
        }

        MapSource clear(final ThemeId id) {
            this.files.remove(id);
            return this;
        }

        MapSource bump() {
            this.version++;
            return this;
        }

        @Override
        public Optional<Theme> base(final ThemeId id) {
            return Optional.ofNullable(this.bases.get(id));
        }

        @Override
        public List<ThemeDocument> documents(final ThemeId id) {
            return List.copyOf(this.files.getOrDefault(id, List.of()));
        }

        @Override
        public long version() {
            return this.version;
        }
    }
}
