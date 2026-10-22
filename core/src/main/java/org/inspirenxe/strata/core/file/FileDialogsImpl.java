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
package org.inspirenxe.strata.core.file;

import org.inspirenxe.strata.api.UiThread;
import org.inspirenxe.strata.api.file.FileDialog;
import org.inspirenxe.strata.api.file.FileDialogs;
import org.inspirenxe.strata.spi.FileDialogSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The core {@link FileDialogs}: the platform's {@link FileDialogSource}, with each result handed to the UI thread
 * through {@link UiThread#execute}. Without a source every future completes empty at once. A source that throws, or
 * returns null, fails its future on the UI thread too.
 */
public final class FileDialogsImpl implements FileDialogs {
    private final FileDialogSource source;
    private final UiThread uiThread;

    /**
     * Creates the dialogs of a platform.
     *
     * @param source the platform's dialogs, or {@code null} for none
     * @param uiThread the platform's UI thread
     */
    public FileDialogsImpl(final FileDialogSource source, final UiThread uiThread) {
        this.source = source;
        this.uiThread = Objects.requireNonNull(uiThread, "uiThread");
    }

    @Override
    public boolean isSupported() {
        return this.source != null;
    }

    @Override
    public CompletableFuture<Optional<Path>> open(final FileDialog dialog) {
        return this.ask(dialog, FileDialogSource::open, Optional.empty());
    }

    @Override
    public CompletableFuture<List<Path>> openMany(final FileDialog dialog) {
        return this.ask(dialog, FileDialogSource::openMany, List.of());
    }

    @Override
    public CompletableFuture<Optional<Path>> save(final FileDialog dialog) {
        return this.ask(dialog, FileDialogSource::save, Optional.empty());
    }

    @Override
    public CompletableFuture<Optional<Path>> folder(final FileDialog dialog) {
        return this.ask(dialog, FileDialogSource::folder, Optional.empty());
    }

    private <T> CompletableFuture<T> ask(final FileDialog dialog, final Asker<T> asker, final T none) {
        Objects.requireNonNull(dialog, "dialog");
        if (this.source == null) {
            return CompletableFuture.completedFuture(none);
        }
        final CompletableFuture<T> result = new CompletableFuture<>();
        final CompletableFuture<T> shown;
        try {
            shown = asker.ask(this.source, dialog);
        } catch (final RuntimeException e) {
            this.uiThread.execute(() -> result.completeExceptionally(e));
            return result;
        }
        if (shown == null) {
            this.uiThread.execute(() -> result.completeExceptionally(
                    new IllegalStateException("The platform's file dialogs returned no future")));
            return result;
        }
        shown.whenComplete((value, failure) -> this.uiThread.execute(() -> {
            if (failure != null) {
                result.completeExceptionally(failure);
            } else {
                result.complete(value == null ? none : value);
            }
        }));
        return result;
    }

    /**
     * Shows one kind of dialog.
     */
    @FunctionalInterface
    private interface Asker<T> {
        CompletableFuture<T> ask(FileDialogSource source, FileDialog dialog);
    }
}
