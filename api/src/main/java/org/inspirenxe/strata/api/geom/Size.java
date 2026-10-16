package org.inspirenxe.strata.api.geom;

public record Size(float width, float height) {
    public static final Size ZERO = new Size(0, 0);
}
