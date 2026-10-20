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
package org.inspirenxe.strata.core.control.display;

import org.inspirenxe.strata.api.control.display.BusyIndicator;
import org.inspirenxe.strata.api.extension.MeasureContext;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.geometry.Size;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.text.TextStyle;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.BuiltInComponent;
import org.inspirenxe.strata.core.node.ComponentFactoryImpl;
import org.inspirenxe.strata.core.text.TextLine;

import java.util.Objects;
import java.util.Set;

/**
 * The core {@link BusyIndicator}. The lit dot follows the frames 0, 1, 2, 1 and repeats, one frame every
 * {@link BusyIndicator#FRAME_MILLIS} milliseconds of the environment's clock. The caption is measured during layout
 * and the dots' rectangles are worked out again only when the bounds change, so painting allocates nothing.
 */
public final class BusyIndicatorImpl extends BuiltInComponent<BusyIndicator> implements BusyIndicator {
    private static final Set<String> REQUIRED = Set.of(STYLE_DOT, STYLE_DOT_ACTIVE);
    private static final int[] FRAMES = {0, 1, 2, 1};
    private static final int DOT_COUNT = 3;
    private static final float DOTS_WIDTH = DOT_COUNT * DOT_SIZE + (DOT_COUNT - 1) * DOT_GAP;

    private final Rect[] dots;
    private Rect dotBounds;
    private Text text;
    private Paint dotPaint;
    private Paint activePaint;
    private TextStyle textStyle;
    private TextLine line;
    private float textWidth;
    private float lineHeight;

    /**
     * Creates an indicator with no caption.
     *
     * @param ui the factory that owns the indicator
     */
    public BusyIndicatorImpl(final ComponentFactoryImpl ui) {
        super(ui);
        this.dots = new Rect[DOT_COUNT];
        this.dotBounds = null;
        this.text = Text.empty();
        this.dotPaint = Paint.NONE;
        this.activePaint = Paint.NONE;
        this.textStyle = TextStyle.DEFAULT;
        this.line = TextLine.EMPTY;
    }

    @Override
    public Text text() {
        return this.text;
    }

    @Override
    public BusyIndicator setText(final Text text) {
        this.text = Objects.requireNonNull(text, "text");
        this.invalidateLayout();
        return this;
    }

    @Override
    protected Set<String> requiredStyleKeys() {
        return REQUIRED;
    }

    @Override
    protected void onApplyTheme(final Theme theme) {
        this.dotPaint = theme.paintOr(STYLE_DOT, Paint.NONE);
        this.activePaint = theme.paintOr(STYLE_DOT_ACTIVE, Paint.NONE);
        this.textStyle = theme.textStyleOr(STYLE_TEXT, TextStyle.DEFAULT);
    }

    @Override
    protected Size measure(final MeasureContext context, final float availableWidth, final float availableHeight) {
        this.line = TextLine.of(this.text, this.textStyle, context.text());
        if (this.line.isEmpty()) {
            this.textWidth = 0f;
            this.lineHeight = 0f;
            return new Size(DOTS_WIDTH, DOT_SIZE);
        }
        this.textWidth = this.line.width();
        this.lineHeight = this.line.height();
        return new Size(Math.max(DOTS_WIDTH, this.textWidth), this.lineHeight + DOT_GAP + DOT_SIZE);
    }

    @Override
    protected void paint(final Painter painter, final Rect bounds) {
        if (!bounds.equals(this.dotBounds)) {
            final float left = bounds.x() + (bounds.width() - DOTS_WIDTH) * 0.5f;
            final float top = bounds.bottom() - DOT_SIZE;
            for (int i = 0; i < DOT_COUNT; i++) {
                this.dots[i] = new Rect(left + i * (DOT_SIZE + DOT_GAP), top, DOT_SIZE, DOT_SIZE);
            }
            this.dotBounds = bounds;
        }
        final long step = Math.floorDiv(this.environment().millis(), FRAME_MILLIS);
        final int lit = FRAMES[(int) Math.floorMod(step, (long) FRAMES.length)];
        for (int i = 0; i < DOT_COUNT; i++) {
            (i == lit ? this.activePaint : this.dotPaint).paint(painter, this.dots[i]);
        }
        if (!this.line.isEmpty()) {
            this.line.draw(painter, bounds.x() + (bounds.width() - this.textWidth) * 0.5f,
                    bounds.bottom() - DOT_SIZE - DOT_GAP - this.lineHeight);
        }
    }
}
