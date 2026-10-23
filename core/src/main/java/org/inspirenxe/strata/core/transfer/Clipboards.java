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
package org.inspirenxe.strata.core.transfer;

import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.spi.Clipboard;
import org.inspirenxe.strata.spi.Platform;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Reads and writes a platform's clipboard in transfers. Each platform keeps the last transfer written through here,
 * with the text its clipboard returned right after the write. A read hands that whole transfer back, local formats
 * included, while the clipboard still returns the same text, and otherwise reads the engine's clipboard and forgets
 * it. The engine never sees a local format. Platforms are held weakly, so a closed adapter's transfer goes with it.
 * Safe from any thread.
 */
public final class Clipboards {
    private static final Map<Platform, Held> HELD = new WeakHashMap<>();

    private Clipboards() {
    }

    /**
     * Reads a platform's clipboard.
     *
     * @param platform the platform
     * @return the last transfer written here while the engine still holds its text, else what the engine reads, or
     *     empty when it reads nothing
     */
    public static Optional<Transfer> read(final Platform platform) {
        final Clipboard clipboard = platform.clipboard();
        final Held held;
        synchronized (HELD) {
            held = HELD.get(platform);
        }
        if (held != null) {
            if (clipboard.get().equals(held.marker())) {
                return held.transfer().isEmpty() ? Optional.empty() : Optional.of(held.transfer());
            }
            synchronized (HELD) {
                HELD.remove(platform, held);
            }
        }
        return clipboard.getTransfer().filter(transfer -> !transfer.isEmpty());
    }

    /**
     * Writes a transfer to a platform's clipboard: every format but the local ones goes to the engine, and the
     * whole transfer is kept for {@link #read}.
     *
     * @param platform the platform
     * @param transfer the transfer
     */
    public static void write(final Platform platform, final Transfer transfer) {
        Objects.requireNonNull(transfer, "transfer");
        final Clipboard clipboard = platform.clipboard();
        clipboard.setTransfer(transfer.only(format -> !format.isLocal()));
        // What the engine holds now: the transfer's text, or what it held before when the transfer has none it takes
        final Optional<String> marker = clipboard.get();
        synchronized (HELD) {
            HELD.put(platform, new Held(transfer, marker));
        }
    }

    /**
     * A transfer written, and the engine's text right after.
     *
     * @param transfer the whole transfer
     * @param marker the {@link DataFormat#TEXT} the clipboard returned after the write
     */
    private record Held(Transfer transfer, Optional<String> marker) {
    }
}
