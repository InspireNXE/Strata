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
package org.inspirenxe.strata.core.text;

import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextMetrics;
import org.inspirenxe.strata.api.text.TextStyle;

/**
 * A one-line caption that parses its mnemonic, as {@link Mnemonics} describes, measured twice: once with its
 * markers taken out, and once with its mnemonic's letter underlined. Both measure the same width, since an
 * underline takes no room, so a control picks one when it paints with no new layout.
 *
 * @param plain the caption without markers
 * @param marked the caption with its mnemonic underlined; the same line as {@code plain} when it has none
 * @param mnemonic the mnemonic in lower case, or {@link Mnemonics#NONE}
 */
public record MnemonicLine(TextLine plain, TextLine marked, int mnemonic) {
    /**
     * An empty caption.
     */
    public static final MnemonicLine EMPTY = new MnemonicLine(TextLine.EMPTY, TextLine.EMPTY, Mnemonics.NONE);

    /**
     * Resolves, parses and measures a caption.
     *
     * @param text the caption
     * @param style the style plain parts are drawn in
     * @param metrics the engine's text metrics
     * @return the measured caption
     */
    public static MnemonicLine of(final Text text, final TextStyle style, final TextMetrics metrics) {
        final Mnemonics parsed = Mnemonics.parse(text.resolve(style));
        final TextLine plain = TextLine.of(parsed.plain(), style, metrics);
        final TextLine marked = parsed.mnemonic() == Mnemonics.NONE ? plain
                : TextLine.of(parsed.marked(), style, metrics);
        return new MnemonicLine(plain, marked, parsed.mnemonic());
    }

    /**
     * Returns the line to draw.
     *
     * @param showMnemonic true while mnemonics show
     * @return the marked line while mnemonics show, else the plain one
     */
    public TextLine line(final boolean showMnemonic) {
        return showMnemonic ? this.marked : this.plain;
    }
}
