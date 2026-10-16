/**
 * Themes: how components look.
 *
 * <p>A {@link org.inspirenxe.strata.api.theme.Theme} maps style keys, such as {@code button.hovered}, to
 * {@link org.inspirenxe.strata.api.theme.Paint}s and {@link org.inspirenxe.strata.api.text.TextStyle}s. Each widget
 * documents the keys it reads as constants on its interface ({@code Button.STYLE_HOVERED}). Some keys are required:
 * a host refuses a tree whose theme lacks them and lists every missing key in a
 * {@link org.inspirenxe.strata.api.theme.ThemeException}. Other keys are optional and fall back to a documented
 * default.</p>
 *
 * <p>Every component in one host shares the host's theme. Change it on the host; components re-read their keys and
 * a new layout runs.</p>
 */
package org.inspirenxe.strata.api.theme;
