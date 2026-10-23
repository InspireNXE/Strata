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

import org.inspirenxe.strata.api.file.FileDialog;
import org.inspirenxe.strata.api.file.FileDialogs;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.spi.FileDialogSource;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * File dialogs (design 4.54).
 */
class FileDialogsTest {
    private final TestPlatform platform = new TestPlatform();

    /**
     * Dialogs whose answers a test gives later, as a person would.
     */
    static final class Pending implements FileDialogSource {
        final List<FileDialog> asked = new ArrayList<>();
        final CompletableFuture<Optional<Path>> one = new CompletableFuture<>();
        final CompletableFuture<List<Path>> many = new CompletableFuture<>();

        @Override
        public CompletableFuture<Optional<Path>> open(final FileDialog dialog) {
            this.asked.add(dialog);
            return this.one;
        }

        @Override
        public CompletableFuture<List<Path>> openMany(final FileDialog dialog) {
            this.asked.add(dialog);
            return this.many;
        }

        @Override
        public CompletableFuture<Optional<Path>> save(final FileDialog dialog) {
            this.asked.add(dialog);
            return this.one;
        }

        @Override
        public CompletableFuture<Optional<Path>> folder(final FileDialog dialog) {
            throw new IllegalStateException("no folders here");
        }
    }

    @Test
    void aDialogSaysWhatItAsks() {
        final FileDialog dialog = FileDialog.of(Text.of("Export"))
                .withStart(Path.of("reports"))
                .withSuggestedName("orders.csv")
                .withFilter(Text.of("Spreadsheets"), "CSV", "xlsx")
                .withFilter(Text.of("Text"), "txt");
        assertEquals(Path.of("reports"), dialog.start());
        assertEquals("orders.csv", dialog.suggestedName());
        assertEquals(List.of("csv", "xlsx"), dialog.filters().get(0).extensions());
        assertEquals(2, dialog.filters().size());
        assertTrue(dialog.filters().get(0).accepts(Path.of("dir", "Q3.CSV")));
        assertFalse(dialog.filters().get(0).accepts(Path.of("notes.txt")));
        assertEquals(null, FileDialog.of(Text.of("Open")).start());
        for (final String bad : List.of("", ".csv", "a/b", "a b")) {
            assertThrows(IllegalArgumentException.class, () -> dialog.withFilter(Text.of("Bad"), bad), bad);
        }
        assertThrows(IllegalArgumentException.class, () -> dialog.withFilter(Text.of("None")));
    }

    @Test
    void withoutDialogsEveryRequestCompletesEmptyAtOnce() throws Exception {
        final FileDialogs dialogs = new TestHost(this.platform, Themes.flat(), 100, 100).factory().fileDialogs();
        assertFalse(dialogs.isSupported());
        final FileDialog dialog = FileDialog.of(Text.of("Open"));
        assertEquals(Optional.empty(), dialogs.open(dialog).getNow(null));
        assertEquals(List.of(), dialogs.openMany(dialog).getNow(null));
        assertEquals(Optional.empty(), dialogs.save(dialog).getNow(null));
        assertEquals(Optional.empty(), dialogs.folder(dialog).getNow(null));
    }

    @Test
    void resultsArriveOnTheUiThreadAtTheNextFrame() throws Exception {
        final Pending pending = new Pending();
        this.platform.setFileDialogs(pending);
        final TestHost test = new TestHost(this.platform, Themes.flat(), 100, 100);
        final FileDialogs dialogs = test.factory().fileDialogs();
        assertTrue(dialogs.isSupported());
        assertSame(dialogs, test.factory().fileDialogs());

        final FileDialog dialog = FileDialog.of(Text.of("Import"));
        final CompletableFuture<Optional<Path>> opened = dialogs.open(dialog);
        assertEquals(List.of(dialog), pending.asked);
        final Thread elsewhere = new Thread(() -> pending.one.complete(Optional.of(Path.of("a.csv"))));
        elsewhere.start();
        elsewhere.join();
        assertFalse(opened.isDone(), "the result waits for the UI thread");
        assertTrue(test.host().needsFrame());

        test.host().render(new NullCanvas());
        assertEquals(Optional.of(Path.of("a.csv")), opened.get());
    }

    @Test
    void aCanceledOrFailedDialogCompletesAsSuch() {
        final Pending pending = new Pending();
        this.platform.setFileDialogs(pending);
        final TestHost test = new TestHost(this.platform, Themes.flat(), 100, 100);
        final FileDialogs dialogs = test.factory().fileDialogs();
        final FileDialog dialog = FileDialog.of(Text.of("Import"));

        final CompletableFuture<List<Path>> many = dialogs.openMany(dialog);
        pending.many.complete(null);
        final CompletableFuture<Optional<Path>> folder = dialogs.folder(dialog);
        test.host().render(new NullCanvas());

        assertEquals(List.of(), many.getNow(null));
        final ExecutionException failure = assertThrows(ExecutionException.class, folder::get);
        assertEquals("no folders here", failure.getCause().getMessage());
    }
}
