package org.inspirenxe.strata.api.text;

/**
 * How a span of text looks. Immutable; the {@code with} methods return modified copies.
 *
 * <p>Engines draw what they can: an engine with one font weight may fake or ignore bold. An adapter may use
 * the engine's own bold, italic, underline, strikethrough and shadow; the Swing adapter derives bold and italic
 * fonts and draws the lines and shadow itself.</p>
 *
 * @param color the color as ARGB; alpha 0 draws nothing
 * @param bold whether the text is bold
 * @param italic whether the text is italic
 * @param underline whether a line is drawn under the text
 * @param strikethrough whether a line is drawn through the text
 * @param shadow whether a darker copy is drawn one unit down and to the right
 */
public record TextStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean shadow) {
    /**
     * Opaque white, no decoration, no shadow.
     */
    public static final TextStyle DEFAULT = new TextStyle(0xFFFFFFFF, false, false, false, false, false);

    /**
     * Returns a copy with another color.
     *
     * @param color the color as ARGB
     * @return the new style
     */
    public TextStyle withColor(final int color) {
        return new TextStyle(color, this.bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with bold set or cleared.
     *
     * @param bold true for bold
     * @return the new style
     */
    public TextStyle withBold(final boolean bold) {
        return new TextStyle(this.color, bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with italic set or cleared.
     *
     * @param italic true for italic
     * @return the new style
     */
    public TextStyle withItalic(final boolean italic) {
        return new TextStyle(this.color, this.bold, italic, this.underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with underline set or cleared.
     *
     * @param underline true to underline
     * @return the new style
     */
    public TextStyle withUnderline(final boolean underline) {
        return new TextStyle(this.color, this.bold, this.italic, underline, this.strikethrough, this.shadow);
    }

    /**
     * Returns a copy with strikethrough set or cleared.
     *
     * @param strikethrough true to strike through
     * @return the new style
     */
    public TextStyle withStrikethrough(final boolean strikethrough) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, strikethrough, this.shadow);
    }

    /**
     * Returns a copy with the shadow set or cleared.
     *
     * @param shadow true to draw a shadow
     * @return the new style
     */
    public TextStyle withShadow(final boolean shadow) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, this.strikethrough, shadow);
    }
}
