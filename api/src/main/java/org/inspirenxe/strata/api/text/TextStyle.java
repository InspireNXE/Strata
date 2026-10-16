package org.inspirenxe.strata.api.text;

public record TextStyle(int color, boolean bold, boolean italic, boolean underline, boolean strikethrough, boolean shadow) {
    public static final TextStyle DEFAULT = new TextStyle(0xFFFFFFFF, false, false, false, false, false);

    public TextStyle withColor(final int color) {
        return new TextStyle(color, this.bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    public TextStyle withBold(final boolean bold) {
        return new TextStyle(this.color, bold, this.italic, this.underline, this.strikethrough, this.shadow);
    }

    public TextStyle withItalic(final boolean italic) {
        return new TextStyle(this.color, this.bold, italic, this.underline, this.strikethrough, this.shadow);
    }

    public TextStyle withUnderline(final boolean underline) {
        return new TextStyle(this.color, this.bold, this.italic, underline, this.strikethrough, this.shadow);
    }

    public TextStyle withStrikethrough(final boolean strikethrough) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, strikethrough, this.shadow);
    }

    public TextStyle withShadow(final boolean shadow) {
        return new TextStyle(this.color, this.bold, this.italic, this.underline, this.strikethrough, shadow);
    }
}
