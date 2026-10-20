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
package org.inspirenxe.strata.core.control.field;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.attribute.text.TextEntry;
import org.inspirenxe.strata.api.control.field.PasswordTextBox;
import org.inspirenxe.strata.api.control.pane.Expander;
import org.inspirenxe.strata.api.overlay.Overlay;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.core.TestHost;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a password box leaves behind in memory: the buffer zeroes what it frees, and the host wipes boxes when their
 * screen or overlay closes. These tests read the buffer's array through its test hook.
 */
class SecretBufferTest {

    private static boolean zeroed(final char[] chars) {
        for (final char c : chars) {
            if (c != '\0') {
                return false;
            }
        }
        return true;
    }

    private static boolean zeroedFrom(final char[] chars, final int from) {
        for (int i = from; i < chars.length; i++) {
            if (chars[i] != '\0') {
                return false;
            }
        }
        return true;
    }

    @Test
    void growingZeroesTheOldArray() {
        final SecretBuffer buffer = new SecretBuffer();
        buffer.insert(0, "0123456789abcdef");
        final char[] old = buffer.storage();
        buffer.insertCodePoint(buffer.length(), 'g');
        assertTrue(zeroed(old), "the array the text grew out of is all zeros");
        assertEquals(17, buffer.length());
    }

    @Test
    void deletingZeroesTheFreedTail() {
        final SecretBuffer buffer = new SecretBuffer();
        buffer.insert(0, "abcdef");
        buffer.delete(1, 4);
        assertEquals(3, buffer.length());
        assertEquals('a', buffer.charAt(0));
        assertEquals('e', buffer.charAt(1));
        assertTrue(zeroedFrom(buffer.storage(), 3));
    }

    @Test
    void clearingZeroesEverything() {
        final SecretBuffer buffer = new SecretBuffer();
        buffer.insert(0, "abcdef");
        buffer.clear();
        assertTrue(zeroed(buffer.storage()));
        assertTrue(buffer.isEmpty());
    }

    @Test
    void itPrintsOnlyItsLengthAndComparesItsChars() {
        final SecretBuffer buffer = new SecretBuffer();
        buffer.insert(0, "abc");
        assertEquals("SecretBuffer[length=3]", buffer.toString());
        final SecretBuffer other = new SecretBuffer();
        other.insert(0, "abc");
        assertTrue(buffer.matches(other));
        assertTrue(buffer.matches("abc".toCharArray()));
        assertFalse(buffer.matches("abd".toCharArray()));
        assertFalse(buffer.matches("ab".toCharArray()));
        assertFalse(buffer.matches("abcd".toCharArray()));
    }

    @Test
    void surrogatePairsStayWhole() {
        final SecretBuffer buffer = new SecretBuffer();
        buffer.insertCodePoint(0, 0x1F600);
        buffer.insertCodePoint(2, 'a');
        assertEquals(3, buffer.length());
        assertEquals(2, buffer.codePointCount(0, 3));
        assertEquals(0x1F600, buffer.codePointAt(0));
        assertEquals(0x1F600, buffer.codePointBefore(2));
        assertEquals(2, buffer.offsetByCodePoints(0, 1));
    }

    @Test
    void closingTheScreenWipesTheBoxAfterClosedRuns() {
        final TestHost test = new TestHost(300, 100);
        final List<String> readInClosed = new ArrayList<>();
        final PasswordTextBox[] box = new PasswordTextBox[1];
        test.host().open(new Screen() {
            @Override
            public Component<?> build(final ComponentFactory factory) {
                box[0] = factory.passwordTextBox().setPassword("open sesame".toCharArray());
                return factory.absolutePanel().add(box[0]);
            }

            @Override
            public void closed() {
                readInClosed.add(box[0].withPassword(String::new));
            }
        });
        final SecretBuffer buffer = ((PasswordTextBoxImpl) box[0]).secretBuffer();
        final List<Integer> modified = new ArrayList<>();
        box[0].onEvent(TextEntry.MODIFIED, event -> modified.add(event.value()));

        test.host().close();

        assertEquals(List.of("open sesame"), readInClosed);
        assertTrue(box[0].isEmpty());
        assertTrue(zeroed(buffer.storage()));
        assertEquals(List.of(), modified, "a wipe fires nothing");
    }

    @Test
    void rebuildingTheScreenWipesTheOldBox() {
        final TestHost test = new TestHost(300, 100);
        final List<PasswordTextBox> built = new ArrayList<>();
        final ScreenHandle[] handle = new ScreenHandle[1];
        test.host().open(new Screen() {
            @Override
            public Component<?> build(final ComponentFactory factory) {
                final PasswordTextBox box = factory.passwordTextBox().setPassword("first".toCharArray());
                built.add(box);
                return factory.absolutePanel().add(box);
            }

            @Override
            public void opened(final ScreenHandle opened) {
                handle[0] = opened;
            }
        });
        handle[0].invalidate();
        test.host().layout();

        assertEquals(2, built.size());
        assertTrue(built.getFirst().isEmpty());
        assertTrue(zeroed(((PasswordTextBoxImpl) built.getFirst()).secretBuffer().storage()));
        assertEquals(5, built.getLast().length());
    }

    @Test
    void closingAnOverlayWipesItsBoxButCollapsingAnExpanderDoesNot() {
        final TestHost test = new TestHost(300, 200);
        final ComponentFactory ui = test.factory();
        final PasswordTextBox inExpander = ui.passwordTextBox().setPassword("kept".toCharArray());
        final Expander expander = ui.expander("More").setContent(inExpander).setExpanded(true);
        test.show(ui.absolutePanel().add(expander));
        final PasswordTextBox inDialog = ui.passwordTextBox().setPassword("dropped".toCharArray());
        final Overlay dialog = ui.overlays().dialog(ui.anchorPanel().add(inDialog));

        expander.setExpanded(false);
        dialog.close();

        assertEquals(4, inExpander.length());
        assertTrue(inDialog.isEmpty());
        assertTrue(zeroed(((PasswordTextBoxImpl) inDialog).secretBuffer().storage()));
    }

    @Test
    void clearingTheBoxZeroesItsBuffer() {
        final TestHost test = new TestHost(300, 100);
        final PasswordTextBox box = test.factory().passwordTextBox().setPassword("open sesame".toCharArray());
        final SecretBuffer buffer = ((PasswordTextBoxImpl) box).secretBuffer();
        box.clear();
        assertTrue(zeroed(buffer.storage()));
    }
}
