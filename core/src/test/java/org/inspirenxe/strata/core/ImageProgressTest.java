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
import org.inspirenxe.strata.api.control.display.ProgressBar;
import org.inspirenxe.strata.api.layout.Orientation;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.theme.Themes;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Images that cover their bounds or keep their size, and vertical progress bars (design 4.57).
 */
class ImageProgressTest {
    private static final SpriteId BANNER = SpriteId.of("example", "banner");

    private final TestHost test = new TestHost(300, 300);
    private final ComponentFactory ui = this.test.factory();

    private List<String> render() {
        final DrawLog log = new DrawLog();
        this.test.host().render(log);
        return log.lines();
    }

    private Image show(final Image.Scaling scaling) {
        final Image image = this.ui.image(BANNER).setImageSize(40, 20).setScaling(scaling);
        image.setSize(20, 20);
        this.test.show(this.ui.absolutePanel().add(image, 10, 10));
        return image;
    }

    @Test
    void coverFillsTheBoundsKeepingTheAspectAndCutsTheRest() {
        this.show(Image.Scaling.COVER);
        final List<String> lines = this.render();
        assertTrue(lines.contains("sprite example:banner 0,10 40x20 #FFFFFFFF"), lines.toString());
    }

    @Test
    void noneDrawsTheImageSizeCenteredAndCut() {
        final Image image = this.show(Image.Scaling.NONE);
        image.setSize(60, 60);
        final List<String> lines = this.render();
        assertTrue(lines.contains("sprite example:banner 20,30 40x20 #FFFFFFFF"), lines.toString());
        image.setSize(20, 60);
        assertTrue(this.render().contains("sprite example:banner 0,30 40x20 #FFFFFFFF"));
    }

    @Test
    void aVerticalProgressBarFillsFromTheBottom() {
        final ProgressBar bar = this.ui.progressBar().setOrientation(Orientation.VERTICAL);
        bar.setValue(0.25);
        this.test.show(this.ui.absolutePanel().add(bar, 10, 10));
        this.test.host().layout();
        assertEquals(Orientation.VERTICAL, bar.orientation());
        assertEquals(100f, bar.bounds().height());
        final float width = bar.bounds().width();
        final int fill = ((Paint.Solid) Themes.flat().paint(ProgressBar.STYLE_FILL).orElseThrow()).argb();
        final List<String> lines = this.render();
        final String expected = "fill 10,85 " + (int) width + "x25 " + String.format("#%08X", fill);
        assertTrue(lines.contains(expected), expected + " in " + lines);
    }

    @Test
    void aVerticalIndeterminateSegmentRisesFromTheBottom() {
        final ProgressBar bar = this.ui.progressBar().setOrientation(Orientation.VERTICAL).setIndeterminate(true);
        bar.setSize(10, 100);
        this.test.show(this.ui.absolutePanel().add(bar, 0, 0));
        this.test.platform().advanceMillis(15L * 1500L + 150L);
        final int segment = ((Paint.Solid) Themes.flat().paint(ProgressBar.STYLE_SEGMENT)
                .or(() -> Themes.flat().paint(ProgressBar.STYLE_FILL)).orElseThrow()).argb();
        final String color = String.format("#%08X", segment);
        final List<String> lines = this.render().stream().filter(line -> line.endsWith(color)).toList();
        assertEquals(1, lines.size(), lines.toString());
        final String[] size = lines.getFirst().split(" ")[2].split("x");
        assertEquals("10", size[0]);
        final String[] at = lines.getFirst().split(" ")[1].split(",");
        assertTrue(Float.parseFloat(at[1]) + Float.parseFloat(size[1]) >= 95f, lines.toString());
    }
}
