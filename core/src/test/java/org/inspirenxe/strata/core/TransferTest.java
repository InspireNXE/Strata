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
import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.input.Key;
import org.inspirenxe.strata.api.input.KeyAction;
import org.inspirenxe.strata.api.input.Modifier;
import org.inspirenxe.strata.api.input.Modifiers;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Themes;
import org.inspirenxe.strata.api.transfer.DataFormat;
import org.inspirenxe.strata.api.transfer.Transfer;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.spi.Clipboard;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transfers, data formats and the clipboard that holds them (design 4.48).
 */
class TransferTest {
    record Listing(String seller, int price) {
    }

    private static final DataFormat<Listing> LISTING = DataFormat.local("test:listing", Listing.class);

    @Test
    void formatsAreComparedByIdentityExceptMimeTypes() {
        assertNotSame(DataFormat.local("a", String.class), DataFormat.local("a", String.class));
        assertSame(DataFormat.mime("text/csv"), DataFormat.mime("TEXT/CSV"));
        assertSame(DataFormat.TEXT, DataFormat.mime("text/plain"));
        assertEquals(Optional.of("text/csv"), DataFormat.mime("text/csv").mimeType());
        assertEquals(Optional.empty(), DataFormat.FILES.mimeType());
        assertTrue(LISTING.isLocal());
        assertFalse(DataFormat.TEXT.isLocal());
        assertFalse(DataFormat.IMAGE.isLocal());
        for (final String bad : List.of("text", "/csv", "text/", "text/csv/x", "text/csv; charset=utf-8", "te xt/csv")) {
            assertThrows(IllegalArgumentException.class, () -> DataFormat.mime(bad), bad);
        }
    }

    @Test
    void aTransferHoldsEachFormatInOrder() {
        final Listing listing = new Listing("Ana", 12);
        final PixelBuffer image = PixelBuffer.of(2, 2);
        final Transfer transfer = Transfer.builder()
                .add(DataFormat.TEXT, "Ana 12")
                .add(LISTING, listing)
                .add(DataFormat.IMAGE, image)
                .add(DataFormat.TEXT, "Ana, 12")
                .build();

        assertEquals(List.of(DataFormat.TEXT, LISTING, DataFormat.IMAGE), new ArrayList<>(transfer.formats()));
        assertEquals(Optional.of("Ana, 12"), transfer.get(DataFormat.TEXT));
        assertSame(listing, transfer.get(LISTING).orElseThrow());
        assertSame(image, transfer.get(DataFormat.IMAGE).orElseThrow());
        assertEquals(Optional.empty(), transfer.get(DataFormat.FILES));
        assertFalse(transfer.has(DataFormat.STYLED_TEXT));
        assertTrue(Transfer.builder().build().isEmpty());
        assertSame(Transfer.empty(), Transfer.builder().build());
        assertEquals("Transfer[text/plain, test:listing, strata:image]", transfer.toString());
    }

    @Test
    void valuesMustBeOfTheFormatsType() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        final DataFormat<Object> text = (DataFormat) DataFormat.TEXT;
        assertThrows(IllegalArgumentException.class, () -> Transfer.builder().add(text, 12));
        assertThrows(IllegalArgumentException.class, () -> Transfer.files(List.of()));
        @SuppressWarnings({"unchecked", "rawtypes"})
        final DataFormat<List<Object>> files = (DataFormat) DataFormat.FILES;
        assertThrows(IllegalArgumentException.class, () -> Transfer.builder().add(files, List.of("a.txt")));
        assertThrows(NullPointerException.class, () -> Transfer.builder().add(DataFormat.TEXT, null));
    }

    @Test
    void filesAreCopied() {
        final List<Path> list = new ArrayList<>(List.of(Path.of("a.txt")));
        final Transfer transfer = Transfer.files(list);
        list.add(Path.of("b.txt"));
        assertEquals(List.of(Path.of("a.txt")), transfer.get(DataFormat.FILES).orElseThrow());
    }

    @Test
    void aSuppliedValueIsReadWhenAskedForAndKeptOnceRead() {
        final AtomicInteger calls = new AtomicInteger();
        final String[] value = {null};
        final Transfer transfer = Transfer.builder().supply(DataFormat.TEXT, () -> {
            calls.incrementAndGet();
            return value[0];
        }).build();

        assertTrue(transfer.has(DataFormat.TEXT));
        assertEquals(0, calls.get());
        assertEquals(Optional.empty(), transfer.get(DataFormat.TEXT));
        value[0] = "dropped";
        assertEquals(Optional.of("dropped"), transfer.get(DataFormat.TEXT));
        value[0] = "later";
        assertEquals(Optional.of("dropped"), transfer.get(DataFormat.TEXT));
        assertEquals(2, calls.get());

        final Transfer throwing = Transfer.builder().supply(DataFormat.TEXT, () -> {
            throw new IllegalStateException("not yet");
        }).build();
        assertEquals(Optional.empty(), throwing.get(DataFormat.TEXT));
    }

    @Test
    void onlyKeepsTheFormatsThatPass() {
        final Transfer transfer = Transfer.builder().add(DataFormat.TEXT, "a").add(LISTING, new Listing("Ben", 7)).build();
        final Transfer outside = transfer.only(format -> !format.isLocal());
        assertEquals(List.of(DataFormat.TEXT), new ArrayList<>(outside.formats()));
        assertSame(transfer, transfer.only(format -> true));
        assertSame(Transfer.empty(), transfer.only(format -> false));
    }

    // ------------------------------------------------------------------
    // The clipboard
    // ------------------------------------------------------------------

    private final TestHost test = new TestHost(100, 100);
    private final ComponentFactory ui = this.test.factory();

    @Test
    void localFormatsStayInTheProcessAndComeBackWhole() {
        final Listing listing = new Listing("Cleo", 30);
        final Transfer rows = Transfer.builder().add(DataFormat.TEXT, "Cleo\t30").add(LISTING, listing).build();
        this.ui.setClipboard(rows);

        assertEquals(Optional.of("Cleo\t30"), this.test.platform().clipboard().get());
        assertSame(rows, this.ui.clipboard().orElseThrow());
        assertSame(listing, this.ui.clipboard().orElseThrow().get(LISTING).orElseThrow());
        assertEquals(Optional.of("Cleo\t30"), ((ComponentFactoryImpl) this.ui).environment().clipboardText());
    }

    @Test
    void anotherProgramReplacingTheTextDropsTheHeldTransfer() {
        this.ui.setClipboard(Transfer.builder().add(DataFormat.TEXT, "Dev").add(LISTING, new Listing("Dev", 4)).build());
        this.test.platform().clipboard().set("from elsewhere");

        final Transfer read = this.ui.clipboard().orElseThrow();
        assertEquals(Optional.of("from elsewhere"), read.get(DataFormat.TEXT));
        assertFalse(read.has(LISTING));
    }

    @Test
    void anEmptyClipboardReadsAsEmpty() {
        assertEquals(Optional.empty(), this.ui.clipboard());
    }

    @Test
    void theEngineGetsEveryFormatButTheLocalOnes() {
        final List<Transfer> written = new ArrayList<>();
        final TestPlatform platform = new TestPlatform();
        final Clipboard text = platform.clipboard();
        platform.setClipboard(new Clipboard() {
            @Override
            public Optional<String> get() {
                return text.get();
            }

            @Override
            public void set(final String value) {
                text.set(value);
            }

            @Override
            public void setTransfer(final Transfer transfer) {
                written.add(transfer);
                Clipboard.super.setTransfer(transfer);
            }
        });
        final TestHost host = new TestHost(platform, Themes.flat(), 100, 100);
        host.factory().setClipboard(Transfer.builder().add(DataFormat.TEXT, "Ana").add(LISTING, new Listing("Ana", 1))
                .add(DataFormat.STYLED_TEXT, Text.of("Ana")).build());

        assertEquals(1, written.size());
        assertEquals(List.of(DataFormat.TEXT, DataFormat.STYLED_TEXT), new ArrayList<>(written.get(0).formats()));
        assertTrue(host.factory().clipboard().orElseThrow().has(LISTING));
    }

    @Test
    void textBoxesStillCopyAndPasteText() {
        this.ui.setClipboard(Transfer.builder().add(DataFormat.TEXT, "pasted").add(LISTING, new Listing("Ben", 2)).build());
        final TextBox box = this.ui.textBox();
        this.test.show(box);
        box.requestFocus();
        this.test.host().key(Key.V, KeyAction.PRESS, Modifiers.of(Modifier.CONTROL));
        assertEquals("pasted", box.text());
    }
}
