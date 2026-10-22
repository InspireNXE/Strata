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
package org.inspirenxe.strata.api.visual;

import java.util.Arrays;

/**
 * A grid of ARGB pixels that a project writes and any engine draws, such as a heat map, a chart rendered by hand or
 * a picture decoded from bytes. Draw it with {@link org.inspirenxe.strata.api.extension.Painter#pixels}.
 *
 * <p>Write it on the UI thread, then call {@link #changed()}, which moves {@link #version()} on. An engine keeps its
 * own copy of the pixels, such as a texture, and copies them again when it draws a buffer whose version moved, so
 * writes without {@code changed()} may not show. Pixels start transparent black ({@code 0x00000000}).</p>
 *
 * <p>Two buffers are equal only when they are the same object.</p>
 */
public final class PixelBuffer {
    /** The most pixels one buffer may hold, so its array always fits. */
    public static final int MAX_PIXELS = 1 << 28;

    private final int width;
    private final int height;
    private final int[] argb;
    private long version;

    private PixelBuffer(final int width, final int height) {
        this.width = width;
        this.height = height;
        this.argb = new int[width * height];
    }

    /**
     * Creates a buffer of transparent black pixels.
     *
     * @param width the width in pixels, at least 1
     * @param height the height in pixels, at least 1
     * @return the buffer, at version 0
     * @throws IllegalArgumentException if a side is below 1 or the buffer would hold more than {@link #MAX_PIXELS}
     */
    public static PixelBuffer of(final int width, final int height) {
        if (width < 1 || height < 1 || (long) width * height > MAX_PIXELS) {
            throw new IllegalArgumentException("A pixel buffer needs 1 to " + MAX_PIXELS + " pixels: " + width + " x "
                    + height);
        }
        return new PixelBuffer(width, height);
    }

    /**
     * Returns the width.
     *
     * @return the width in pixels
     */
    public int width() {
        return this.width;
    }

    /**
     * Returns the height.
     *
     * @return the height in pixels
     */
    public int height() {
        return this.height;
    }

    /**
     * Returns one pixel.
     *
     * @param x the column, from 0 at the left
     * @param y the row, from 0 at the top
     * @return the pixel as ARGB
     * @throws IndexOutOfBoundsException if the pixel is outside the buffer
     */
    public int get(final int x, final int y) {
        return this.argb[this.index(x, y)];
    }

    /**
     * Sets one pixel. Call {@link #changed()} when done writing.
     *
     * @param x the column, from 0 at the left
     * @param y the row, from 0 at the top
     * @param argb the pixel as ARGB
     * @return this buffer
     * @throws IndexOutOfBoundsException if the pixel is outside the buffer
     */
    public PixelBuffer set(final int x, final int y, final int argb) {
        this.argb[this.index(x, y)] = argb;
        return this;
    }

    /**
     * Sets every pixel to one color. Call {@link #changed()} when done writing.
     *
     * @param argb the color
     * @return this buffer
     */
    public PixelBuffer fill(final int argb) {
        Arrays.fill(this.argb, argb);
        return this;
    }

    /**
     * Returns the array that holds the pixels, row by row from the top, each row from the left, so pixel
     * {@code (x, y)} is at {@code y * width() + x}. Writing it is the same as {@link #set}, and faster for a whole
     * picture: call {@link #changed()} when done. Engines read it to copy the pixels.
     *
     * @return the buffer's own array, {@code width() * height()} long
     */
    public int[] array() {
        return this.argb;
    }

    /**
     * Says the pixels were written, so the next draw copies them to the engine again.
     *
     * @return this buffer
     */
    public PixelBuffer changed() {
        this.version++;
        return this;
    }

    /**
     * Returns how many times {@link #changed()} was called. Engines compare it with the version they copied.
     *
     * @return the version, 0 for a new buffer
     */
    public long version() {
        return this.version;
    }

    private int index(final int x, final int y) {
        if (x < 0 || y < 0 || x >= this.width || y >= this.height) {
            throw new IndexOutOfBoundsException("Pixel " + x + ", " + y + " is outside " + this.width + " x "
                    + this.height);
        }
        return y * this.width + x;
    }

    @Override
    public String toString() {
        return "PixelBuffer[" + this.width + " x " + this.height + ", version " + this.version + "]";
    }
}
