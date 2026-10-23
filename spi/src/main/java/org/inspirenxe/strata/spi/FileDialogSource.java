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
package org.inspirenxe.strata.spi;

import org.inspirenxe.strata.api.file.FileDialog;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The engine's file dialogs, which core offers as {@code ComponentFactory.fileDialogs()}. An adapter whose engine has
 * native dialogs returns one from {@link Platform#fileDialogs()}, such as {@code java.awt.FileDialog} on a desktop.
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>Core calls each method on the UI thread. Return at once: show the dialog without blocking that thread, or
 *     show it on another thread, and complete the future when the user chooses.</li>
 *     <li>The future may complete on any thread; core hands its result to the UI thread before the application sees
 *     it.</li>
 *     <li>Complete with an empty result when the user cancels, and exceptionally only when the dialog failed.</li>
 *     <li>Honor the dialog's title, start and filters where the engine can, and a save dialog's suggested name.
 *     Filters the engine cannot show may be left out.</li>
 * </ul>
 */
public interface FileDialogSource {

    /**
     * Shows a dialog for one file to open.
     *
     * @param dialog what the dialog asks
     * @return the file chosen, or empty when the user canceled
     */
    CompletableFuture<Optional<Path>> open(FileDialog dialog);

    /**
     * Shows a dialog for one or more files to open.
     *
     * @param dialog what the dialog asks
     * @return the files chosen, or an empty list when the user canceled
     */
    CompletableFuture<List<Path>> openMany(FileDialog dialog);

    /**
     * Shows a dialog for where to save a file.
     *
     * @param dialog what the dialog asks
     * @return the file chosen, or empty when the user canceled
     */
    CompletableFuture<Optional<Path>> save(FileDialog dialog);

    /**
     * Shows a dialog for a folder.
     *
     * @param dialog what the dialog asks
     * @return the folder chosen, or empty when the user canceled
     */
    CompletableFuture<Optional<Path>> folder(FileDialog dialog);
}
