package org.inspirenxe.strata.api.theme;

import java.util.Objects;

/**
 * Names a sprite as {@code namespace:path}. The engine adapter decides where the image comes from:
 * <ul>
 *     <li>Minecraft: the {@code minecraft:gui} atlas, which reads {@code assets/<namespace>/textures/gui/sprites/
 *     <path>.png}. The sprite's {@code .png.mcmeta} controls nine-slice scaling.</li>
 *     <li>Swing: the classpath resource {@code assets/<namespace>/textures/gui/sprites/<path>.png}, the same layout,
 *     so one set of images serves both. A missing image draws as magenta.</li>
 * </ul>
 *
 * @param namespace the owner, usually a mod id; not empty
 * @param path the path inside the namespace, without extension; not empty
 */
public record SpriteId(String namespace, String path) {
    /**
     * Checks the parts.
     *
     * @throws NullPointerException if either part is null
     * @throws IllegalArgumentException if either part is empty
     */
    public SpriteId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (namespace.isEmpty() || path.isEmpty()) {
            throw new IllegalArgumentException("Sprite id needs a namespace and a path: " + namespace + ":" + path);
        }
    }

    /**
     * Creates a sprite id.
     *
     * @param namespace the owner, usually a mod id
     * @param path the path inside the namespace
     * @return the sprite id
     */
    public static SpriteId of(final String namespace, final String path) {
        return new SpriteId(namespace, path);
    }

    /**
     * Parses {@code namespace:path}. Unlike Minecraft's identifiers, a missing namespace is an error, so a sprite
     * never lands in another mod's namespace by accident.
     *
     * @param id the text to parse
     * @return the sprite id
     * @throws IllegalArgumentException if there is no colon, or either side of it is empty
     */
    public static SpriteId parse(final String id) {
        final int colon = id.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("Sprite id needs a namespace: " + id);
        }
        return new SpriteId(id.substring(0, colon), id.substring(colon + 1));
    }

    /**
     * Returns the id as {@code namespace:path}, the form {@link #parse} accepts.
     *
     * @return the id as text
     */
    @Override
    public String toString() {
        return this.namespace + ":" + this.path;
    }
}
