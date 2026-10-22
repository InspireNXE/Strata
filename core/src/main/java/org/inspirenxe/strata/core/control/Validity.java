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
package org.inspirenxe.strata.core.control;

import org.inspirenxe.strata.api.component.Validation;
import org.inspirenxe.strata.api.control.attribute.value.Validatable;
import org.inspirenxe.strata.api.extension.Painter;
import org.inspirenxe.strata.api.geometry.Rect;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.Paint;
import org.inspirenxe.strata.api.theme.Theme;
import org.inspirenxe.strata.core.node.Node;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * The validation of one {@code Validatable} control: what code set, what its validator worked out, and a binding's
 * conversion error, which wins while it lasts; and how the control draws it.
 *
 * <p>Whenever the validation the control shows changes, this puts its message, if it has a severity, on the
 * control's node as the {@linkplain Node#setTooltipNotice tooltip notice}, and calls the control back so it can
 * report {@code VALIDATED}.</p>
 *
 * <p>The look: with an error, {@link #face} swaps the control's face for its theme's invalid face when the theme has
 * one; otherwise, and for a warning, {@link #paintOutline} draws a one-unit outline inside the bounds in
 * {@value #STYLE_ERROR} or {@value #STYLE_WARNING}. The outline's four rectangles are kept until the bounds change,
 * so a frame allocates nothing.</p>
 */
public final class Validity {
    /**
     * Style key for the outline of a control with an error and no invalid face (paint, optional).
     */
    public static final String STYLE_ERROR = Validatable.STYLE_ERROR;
    /**
     * Style key for the outline of a control with a warning (paint, optional).
     */
    public static final String STYLE_WARNING = Validatable.STYLE_WARNING;
    /**
     * The outline of an error when the theme has no {@value #STYLE_ERROR}: a solid red.
     */
    public static final Paint DEFAULT_ERROR = Paint.solid(0xFFE04848);
    /**
     * The outline of a warning when the theme has no {@value #STYLE_WARNING}: a solid amber.
     */
    public static final Paint DEFAULT_WARNING = Paint.solid(0xFFE0A030);

    private final Node node;
    private final BiConsumer<Validation, Validation> changed;
    private Validation set;
    private Text conversion;
    private Supplier<Validation> validator;
    private Validation shown;

    private Paint invalidFace;
    private Paint errorOutline;
    private Paint warningOutline;
    private Rect outlineFor;
    private final Rect[] outline;

    /**
     * Creates a valid validity.
     *
     * @param node the control's node, which shows the message in its tooltip
     * @param changed called with the new validation and the one before after each change, to report it
     */
    public Validity(final Node node, final BiConsumer<Validation, Validation> changed) {
        this.node = Objects.requireNonNull(node, "node");
        this.changed = Objects.requireNonNull(changed, "changed");
        this.set = Validation.VALID;
        this.shown = Validation.VALID;
        this.errorOutline = DEFAULT_ERROR;
        this.warningOutline = DEFAULT_WARNING;
        this.outline = new Rect[4];
    }

    /**
     * Returns the validation the control shows.
     *
     * @return the conversion error if there is one, else what was set or worked out last
     */
    public Validation validation() {
        return this.shown;
    }

    /**
     * Sets the validation from code.
     *
     * @param validation the validation
     */
    public void set(final Validation validation) {
        this.set = Objects.requireNonNull(validation, "validation");
        this.update();
    }

    /**
     * Sets the validator and asks it at once, or removes it.
     *
     * @param validator works out the validation from the control, or {@code null} for none
     */
    public void setValidator(final Supplier<Validation> validator) {
        this.validator = validator;
        this.check();
    }

    /**
     * Asks the validator again, after the control's value changed and was reported. Does nothing without one.
     *
     * @throws NullPointerException if the validator returns null
     */
    public void check() {
        final Supplier<Validation> validator = this.validator;
        if (validator != null) {
            this.set = Objects.requireNonNull(validator.get(), "The validator returned null");
            this.update();
        }
    }

    /**
     * Shows or clears a binding's conversion error.
     *
     * @param message why the value does not convert, or {@code null} once it does
     */
    public void setConversionError(final Text message) {
        this.conversion = message;
        this.update();
    }

    private void update() {
        final Validation next = this.conversion != null ? Validation.error(this.conversion) : this.set;
        if (next.equals(this.shown)) {
            return;
        }
        final Validation previous = this.shown;
        this.shown = next;
        this.node.setTooltipNotice(next.severity() == Validation.Severity.NONE || next.message().isEmpty() ? null
                : next.message());
        this.changed.accept(next, previous);
    }

    /**
     * Reads the theme's paints.
     *
     * @param theme the control's theme
     * @param invalidKey the key of the control's invalid face, or {@code null} if it has none
     */
    public void applyTheme(final Theme theme, final String invalidKey) {
        this.invalidFace = invalidKey == null ? null : theme.paint(invalidKey).orElse(null);
        this.errorOutline = theme.paintOr(STYLE_ERROR, DEFAULT_ERROR);
        this.warningOutline = theme.paintOr(STYLE_WARNING, DEFAULT_WARNING);
    }

    /**
     * Returns the face to draw: the theme's invalid face while there is an error and the theme has one, else the
     * face given.
     *
     * @param normal the face the control would draw
     * @return the face to draw
     */
    public Paint face(final Paint normal) {
        return this.invalidFace != null && this.shown.severity() == Validation.Severity.ERROR ? this.invalidFace : normal;
    }

    /**
     * Draws the outline for an error the invalid face did not show, or for a warning. Does nothing while valid.
     *
     * @param painter the control's painter
     * @param bounds the rectangle to outline from the inside
     */
    public void paintOutline(final Painter painter, final Rect bounds) {
        final Paint paint = switch (this.shown.severity()) {
            case NONE -> null;
            case WARNING -> this.warningOutline;
            case ERROR -> this.invalidFace != null ? null : this.errorOutline;
        };
        if (paint == null || bounds.width() <= 0f || bounds.height() <= 0f) {
            return;
        }
        if (!bounds.equals(this.outlineFor)) {
            this.outlineFor = bounds;
            final float side = Math.max(0f, bounds.height() - 2f);
            this.outline[0] = new Rect(bounds.x(), bounds.y(), bounds.width(), 1f);
            this.outline[1] = new Rect(bounds.x(), bounds.bottom() - 1f, bounds.width(), 1f);
            this.outline[2] = new Rect(bounds.x(), bounds.y() + 1f, 1f, side);
            this.outline[3] = new Rect(bounds.right() - 1f, bounds.y() + 1f, 1f, side);
        }
        for (final Rect rect : this.outline) {
            paint.paint(painter, rect);
        }
    }
}
