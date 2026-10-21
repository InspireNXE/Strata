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

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.Strata;
import org.inspirenxe.strata.api.appearance.Appearance;
import org.inspirenxe.strata.api.component.ComponentSnapshot;
import org.inspirenxe.strata.api.component.Tooltip;
import org.inspirenxe.strata.api.control.display.Avatar;
import org.inspirenxe.strata.api.event.Subscription;
import org.inspirenxe.strata.api.event.input.InputEvents;
import org.inspirenxe.strata.api.geometry.Insets;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.AbsolutePanel;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.api.theme.ThemeException;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.visual.Visual;
import org.inspirenxe.strata.core.control.Badge;
import org.inspirenxe.strata.core.control.display.AvatarImpl;
import org.inspirenxe.strata.core.text.Translations;
import org.inspirenxe.strata.spi.Translator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The avatar: its initials, what it shows first, how it draws the initials and the badge, its tooltip and its theme
 * keys. Text in the test platform is 6 units per code point and 9 per line. The avatar sits at (10, 10), so with the
 * default size its square is (10, 10, 24, 24).
 */
class AvatarTest {
    private static final SpriteId FACE = SpriteId.of("test", "face");
    private static final int BACKGROUND = 0xFF3C3F44;
    private static final int INITIALS = 0xFFF0F0F0;

    private final TestHost test = new TestHost(300, 200);
    private final ComponentFactory ui = this.test.factory();
    private final List<Subscription> registered = new ArrayList<>();

    private record Icon(String name) implements Visual {
        @Override
        public Size size() {
            return new Size(8f, 8f);
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    private record Account(String name) {
    }

    @AfterEach
    void cleanUp() {
        this.registered.forEach(Subscription::unsubscribe);
        Translations.setTranslator(Translator.NONE);
    }

    private Avatar show(final Avatar avatar) {
        this.test.show(this.ui.absolutePanel().add(avatar, 10, 10));
        return avatar;
    }

    private DrawLog render() {
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas;
    }

    private void registerAccounts() {
        this.registered.add(Strata.appearances().register(Account.class, Appearance.<Account>builder()
                .name(account -> Text.of(account.name()))
                .tooltip(account -> Tooltip.text("Account of " + account.name()))
                .build()));
    }

    @Nested
    class Initials {
        @Test
        void takeTheFirstLetterOfTheFirstAndLastWords() {
            assertEquals("AL", AvatarImpl.initialsOf("Ana Lima", Locale.US));
            assertEquals("C", AvatarImpl.initialsOf("Cleo", Locale.US));
            assertEquals("CD", AvatarImpl.initialsOf("Cleo  van Dev", Locale.US));
            assertEquals("BD", AvatarImpl.initialsOf("  ben\tdev ", Locale.US));
        }

        @Test
        void skipWordsWithoutLettersAndLeadingMarks() {
            assertEquals("", AvatarImpl.initialsOf("", Locale.US));
            assertEquals("", AvatarImpl.initialsOf("42 - 7", Locale.US));
            assertEquals("A", AvatarImpl.initialsOf("Ana 3", Locale.US));
            assertEquals("AL", AvatarImpl.initialsOf("(ana) \"lima\"", Locale.US));
            assertEquals("A", AvatarImpl.initialsOf("Ana ", Locale.US));
        }

        @Test
        void upperCaseForTheLocaleByCodePoint() {
            assertEquals("İK", AvatarImpl.initialsOf("ilk kar", Locale.forLanguageTag("tr-TR")));
            assertEquals("IK", AvatarImpl.initialsOf("ilk kar", Locale.US));
            assertEquals("SS", AvatarImpl.initialsOf("ßtadt", Locale.GERMANY));
            assertEquals("𐐀", AvatarImpl.initialsOf("𐐨n", Locale.US));
        }

        @Test
        void comeFromTheNameThenTheValue() {
            AvatarTest.this.registerAccounts();
            final Avatar avatar = AvatarTest.this.ui.avatar(new Account("Ben Dev"));
            assertEquals("BD", avatar.initials());
            avatar.setName("Ana Lima");
            assertEquals("AL", avatar.initials());
            avatar.setInitials("X");
            assertEquals("X", avatar.initials());
            avatar.setInitials("");
            assertEquals("", avatar.initials());
            avatar.setInitials(null).setName((String) null);
            assertEquals("BD", avatar.initials());
            assertEquals(Text.empty(), avatar.name());
        }

        @Test
        void aValueWithNoAppearanceIsNamedByItsString() {
            assertEquals("AL", AvatarTest.this.ui.avatar("Ana Lima").initials());
            assertEquals("", AvatarTest.this.ui.avatar().initials());
        }

        @Test
        void followTheLanguage() {
            final LocaleTranslator translator = new LocaleTranslator(Locale.US, Map.of("test.name", "ilk kar"));
            Translations.setTranslator(translator);
            final Avatar avatar = AvatarTest.this.ui.avatar().setName(Text.translatable("test.name"));
            assertEquals("IK", avatar.initials());
            translator.set(Locale.forLanguageTag("tr-TR"), Map.of("test.name", "ilk kar"));
            assertEquals("İK", avatar.initials());
        }
    }

    @Nested
    class Drawing {
        @Test
        void isASquareOfItsSize() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar());
            assertEquals(new Rect(10, 10, 24, 24), avatar.bounds());
            avatar.setAvatarSize(40);
            AvatarTest.this.test.host().layout();
            assertEquals(new Rect(10, 10, 40, 40), avatar.bounds());
            assertEquals(40f, avatar.avatarSize());
            assertThrows(IllegalArgumentException.class, () -> avatar.setAvatarSize(-1));
            assertThrows(IllegalArgumentException.class, () -> avatar.setAvatarSize(Float.POSITIVE_INFINITY));
            assertThrows(IllegalArgumentException.class, () -> avatar.setAvatarSize(Float.NaN));
        }

        @Test
        void drawsTheLargestCenteredSquareInOtherBounds() {
            final Avatar avatar = AvatarTest.this.ui.avatar().setSprite(FACE);
            avatar.setAnchor(Anchor.FILL);
            AvatarTest.this.test.show(AvatarTest.this.ui.anchorPanel().setWidth(60).setHeight(30).add(avatar));
            assertEquals(List.of("fill 15,0 30x30 #FF3C3F44", "sprite test:face 15,0 30x30 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
        }

        @Test
        void showsTheInitialsCenteredAtOneScale() {
            AvatarTest.this.show(AvatarTest.this.ui.avatar().setName("Ana Lima"));
            // "AL" is 12 by 9, centered in the square at (10, 10, 24, 24)
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "text AL 16,17.5 #FFF0F0F0"),
                    AvatarTest.this.render().lines());
        }

        @Test
        void scalesTheInitialsByWholeSteps() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setName("Cleo")
                    .setAvatarSize(48));
            // Two fifths of 48 is 19.2, two lines of 9: scale 2, so "C" is 12 by 18 at (28, 25)
            assertEquals(List.of("fill 10,10 48x48 #FF3C3F44", "scale 2", "text C 14,12.5 #FFF0F0F0", "pop"),
                    AvatarTest.this.render().lines());
            avatar.setAvatarSize(72);
            AvatarTest.this.test.host().layout();
            assertTrue(AvatarTest.this.render().lines().contains("scale 3"));
            avatar.setAvatarSize(12);
            AvatarTest.this.test.host().layout();
            assertEquals(List.of(), AvatarTest.this.render().lines("scale"));
        }

        @Test
        void prefersAPictureToTheInitials() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setName("Ana Lima").setSprite(FACE));
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "sprite test:face 10,10 24x24 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
            avatar.setVisual(new Icon("photo"));
            assertNull(avatar.sprite());
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "visual photo 10,10 24x24 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
            avatar.setSprite(FACE);
            assertNull(avatar.visual());
            avatar.setSprite(null);
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "text AL 16,17.5 #FFF0F0F0"),
                    AvatarTest.this.render().lines());
        }

        @Test
        void showsTheValuesIconUnlessAPictureIsSet() {
            AvatarTest.this.registered.add(Strata.appearances().register(Account.class,
                    Appearance.<Account>builder().icon(account -> new Icon("face of " + account.name())).build()));
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar(new Account("Ana")));
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "visual face of Ana 10,10 24x24 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
            avatar.setSprite(FACE);
            assertEquals("sprite test:face 10,10 24x24 #FFFFFFFF", AvatarTest.this.render().lines().get(1));
        }

        @Test
        void fallsBackToThePlaceholderThenTheBackgroundAlone() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar());
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44"), AvatarTest.this.render().lines());

            final Theme theme = Themes.flat().toBuilder()
                    .paint(Avatar.STYLE_PLACEHOLDER, Paint.solid(0xFF000001))
                    .paint(Avatar.STYLE_FRAME, Paint.solid(0xFF000003))
                    .build();
            AvatarTest.this.test.host().setTheme(theme);
            AvatarTest.this.test.host().layout();
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "fill 10,10 24x24 #FF000001", "fill 10,10 24x24 #FF000003"),
                    AvatarTest.this.render().lines());
            // A group falls back to the plain placeholder, then takes its own
            avatar.setGroup(true);
            assertTrue(avatar.isGroup());
            assertEquals("fill 10,10 24x24 #FF000001", AvatarTest.this.render().lines().get(1));
            AvatarTest.this.test.host().setTheme(theme.toBuilder()
                    .paint(Avatar.STYLE_PLACEHOLDER_GROUP, Paint.solid(0xFF000002))
                    .build());
            AvatarTest.this.test.host().layout();
            assertEquals("fill 10,10 24x24 #FF000002", AvatarTest.this.render().lines().get(1));
        }

        @Test
        void drawsTheFrameOverThePicture() {
            AvatarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .paint(Avatar.STYLE_FRAME, Paint.solid(0xFF000003))
                    .build());
            AvatarTest.this.show(AvatarTest.this.ui.avatar().setSprite(FACE));
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "sprite test:face 10,10 24x24 #FFFFFFFF",
                    "fill 10,10 24x24 #FF000003"), AvatarTest.this.render().lines());
        }

        @Test
        void requiresItsBackground() {
            final Theme theme = Theme.builder("bare").build();
            final TestHost bare = new TestHost(new TestPlatform(), theme, 100, 100);
            final ThemeException thrown = assertThrows(ThemeException.class,
                    () -> bare.show(bare.factory().avatar()));
            assertEquals(Set.of(Avatar.STYLE_BACKGROUND), thrown.missingKeys());
        }

        @Test
        void drawsTheSameWhenDisabled() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setName("Ana Lima"));
            final List<String> enabled = AvatarTest.this.render().lines();
            avatar.setEnabled(false);
            assertEquals(enabled, AvatarTest.this.render().lines());
        }
    }

    @Nested
    class Badges {
        @Test
        void showACountAtTheBottomRightCorner() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setBadge(3));
            assertEquals(3, avatar.badge());
            // "3" is 6 by 9, with 2 at the sides and 1 above and below: 10 by 11
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "fill 24,23 10x11 #FFD32F2F", "text 3 26,24 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
        }

        @Test
        void capAtNinetyNinePlus() {
            assertEquals("", Badge.text(0));
            assertEquals("99", Badge.text(99));
            assertEquals("99+", Badge.text(100));
            AvatarTest.this.show(AvatarTest.this.ui.avatar().setBadge(120));
            assertEquals(List.of("text 99+ 14,24 #FFFFFFFF"), AvatarTest.this.render().lines("text"));
        }

        @Test
        void hideAtZeroAndRefuseNegativeCounts() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setBadge(5));
            avatar.setBadge(0);
            AvatarTest.this.test.host().layout();
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44"), AvatarTest.this.render().lines());
            assertThrows(IllegalArgumentException.class, () -> avatar.setBadge(-1));
        }

        @Test
        void aVisualTakesTheNumbersPlaceInAThirdOfTheSize() {
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar().setBadge(4)
                    .setBadgeVisual(new Icon("online")));
            assertEquals(Optional.of(new Icon("online")), avatar.badgeVisual());
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "visual online 26,26 8x8 #FFFFFFFF"),
                    AvatarTest.this.render().lines());
            avatar.setBadgeVisual(null);
            assertEquals(Optional.empty(), avatar.badgeVisual());
            assertTrue(AvatarTest.this.render().lines().contains("text 4 26,24 #FFFFFFFF"));
        }

        @Test
        void readTheSharedBadgeKeys() {
            AvatarTest.this.test.host().setTheme(Themes.flat().toBuilder()
                    .paint(Avatar.STYLE_BADGE, Paint.solid(0xFF0000AA))
                    .textStyle(Avatar.STYLE_BADGE_TEXT, TextStyle.DEFAULT.withColor(0xFF00AA00))
                    .insets(Avatar.STYLE_BADGE_PADDING, new Insets(1, 0, 1, 0))
                    .build());
            AvatarTest.this.show(AvatarTest.this.ui.avatar().setBadge(7));
            assertEquals(List.of("fill 10,10 24x24 #FF3C3F44", "fill 26,25 8x9 #FF0000AA", "text 7 27,25 #FF00AA00"),
                    AvatarTest.this.render().lines());
        }
    }

    @Nested
    class Input {
        @Test
        void takesNoFocusAndLetsClicksThrough() {
            final AtomicInteger clicks = new AtomicInteger();
            final Avatar avatar = AvatarTest.this.ui.avatar();
            final AbsolutePanel panel = AvatarTest.this.ui.absolutePanel().add(avatar, 10, 10);
            panel.onEvent(InputEvents.CLICK, event -> clicks.incrementAndGet());
            AvatarTest.this.test.show(panel);
            AvatarTest.this.test.click(20, 20);
            assertEquals(1, clicks.get());
            assertNull(AvatarTest.this.test.focused());
            assertEquals(List.of(), AvatarTest.this.test.tabOrder());
        }

        @Test
        void showsTheValuesTooltipAskedAnewUntilOneIsSet() {
            final AtomicInteger asked = new AtomicInteger();
            AvatarTest.this.registered.add(Strata.appearances().register(Account.class, Appearance.<Account>builder()
                    .tooltip(account -> Tooltip.text("asked " + asked.incrementAndGet()))
                    .build()));
            final Avatar avatar = AvatarTest.this.show(AvatarTest.this.ui.avatar(new Account("Ana")));
            assertEquals(Tooltip.text("asked 1"), avatar.tooltip());
            AvatarTest.this.test.host().pointerMoved(20, 20, Modifiers.NONE);
            assertEquals(Tooltip.text("asked 2"), avatar.tooltip());
            avatar.setTooltip("Mine");
            AvatarTest.this.test.host().pointerMoved(200, 150, Modifiers.NONE);
            AvatarTest.this.test.host().pointerMoved(20, 20, Modifiers.NONE);
            assertEquals(Tooltip.text("Mine"), avatar.tooltip());
        }
    }

    @Test
    void snapshotsItsState() {
        AvatarTest.this.registerAccounts();
        final Account ana = new Account("Ana Lima");
        final Avatar avatar = this.show(this.ui.avatar(ana).setGroup(true).setBadge(2));
        assertSame(ana, avatar.value());
        final List<ComponentSnapshot> snapshots = new ArrayList<>();
        final AbsolutePanel panel = (AbsolutePanel) avatar.parent();
        panel.onEvent(InputEvents.CLICK, event -> snapshots.add(event.snapshotOf(avatar).orElseThrow()));
        this.test.click(20, 20);
        assertEquals(new Avatar.State(ana, Text.empty(), "AL", true, 2), snapshots.get(0).state());
        assertFalse(snapshots.isEmpty());
    }

    @Test
    void clearsTheValue() {
        this.registerAccounts();
        final Avatar avatar = this.ui.avatar(new Account("Ana Lima")).setValue(null);
        assertNull(avatar.value());
        assertEquals("", avatar.initials());
        assertEquals(INITIALS, Themes.flat().textStyle(Avatar.STYLE_INITIALS).orElseThrow().color());
        assertEquals(BACKGROUND, ((Paint.Solid) Themes.flat().paint(Avatar.STYLE_BACKGROUND).orElseThrow()).argb());
    }
}
