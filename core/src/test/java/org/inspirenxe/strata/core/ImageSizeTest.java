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
import org.inspirenxe.strata.api.control.display.Image;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.PixelBuffer;
import org.inspirenxe.strata.api.visual.Visual;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * An image taking its size from what it shows until the developer sets one.
 */
class ImageSizeTest {
    private static final SpriteId PHOTO = SpriteId.of("test", "photo");
    private static final SpriteId UNKNOWN = SpriteId.of("test", "unknown");

    private final TestHost test = new TestHost(800, 600);
    private final ComponentFactory ui = this.test.factory();

    ImageSizeTest() {
        this.test.host().setSpriteSizes(sprite -> PHOTO.equals(sprite) ? Optional.of(new Size(64, 32))
                : Optional.empty());
    }

    private Size shown(final Image image) {
        this.test.show(this.ui.absolutePanel().add(image, 0, 0));
        return new Size(image.bounds().width(), image.bounds().height());
    }

    @Test
    void takesAPixelBuffersSizeOnePixelToAUnit() {
        final Image image = this.ui.image().setPixels(PixelBuffer.of(320, 180));
        assertEquals(320, image.imageWidth());
        assertEquals(180, image.imageHeight());
        assertEquals(new Size(320, 180), this.shown(image));
    }

    @Test
    void keepsTheSizeWhenShowingNothing() {
        final Image image = this.ui.image().setPixels(PixelBuffer.of(30, 20)).setPixels(null);
        assertEquals(new Size(30, 20), this.shown(image));
    }

    @Test
    void takesASpritesSizeFromTheHostOnceShown() {
        final Image image = this.ui.image(PHOTO);
        assertEquals(new Size(64, 32), this.shown(image));
        assertEquals(64, image.imageWidth());
        assertEquals(32, image.imageHeight());
    }

    @Test
    void sizesASpriteTheHostDoesNotKnowSixteenBySixteen() {
        final Image image = this.ui.image().setPixels(PixelBuffer.of(30, 20)).setSprite(UNKNOWN);
        assertEquals(new Size(16, 16), this.shown(image));
    }

    @Test
    void measuresAgainWhenTheHostsSpriteSizesChange() {
        final Image image = this.ui.image(PHOTO);
        this.shown(image);
        this.test.host().setSpriteSizes(sprite -> Optional.of(new Size(10, 40)));
        this.test.host().layout();
        assertEquals(10, image.bounds().width());
        assertEquals(40, image.bounds().height());
    }

    @Test
    void aSizeSetByHandWinsOverSpritesAndPixels() {
        final Image image = this.ui.image().setImageSize(40, 30).setPixels(PixelBuffer.of(320, 180));
        assertEquals(new Size(40, 30), this.shown(image));
        image.setSprite(PHOTO);
        this.test.host().layout();
        assertEquals(40, image.bounds().width());
        assertEquals(30, image.bounds().height());
    }

    @Test
    void aVisualBringsItsSizeAndTheSizeFollowsTheSourceFromThere() {
        final Visual visual = () -> new Size(24, 12);
        final Image image = this.ui.image().setImageSize(40, 30).setVisual(visual);
        assertEquals(24, image.imageWidth());
        image.setPixels(PixelBuffer.of(8, 6));
        assertEquals(new Size(8, 6), this.shown(image));
    }
}
