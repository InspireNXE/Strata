package org.inspirenxe.strata.api.theme;

import java.util.Objects;

/**
 * Names a sprite as {@code namespace:path}. Each adapter decides where the image lives; the Minecraft adapter maps
 * it to the gui atlas and the Swing adapter to {@code assets/<namespace>/textures/gui/sprites/<path>.png}.
 */
public record SpriteId(String namespace, String path) {
    public SpriteId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Sprite id needs a namespace and a path: " + namespace + ":" + path);
        }
    }

    public static SpriteId of(final String namespace, final String path) {
        return new SpriteId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}. A missing namespace is an error, so sprites never silently land in someone
     * else's namespace.
     */
    public static SpriteId parse(final String id) {
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Sprite id needs a namespace: " + id);
        }
        return new SpriteId(id.substring(0, colon), id.substring(colon + 1));
    }

    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }
}
