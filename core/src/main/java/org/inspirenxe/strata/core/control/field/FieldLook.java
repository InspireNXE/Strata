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

import org.inspirenxe.strata.api.control.field.TextBox;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;

/**
 * How the text fields draw their caret, selection and placeholder, read from the {@code textbox.*} keys that
 * {@link TextBoxImpl}, {@link PasswordTextBoxImpl} and {@link TextAreaImpl} share.
 *
 * @param selection the paint behind selected text
 * @param caret the paint of the caret bar
 * @param selectedText the style selected text is drawn in, or null to draw it as it is
 * @param caretExtend how far the caret bar reaches above and below the line, in UI units
 * @param selectionExtend how far the selection reaches above and below the line, in UI units
 * @param blinkMillis how long the caret stays shown and then hidden, in milliseconds
 * @param underscoreCaret whether a caret at the end of the text is drawn as an underscore in the text's style
 * @param hidePlaceholder whether the placeholder hides while the field has focus
 */
record FieldLook(Paint selection, Paint caret, TextStyle selectedText, float caretExtend, float selectionExtend,
                 long blinkMillis, boolean underscoreCaret, boolean hidePlaceholder) {
    /**
     * The look with no theme: the given paints and nothing else changed.
     *
     * @param selection the selection paint
     * @param caret the caret paint
     * @return the look
     */
    static FieldLook plain(final Paint selection, final Paint caret) {
        return new FieldLook(selection, caret, null, 0f, 0f, LineEditor.BLINK_MILLIS, false, false);
    }

    /**
     * Reads the look from a theme.
     *
     * @param theme the theme
     * @param selection the selection paint when the theme has none
     * @param caret the caret paint when the theme has none
     * @return the look
     */
    static FieldLook of(final Theme theme, final Paint selection, final Paint caret) {
        final float blink = theme.metricOr(TextBox.STYLE_CARET_BLINK, LineEditor.BLINK_MILLIS);
        return new FieldLook(
                theme.paintOr(TextBox.STYLE_SELECTION, selection),
                theme.paintOr(TextBox.STYLE_CARET, caret),
                theme.textStyle(TextBox.STYLE_TEXT_SELECTED).orElse(null),
                Math.max(0f, theme.metricOr(TextBox.STYLE_CARET_EXTEND, 0f)),
                Math.max(0f, theme.metricOr(TextBox.STYLE_SELECTION_EXTEND, 0f)),
                Math.max(1L, (long) blink),
                theme.metricOr(TextBox.STYLE_CARET_UNDERSCORE, 0f) != 0f,
                theme.metricOr(TextBox.STYLE_PLACEHOLDER_HIDE, 0f) != 0f);
    }
}
