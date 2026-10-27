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
import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.extension.CustomComponent;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Sampling;
import org.inspirenxe.strata.core.paint.CanvasPainter;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Choosing how sprites and pixel buffers are sampled.
 */
class SamplingTest {
    private static final SpriteId PHOTO = SpriteId.of("test", "photo");

    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();

    private List<String> render(final Component<?> content) {
        this.test.show(this.ui.absolutePanel().add(content, 0, 0));
        final SamplingLog canvas = new SamplingLog();
        this.test.host().render(canvas);
        return canvas.lines().stream().filter(line -> line.startsWith("sampling") || line.startsWith("sprite")
                || line.startsWith("pixels")).toList();
    }

    @Test
    void anImageSamplesNearestByDefaultWithoutAsking() {
        final Image image = this.ui.image(PHOTO).setImageSize(10, 10);
        assertEquals(Sampling.NEAREST, image.sampling());
        assertEquals(List.of("sprite test:photo 0,0 10x10 #FFFFFFFF"), this.render(image));
    }

    @Test
    void aLinearImageWrapsItsSpriteInAPushAndAPop() {
        final Image image = this.ui.image(PHOTO).setImageSize(10, 10).setSampling(Sampling.LINEAR);
        assertEquals(List.of("sampling LINEAR", "sprite test:photo 0,0 10x10 #FFFFFFFF", "sampling pop"),
                this.render(image));
    }

    @Test
    void aLinearImageSamplesItsPixelBufferTheSameWay() {
        final Image image = this.ui.image().setPixels(PixelBuffer.of(4, 2)).setSampling(Sampling.LINEAR);
        assertEquals(List.of("sampling LINEAR", "pixels 4x2", "sampling pop"), this.render(image));
    }

    @Test
    void aPainterPopsEvenWhenTheDrawingThrows() {
        final SamplingLog canvas = new SamplingLog();
        final CanvasPainter painter = new CanvasPainter().begin(canvas);
        assertThrows(IllegalStateException.class, () -> painter.sampled(Sampling.LINEAR, () -> {
            throw new IllegalStateException("drawing failed");
        }));
        assertEquals(List.of("sampling LINEAR", "sampling pop"), canvas.lines());
    }

    @Test
    void aCustomComponentChoosesForWhatItDraws() {
        final Component<?> photo = new Photo(this.ui);
        assertEquals(List.of("sampling LINEAR", "sprite test:photo 0,0 20x10 #FFFFFFFF", "sampling pop",
                "sprite test:photo 0,0 20x10 #FFFFFFFF"), this.render(photo));
    }

    /**
     * Draws a photo smoothly and then again as pixel art, as a developer would.
     */
    static final class Photo extends CustomComponent<Photo> {
        Photo(final ComponentFactory ui) {
            super(ui);
        }

        @Override
        protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
            return new Size(20, 10);
        }

        @Override
        protected void paint(final Painter painter, final Rect bounds) {
            painter.sampled(Sampling.LINEAR, () -> painter.sprite(PHOTO, bounds, 0xFFFFFFFF));
            painter.sprite(PHOTO, bounds, 0xFFFFFFFF);
        }
    }

    /**
     * A draw log that also logs sampling and pixel buffers.
     */
    static final class SamplingLog extends DrawLog {
        @Override
        public void pushSampling(final Sampling sampling) {
            this.lines().add("sampling " + sampling);
        }

        @Override
        public void popSampling() {
            this.lines().add("sampling pop");
        }

        @Override
        public void pixels(final PixelBuffer buffer, final Rect rect, final int tint) {
            this.lines().add("pixels " + buffer.width() + "x" + buffer.height());
        }
    }
}
