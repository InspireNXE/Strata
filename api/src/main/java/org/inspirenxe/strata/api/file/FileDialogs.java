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
package org.inspirenxe.strata.api.file;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The platform's file dialogs, for an application's Import and Export: a dialog to open a file or several, to save
 * one, and to choose a folder. Get them from {@code ComponentFactory.fileDialogs()}.
 *
 * <p>Each call shows the engine's own dialog and returns at once. Its future completes on the UI thread when the
 * user chooses, empty when the user cancels, so its actions may change components. An engine without dialogs is not
 * {@linkplain #isSupported() supported}, and then every future completes empty at once. A future completes
 * exceptionally when the engine's dialog failed. Strata draws no file chooser of its own.</p>
 *
 * <p>Do not implement this interface.</p>
 */
public interface FileDialogs {

    /**
     * Returns whether the engine shows file dialogs.
     *
     * @return false when every dialog completes empty at once
     */
    boolean isSupported();

    /**
     * Asks for one file to open.
     *
     * @param dialog what the dialog asks
     * @return the file chosen, or empty when the user canceled or dialogs are not supported
     */
    CompletableFuture<Optional<Path>> open(FileDialog dialog);

    /**
     * Asks for one or more files to open.
     *
     * @param dialog what the dialog asks
     * @return the files chosen, in the engine's order, or an empty list when the user canceled or dialogs are not
     *     supported
     */
    CompletableFuture<List<Path>> openMany(FileDialog dialog);

    /**
     * Asks where to save a file. The engine may ask the user before it returns a file that exists; Strata writes
     * nothing.
     *
     * @param dialog what the dialog asks, whose suggested name it starts with
     * @return the file chosen, or empty when the user canceled or dialogs are not supported
     */
    CompletableFuture<Optional<Path>> save(FileDialog dialog);

    /**
     * Asks for a folder. The dialog's filters do not apply.
     *
     * @param dialog what the dialog asks
     * @return the folder chosen, or empty when the user canceled or dialogs are not supported
     */
    CompletableFuture<Optional<Path>> folder(FileDialog dialog);
}
