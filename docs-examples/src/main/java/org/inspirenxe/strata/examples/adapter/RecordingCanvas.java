package org.inspirenxe.strata.examples.adapter;

// --8<-- [start:canvas]
import org.inspirenxe.strata.api.component.CursorKind;
import org.inspirenxe.strata.api.geom.Insets;
import org.inspirenxe.strata.api.geom.Rect;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.spi.Canvas;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A canvas for an imaginary engine that takes a list of draw commands each frame. It keeps its own translation
 * and clip stacks, since the commands use absolute coordinates.
 */
public final class RecordingCanvas implements Canvas {
    private final List<String> commands = new ArrayList<>();
    private final Deque<float[]> translations = new ArrayDeque<>();
    private final Deque<Rect> clips = new ArrayDeque<>();
    private float dx;
    private float dy;
    private CursorKind cursor = CursorKind.ARROW;

    /** Starts a frame. A frame without a cursor request shows the default cursor. */
    public void begin() {
        this.commands.clear();
        this.cursor = CursorKind.ARROW;
    }

    /** Returns this frame's commands, for the engine to draw in order. */
    public List<String> commands() {
        return this.commands;
    }

    /** Returns the cursor to show after this frame. */
    public CursorKind cursor() {
        return this.cursor;
    }

    @Override
    public void fill(final Rect rect, final int argb) {
        if ((argb >>> 24) != 0) {
            this.commands.add("fill " + this.place(rect) + " " + Integer.toHexString(argb));
        }
    }

    @Override
    public void fillGradient(final Rect rect, final int topArgb, final int bottomArgb) {
        this.commands.add("gradient " + this.place(rect) + " " + Integer.toHexString(topArgb) + " "
                + Integer.toHexString(bottomArgb));
    }

    @Override
    public void sprite(final SpriteId sprite, final Rect rect, final int tint) {
        // An engine without the image would draw a placeholder here, never throw
        this.commands.add("sprite " + sprite + " " + this.place(rect));
    }

    @Override
    public void nineSlice(final SpriteId sprite, final Rect rect, final Insets border, final int tint) {
        this.commands.add("nineslice " + sprite + " " + this.place(rect) + " " + border);
    }

    @Override
    public void text(final TextSpan span, final float x, final float y) {
        this.commands.add("text '" + span.text() + "' at " + (x + this.dx) + "," + (y + this.dy));
    }

    @Override
    public void pushClip(final Rect rect) {
        // A clip applies in the current transform and intersects the clip already in place
        final Rect placed = this.place(rect);
        this.clips.push(this.clips.isEmpty() ? placed : this.clips.peek().intersect(placed));
        this.commands.add("clip " + this.clips.peek());
    }

    @Override
    public void popClip() {
        this.clips.pop();
        this.commands.add(this.clips.isEmpty() ? "unclip" : "clip " + this.clips.peek());
    }

    @Override
    public void pushTranslation(final float x, final float y) {
        this.translations.push(new float[] {this.dx, this.dy});
        this.dx += x;
        this.dy += y;
    }

    @Override
    public void popTransform() {
        final float[] previous = this.translations.pop();
        this.dx = previous[0];
        this.dy = previous[1];
    }

    @Override
    public void nextLayer() {
        this.commands.add("layer");
    }

    @Override
    public void requestCursor(final CursorKind cursor) {
        this.cursor = cursor;
    }

    private Rect place(final Rect rect) {
        return rect.translate(this.dx, this.dy);
    }
}
// --8<-- [end:canvas]
