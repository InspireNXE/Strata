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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.screen.Screen;
import org.inspirenxe.strata.api.screen.ScreenHandle;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.ScreenHost;
import org.inspirenxe.strata.spi.ScreenStack;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Screens refuse closes the user asks for, know whether they are open, and change their title after they open.
 */
class ScreenClosingTest {
    private final List<String> heard = new ArrayList<>();

    /**
     * An editor that refuses to close while it has unsaved changes, as a developer writes it.
     */
    class Editor implements Screen {
        final String name;
        boolean dirty;
        ScreenHandle handle;

        Editor(final String name) {
            this.name = name;
        }

        @Override
        public Component<?> build(final ComponentFactory factory) {
            return factory.label(this.name);
        }

        @Override
        public Text title() {
            return Text.of(this.name);
        }

        @Override
        public void opened(final ScreenHandle opened) {
            this.handle = opened;
        }

        @Override
        public boolean closeRequested(final ScreenHandle asked) {
            ScreenClosingTest.this.heard.add(this.name + " asked, same handle " + (asked == this.handle));
            return !this.dirty;
        }

        @Override
        public void closed() {
            ScreenClosingTest.this.heard.add(this.name + " closed");
        }
    }

    private ScreenHost host(final Runnable closeRequested) {
        return StrataCore.createScreenHost(new TestPlatform(), Themes.flat(), closeRequested);
    }

    @Test
    void theEnginesCloseButtonAsksTheScreenAndClosesItIfItAgrees() {
        final ScreenHost host = this.host(() -> this.heard.add("runnable"));
        final Editor editor = new Editor("notes");
        host.open(editor);

        editor.dirty = true;
        assertFalse(host.requestClose());
        assertTrue(editor.handle.isOpen());

        editor.dirty = false;
        assertTrue(host.requestClose());
        assertFalse(editor.handle.isOpen());
        assertTrue(host.screen().isEmpty());
        assertEquals(List.of("notes asked, same handle true", "notes asked, same handle true", "notes closed"),
                this.heard, "the adapter asked, so its runnable does not run");
    }

    @Test
    void requestCloseWithNoScreenOpenLetsTheEngineClose() {
        assertTrue(this.host(null).requestClose());
    }

    @Test
    void aCancelButtonAsksThroughTheHandleAndTheRunnableRunsWhenTheScreenAgrees() {
        final ScreenHost host = this.host(() -> this.heard.add("runnable"));
        final Editor editor = new Editor("notes");
        host.open(editor);

        editor.dirty = true;
        assertFalse(editor.handle.requestClose());
        editor.dirty = false;
        assertTrue(editor.handle.requestClose());

        assertEquals(List.of("notes asked, same handle true", "notes asked, same handle true", "runnable"), this.heard);
    }

    @Test
    void closeFromCodeNeverAsks() {
        final ScreenHost host = this.host(null);
        final Editor editor = new Editor("notes");
        editor.dirty = true;
        host.open(editor);

        editor.handle.close();

        assertFalse(editor.handle.isOpen());
        assertEquals(List.of("notes closed"), this.heard);
    }

    @Test
    void aScreenThatRefusesCanCloseItselfOnceTheUserAgrees() {
        final ScreenHost host = this.host(null);
        final Editor editor = new Editor("notes");
        editor.dirty = true;
        host.open(editor);

        assertFalse(host.requestClose());
        // The user answers the screen's own dialog with Discard
        editor.handle.close();

        assertTrue(host.screen().isEmpty());
        assertEquals(List.of("notes asked, same handle true", "notes closed"), this.heard);
    }

    @Test
    void aScreenThatClosesItselfWhileAskedCountsAsAgreeing() {
        final ScreenHost host = this.host(null);
        final Editor editor = new Editor("notes") {
            @Override
            public boolean closeRequested(final ScreenHandle asked) {
                asked.close();
                return true;
            }
        };
        host.open(editor);

        assertTrue(host.requestClose());
        assertEquals(List.of("notes closed"), this.heard);
    }

    @Test
    void anEndedHandleRefusesToCloseAndSetsNoTitle() {
        final ScreenHost host = this.host(null);
        final Editor first = new Editor("first");
        host.open(first);
        host.open(new Editor("second"));

        assertFalse(first.handle.isOpen());
        assertFalse(first.handle.requestClose());
        assertSame(first.handle, first.handle.setTitle(Text.of("stale")));
        assertEquals("second", host.title().plain());
    }

    @Test
    void theTitleStartsAsTheScreensAndFollowsTheHandle() {
        final ScreenHost host = this.host(null);
        assertEquals("", host.title().plain());
        final Editor editor = new Editor("notes");
        host.open(editor);
        assertEquals("notes", host.title().plain());

        editor.handle.setTitle(Text.of("notes*"));
        assertEquals("notes*", host.title().plain());

        host.close();
        assertEquals("", host.title().plain());
    }

    @Test
    void aStackAsksEveryScreenFromTheTopAndStopsAtTheFirstThatRefuses() {
        final ScreenStack stack = StrataCore.createScreenStack(new TestPlatform(), Themes.flat());
        final Editor bottom = new Editor("bottom");
        final Editor middle = new Editor("middle");
        final Editor top = new Editor("top");
        stack.push(bottom);
        stack.push(middle);
        stack.push(top);
        middle.dirty = true;
        assertEquals("top", stack.title().plain());

        assertFalse(stack.requestClose());
        assertSame(middle, stack.top().orElseThrow());
        assertEquals("middle", stack.title().plain());

        middle.dirty = false;
        assertTrue(stack.requestClose());
        assertTrue(stack.isEmpty());
        assertEquals("", stack.title().plain());
        assertEquals(List.of("top asked, same handle true", "top closed", "middle asked, same handle true",
                "middle asked, same handle true", "middle closed", "bottom asked, same handle true", "bottom closed"),
                this.heard);
    }
}
