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
import org.inspirenxe.strata.api.control.button.Button;
import org.inspirenxe.strata.api.layout.FlowDirection;
import org.inspirenxe.strata.api.layout.HorizontalAlignment;
import org.inspirenxe.strata.api.text.TextSpan;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Where a button puts its caption across its width.
 */
class CaptionAlignmentTest {
    private static final float WIDTH = 100;
    private static final float PADDING = 10;

    private final TestHost test = new TestHost(200, 100);
    private final ComponentFactory ui = this.test.factory();
    private final float captionWidth = this.test.platform().textMetrics().width(TextSpan.of("OK"));

    private Button button() {
        return this.ui.button("OK").setTextPadding(PADDING, 5, PADDING, 5).setWidth(WIDTH);
    }

    private List<String> render(final Component<?> button) {
        this.test.show(this.ui.absolutePanel().add(button, 0, 0));
        final DrawLog canvas = new DrawLog();
        this.test.host().render(canvas);
        return canvas.lines();
    }

    private float captionX(final Component<?> button) {
        final String text = this.render(button).stream().filter(line -> line.startsWith("text OK ")).findFirst()
                .orElseThrow();
        return Float.parseFloat(text.split(" ")[2].split(",")[0]);
    }

    @Test
    void centersByDefault() {
        final Button button = this.button();
        assertEquals(HorizontalAlignment.CENTER, button.captionAlignment());
        assertEquals(PADDING + (float) (Math.floor((WIDTH - 2 * PADDING) / 2) - Math.floor(this.captionWidth / 2)),
                this.captionX(button));
    }

    @Test
    void putsTheCaptionAtTheLeftOrTheRightInsideThePadding() {
        assertEquals(PADDING, this.captionX(this.button().setCaptionAlignment(HorizontalAlignment.LEFT)));
        assertEquals(WIDTH - PADDING - this.captionWidth,
                this.captionX(this.button().setCaptionAlignment(HorizontalAlignment.RIGHT)));
    }

    @Test
    void stretchCountsAsLeft() {
        assertEquals(PADDING, this.captionX(this.button().setCaptionAlignment(HorizontalAlignment.STRETCH)));
    }

    @Test
    void leftMeansTheStartRightToLeft() {
        final Button button = this.button().setCaptionAlignment(HorizontalAlignment.LEFT)
                .setFlowDirection(FlowDirection.RIGHT_TO_LEFT);
        assertEquals(WIDTH - PADDING - this.captionWidth, this.captionX(button));
    }

    @Test
    void movesTheIconWithTheCaption() {
        final Button button = this.button().setIcon(SpriteId.of("test", "icon")).setIconSize(8, 8)
                .setCaptionAlignment(HorizontalAlignment.LEFT);
        final List<String> lines = this.render(button);
        assertEquals(1, lines.stream().filter(line -> line.startsWith("sprite test:icon 10,")).count(),
                String.join("\n", lines));
    }

    @Test
    void toggleAndDropDownButtonsAlignTheirCaptionsToo() {
        assertEquals(PADDING, this.captionX(this.ui.toggleButton("OK").setTextPadding(PADDING, 5, PADDING, 5)
                .setWidth(WIDTH).setCaptionAlignment(HorizontalAlignment.LEFT)));
        assertEquals(PADDING, this.captionX(this.ui.dropDownButton("OK").setTextPadding(PADDING, 5, PADDING, 5)
                .setWidth(WIDTH).setCaptionAlignment(HorizontalAlignment.LEFT)));
    }
}
