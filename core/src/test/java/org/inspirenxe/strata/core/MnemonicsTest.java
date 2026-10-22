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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.control.button.CheckBox;
import org.inspirenxe.strata.api.control.display.Label;
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.control.menu.Menu;
import org.inspirenxe.strata.api.control.menu.MenuBar;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.control.pane.TabBar;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.layout.Anchor;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.core.text.Mnemonics;
import org.inspirenxe.strata.spi.Conventions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Mnemonics in captions, Alt with a letter, the keyboard's way into a menu bar, and an open menu's own keys
 * (designs 4.40 and 4.41).
 */
class MnemonicsTest {
    private static final Modifiers ALT = Modifiers.of(Modifier.ALT);

    private TestHost test = new TestHost(400, 300);
    private ComponentFactory ui = this.test.factory();
    private final List<String> heard = new ArrayList<>();

    private boolean press(final Key key, final Modifiers modifiers) {
        final boolean used = this.test.host().key(key, KeyAction.PRESS, modifiers);
        this.test.host().key(key, KeyAction.RELEASE, modifiers);
        return used;
    }

    private boolean alt(final Key key) {
        this.test.host().key(Key.LEFT_ALT, KeyAction.PRESS, ALT);
        final boolean used = this.test.host().key(key, KeyAction.PRESS, ALT);
        this.test.host().key(key, KeyAction.RELEASE, ALT);
        this.test.host().key(Key.LEFT_ALT, KeyAction.RELEASE, Modifiers.NONE);
        return used;
    }

    private void altAlone() {
        this.test.host().key(Key.LEFT_ALT, KeyAction.PRESS, ALT);
        this.test.host().key(Key.LEFT_ALT, KeyAction.RELEASE, Modifiers.NONE);
    }

    private List<String> texts() {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log.lines("text ").stream().map(line -> line.split(" ")[1]).toList();
    }

    private void noMnemonics() {
        final TestPlatform platform = new TestPlatform();
        platform.setConventions(new Conventions(false, Modifier.CONTROL, false));
        this.test = new TestHost(platform, Themes.flat(), 400, 300);
        this.ui = this.test.factory();
    }

    @Nested
    class Parsing {
        @Test
        void theLetterAfterTheFirstUnderscoreIsTheMnemonic() {
            final Mnemonics parsed = Mnemonics.parse(List.of(TextSpan.of("Save _as")));
            assertEquals('a', parsed.mnemonic());
            assertEquals(List.of("Save as"), parsed.plain().stream().map(TextSpan::text).toList());
            assertEquals(List.of("Save ", "a", "s"), parsed.marked().stream().map(TextSpan::text).toList());
            assertTrue(parsed.marked().get(1).style().underline());
            assertFalse(parsed.marked().get(0).style().underline());
        }

        @Test
        void aDoubledUnderscoreIsOneAndAnEndingOneIsKept() {
            final Mnemonics doubled = Mnemonics.parse(List.of(TextSpan.of("snake__case_")));
            assertEquals(Mnemonics.NONE, doubled.mnemonic());
            assertEquals("snake_case_", doubled.plain().getFirst().text());
            assertSame(doubled.plain(), doubled.marked());
        }

        @Test
        void laterMarkersAreTakenOutAndTheMnemonicIsLowerCase() {
            final Mnemonics parsed = Mnemonics.parse(List.of(TextSpan.of("_File _Menu")));
            assertEquals('f', parsed.mnemonic());
            assertEquals("File Menu", parsed.plain().getFirst().text());
        }

        @Test
        void textWithoutUnderscoresIsLeftAlone() {
            final List<TextSpan> spans = List.of(TextSpan.of("Plain"));
            final Mnemonics parsed = Mnemonics.parse(spans);
            assertSame(spans, parsed.plain());
            assertEquals(Mnemonics.NONE, parsed.mnemonic());
        }
    }

    @Nested
    class Controls {
        @Test
        void altWithTheLetterPressesAButtonWhoseMarkerIsNotDrawn() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final TextBox field = outer.ui.textBox();
            final Button save = outer.ui.button("_Save").onEvent(Button.PRESSED, event -> outer.heard.add("save"));
            outer.test.show(outer.ui.stackPanel().add(field).add(save).setAnchor(Anchor.TOP_LEFT));
            field.requestFocus();
            assertTrue(outer.texts().contains("Save"));
            assertTrue(outer.alt(Key.S));
            assertEquals(List.of("save"), outer.heard);
            assertSame(save, outer.test.focused());
        }

        @Test
        void holdingAltUnderlinesTheLetter() {
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.test.show(outer.ui.button("_Save").setAnchor(Anchor.TOP_LEFT));
            final DrawLog idle = new DrawLog();
            outer.test.host().render(idle);
            assertTrue(idle.textStyles().stream().noneMatch(TextStyle::underline));
            outer.test.host().key(Key.LEFT_ALT, KeyAction.PRESS, ALT);
            final DrawLog held = new DrawLog();
            outer.test.host().render(held);
            assertEquals(List.of("text S", "text ave"), held.lines("text ").stream()
                    .map(line -> line.substring(0, line.indexOf(' ', 5))).toList());
            assertTrue(held.textStyles().getFirst().underline());
        }

        @Test
        void withMnemonicsOffTheMarkersStillGoAndAltDoesNothing() {
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.noMnemonics();
            final Button save = outer.ui.button("_Save").onEvent(Button.PRESSED, event -> outer.heard.add("save"));
            outer.test.show(save.setAnchor(Anchor.TOP_LEFT));
            assertFalse(outer.alt(Key.S));
            assertEquals(List.of(), outer.heard);
            outer.test.host().key(Key.LEFT_ALT, KeyAction.PRESS, ALT);
            assertEquals(List.of("Save"), outer.texts());
        }

        @Test
        void aCheckBoxTogglesAndTakesFocus() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final CheckBox grid = outer.ui.checkBox("Show _grid");
            outer.test.show(grid.setAnchor(Anchor.TOP_LEFT));
            outer.alt(Key.G);
            assertTrue(grid.isSelected());
            assertSame(grid, outer.test.focused());
        }

        @Test
        void sharedLettersMoveFocusInTurnWithoutActing() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final Button first = outer.ui.button("_Apply").onEvent(Button.PRESSED, event -> outer.heard.add("first"));
            final Button second = outer.ui.button("_All").onEvent(Button.PRESSED, event -> outer.heard.add("second"));
            outer.test.show(outer.ui.stackPanel().add(first).add(second).setAnchor(Anchor.TOP_LEFT));
            outer.alt(Key.A);
            assertSame(first, outer.test.focused());
            outer.alt(Key.A);
            assertSame(second, outer.test.focused());
            outer.alt(Key.A);
            assertSame(first, outer.test.focused());
            assertEquals(List.of(), outer.heard);
        }

        @Test
        void aParsingLabelFocusesTheNextTabStop() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final Button before = outer.ui.button("Before");
            final Label name = outer.ui.label("_Name:").setMnemonicParsing(true);
            final TextBox field = outer.ui.textBox();
            final Label data = outer.ui.label("file_name");
            outer.test.show(outer.ui.stackPanel().add(before).add(name).add(field).add(data)
                    .setAnchor(Anchor.TOP_LEFT));
            assertEquals(List.of("Before", "Name:", "file_name"), outer.texts());
            before.requestFocus();
            outer.alt(Key.N);
            assertSame(field, outer.test.focused());
        }

        @Test
        void aTabIsSelectedAndAnExpanderToggled() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final TabBar tabs = outer.ui.tabBar().addTab(Text.of("_General")).addTab(Text.of("_Keys"));
            final Expander more = outer.ui.expander("_More").setContent(outer.ui.label("Body"));
            outer.test.show(outer.ui.stackPanel().add(tabs).add(more).setAnchor(Anchor.TOP_LEFT));
            outer.alt(Key.K);
            assertEquals(1, tabs.selectedIndex());
            outer.alt(Key.M);
            assertTrue(more.isExpanded());
        }

        @Test
        void aDialogKeepsMnemonicsToItself() {
            final MnemonicsTest outer = MnemonicsTest.this;
            final Button below = outer.ui.button("_Below").onEvent(Button.PRESSED, event -> outer.heard.add("below"));
            outer.test.show(below.setAnchor(Anchor.TOP_LEFT));
            outer.ui.overlays().dialog(outer.ui.button("_OK").onEvent(Button.PRESSED, event -> outer.heard.add("ok")));
            outer.alt(Key.B);
            outer.alt(Key.O);
            assertEquals(List.of("ok"), outer.heard);
        }
    }

    @Nested
    class MenuBars {
        private MenuBar bar;
        private Menu file;
        private Menu edit;
        private Button target;

        private void show() {
            final MnemonicsTest outer = MnemonicsTest.this;
            this.file = outer.ui.menu()
                    .addItem("_New", () -> outer.heard.add("new"))
                    .addItem("_Open", () -> outer.heard.add("open"))
                    .addItem("Close", () -> outer.heard.add("close"));
            this.edit = outer.ui.menu().addItem("_Undo", () -> outer.heard.add("undo"));
            this.bar = outer.ui.menuBar().addMenu(Text.of("_File"), this.file).addMenu(Text.of("_Edit"), this.edit);
            this.target = outer.ui.button("Target");
            outer.test.show(outer.ui.stackPanel().add(this.bar).add(this.target).setAnchor(Anchor.TOP_LEFT));
            this.target.requestFocus();
        }

        @Test
        void altWithATitlesLetterOpensItsMenuAndALetterPicksAnItem() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.alt(Key.F);
            assertTrue(this.file.isOpen());
            assertEquals(0, this.bar.selectedIndex());
            outer.press(Key.O, Modifiers.NONE);
            assertEquals(List.of("open"), outer.heard);
            assertFalse(this.file.isOpen());
            assertSame(this.target, outer.test.focused());
        }

        @Test
        void altWithAnotherTitlesLetterInAnOpenMenuSwitchesMenus() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.alt(Key.F);
            outer.alt(Key.E);
            assertTrue(this.edit.isOpen());
            assertFalse(this.file.isOpen());
        }

        @Test
        void altAloneHighlightsTheBarAndArrowsMoveAndOpen() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.altAlone();
            assertTrue(outer.test.host().root().showsMnemonics());
            assertTrue(outer.press(Key.RIGHT, Modifiers.NONE));
            outer.press(Key.DOWN, Modifiers.NONE);
            assertTrue(this.edit.isOpen());
            assertFalse(outer.test.host().root().showsMnemonics());
        }

        @Test
        void escapeOrAltAgainLeavesTheBar() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.altAlone();
            outer.press(Key.ESCAPE, Modifiers.NONE);
            assertFalse(outer.test.host().root().showsMnemonics());
            outer.press(Key.F10, Modifiers.NONE);
            assertTrue(outer.test.host().root().showsMnemonics());
            outer.altAlone();
            assertFalse(outer.test.host().root().showsMnemonics());
            assertSame(this.target, outer.test.focused());
        }

        @Test
        void altWithAnotherKeyIsNotAltAlone() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.alt(Key.Q);
            assertFalse(outer.test.host().root().showsMnemonics());
            assertFalse(this.file.isOpen());
        }
    }

    @Nested
    class OpenMenus {
        private Menu menu;

        private void show() {
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.test.show(outer.ui.button("Target").setAnchor(Anchor.TOP_LEFT));
            this.menu = outer.ui.menu()
                    .addItem("Cut", () -> outer.heard.add("cut"))
                    .addItem("Copy", () -> outer.heard.add("copy"))
                    .addItem("_Paste", () -> outer.heard.add("paste"))
                    .addItem("Delete", () -> outer.heard.add("delete"));
            this.menu.openAt(10, 10);
            outer.test.host().layout();
        }

        @Test
        void homeAndEndReachTheEnds() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.press(Key.END, Modifiers.NONE);
            outer.press(Key.ENTER, Modifiers.NONE);
            this.menu.openAt(10, 10);
            outer.test.host().layout();
            outer.press(Key.END, Modifiers.NONE);
            outer.press(Key.HOME, Modifiers.NONE);
            outer.press(Key.ENTER, Modifiers.NONE);
            assertEquals(List.of("delete", "cut"), outer.heard);
        }

        @Test
        void aLetterWithNoMnemonicMovesToTheNextItemStartingWithIt() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.press(Key.C, Modifiers.NONE);
            outer.press(Key.C, Modifiers.NONE);
            outer.press(Key.ENTER, Modifiers.NONE);
            assertEquals(List.of("copy"), outer.heard);
        }

        @Test
        void aMnemonicChoosesAtOnce() {
            this.show();
            final MnemonicsTest outer = MnemonicsTest.this;
            outer.press(Key.P, Modifiers.NONE);
            assertEquals(List.of("paste"), outer.heard);
            assertFalse(this.menu.isOpen());
        }
    }
}
