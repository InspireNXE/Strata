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
package org.inspirenxe.strata.api.control.display;

import org.inspirenxe.strata.api.component.Component;
import org.inspirenxe.strata.api.component.HasCursor;
import org.inspirenxe.strata.api.component.HasTooltip;
import org.inspirenxe.strata.api.state.PropertyKey;
import org.inspirenxe.strata.api.text.Text;
import org.inspirenxe.strata.api.theme.SpriteId;
import org.inspirenxe.strata.api.visual.Visual;

import java.util.Objects;
import java.util.Optional;

/**
 * Who someone or something is, in a small square: their picture, or else their initials, or else a placeholder
 * figure, with an optional badge for a count or a status. It fits a person, a team, a group, a bot or a server.
 *
 * <p>What the square shows, the first that applies:</p>
 * <ol>
 *     <li>A sprite or visual set with {@link #setSprite} or {@link #setVisual}, stretched over the square.</li>
 *     <li>The icon of the {@linkplain #setValue value}'s {@linkplain org.inspirenxe.strata.api.appearance.Appearances
 *     appearance}, so an engine adapter that registers an appearance for its player or account type makes
 *     {@code avatar.setValue(account)} show that face with no more code.</li>
 *     <li>The {@linkplain #initials() initials}: those set with {@link #setInitials}, else worked out from the
 *     {@linkplain #setName name}, else from the value's name. They draw centered in the {@value #STYLE_INITIALS}
 *     style, scaled by a whole number so a line fills at most two fifths of the square, and never below 1, which
 *     keeps bitmap fonts crisp.</li>
 *     <li>The theme's {@value #STYLE_PLACEHOLDER}, or {@value #STYLE_PLACEHOLDER_GROUP} for a
 *     {@linkplain #setGroup group}, when it has one; otherwise only the background.</li>
 * </ol>
 *
 * <p>The square is {@link #avatarSize()} on a side, which is also the desired size. In bounds of another shape, the
 * avatar draws the largest square that fits, centered. {@value #STYLE_BACKGROUND} draws behind everything and
 * {@value #STYLE_FRAME} over the picture, so a theme can round the corners with a frame sprite whose corners are the
 * color of what is behind. The badge sits at the square's bottom-right corner: a number a line tall with
 * {@value #STYLE_BADGE_PADDING} around it, or a {@linkplain #setBadgeVisual badge visual} in a square a third of
 * the size.</p>
 *
 * <p>An avatar has no input of its own. It is not focusable, has no default behavior for input and fires no events,
 * so clicks pass through to its ancestors. Its tooltip is the value's tooltip, asked anew each time the pointer
 * arrives, until one is set with {@code setTooltip}. A clickable avatar is a
 * {@link org.inspirenxe.strata.api.control.button.Button} with the avatar's visual as its icon, or an avatar inside
 * a component that handles clicks. A disabled avatar draws the same as an enabled one.</p>
 *
 * <h2>Theme keys</h2>
 * <ul>
 *     <li>{@value #STYLE_BACKGROUND} (paint, required): behind the square.</li>
 *     <li>{@value #STYLE_FRAME} (paint, optional): over the picture, the initials or the placeholder. Nothing is
 *     drawn if it is missing.</li>
 *     <li>{@value #STYLE_INITIALS} (text style, optional): the initials. Falls back to the default text style.</li>
 *     <li>{@value #STYLE_PLACEHOLDER} (paint, optional): the figure shown with no picture and no initials. Nothing
 *     is drawn if it is missing.</li>
 *     <li>{@value #STYLE_PLACEHOLDER_GROUP} (paint, optional): the same for a group. Falls back to
 *     {@value #STYLE_PLACEHOLDER}.</li>
 *     <li>{@value #STYLE_BADGE} (paint, optional): behind the badge's number. Falls back to a red fill.</li>
 *     <li>{@value #STYLE_BADGE_TEXT} (text style, optional): the badge's number. Falls back to the default text
 *     style.</li>
 *     <li>{@value #STYLE_BADGE_PADDING} (insets, optional): around the badge's number. Defaults to 2 at the sides
 *     and 1 at the top and bottom.</li>
 * </ul>
 *
 * <p>The three {@code badge.*} keys carry no avatar prefix, since the items of a
 * {@link org.inspirenxe.strata.api.control.pane.NavigationView} read them too, so counts look the same
 * everywhere.</p>
 *
 * <pre>{@code
 * Avatar seller = factory.avatar(account).setAvatarSize(32);
 * Avatar team = factory.avatar().setName("Cleo Dev").setGroup(true).setBadge(3);
 * }</pre>
 *
 * <p>Get one from {@link org.inspirenxe.strata.api.ComponentFactory#avatar()}. Do not implement this
 * interface.</p>
 */
public interface Avatar extends Component<Avatar>, HasCursor<Avatar>, HasTooltip<Avatar> {
    /**
     * Style key for behind the square (paint, required).
     */
    String STYLE_BACKGROUND = "avatar.background";
    /**
     * Style key for over the picture, the initials or the placeholder (paint, optional).
     */
    String STYLE_FRAME = "avatar.frame";
    /**
     * Style key for the initials (text style, optional).
     */
    String STYLE_INITIALS = "avatar.initials";
    /**
     * Style key for the figure shown with no picture and no initials (paint, optional).
     */
    String STYLE_PLACEHOLDER = "avatar.placeholder";
    /**
     * Style key for the figure a group shows with no picture and no initials (paint, optional). Falls back to
     * {@value #STYLE_PLACEHOLDER}.
     */
    String STYLE_PLACEHOLDER_GROUP = "avatar.placeholder.group";
    /**
     * Style key for behind a badge's number (paint, optional). Falls back to a red fill. Navigation items read it too.
     */
    String STYLE_BADGE = "badge.background";
    /**
     * Style key for a badge's number (text style, optional). Navigation items read it too.
     */
    String STYLE_BADGE_TEXT = "badge.text";
    /**
     * Style key for the space around a badge's number (insets, optional). Defaults to 2 at the sides and 1 at the
     * top and bottom. Navigation items read it too.
     */
    String STYLE_BADGE_PADDING = "badge.padding";
    /**
     * The side of the square unless {@link #setAvatarSize} says otherwise, in UI units.
     */
    float DEFAULT_SIZE = 24f;
    /**
     * The largest count a badge shows as it is. A larger count shows as this number followed by {@code +}.
     */
    int MAX_BADGE = 99;
    /**
     * The value shown, as {@link #value()} and {@link #setValue} say, null for none. Binds one way.
     */
    PropertyKey<Avatar, Object> VALUE = PropertyKey.of("avatar.value", Avatar::value, Avatar::setValue);
    /**
     * The name the initials are worked out from, as {@link #name()} and {@link #setName(Text)} say. Binds one way.
     * Null or {@link Text#empty()} goes back to the value's name.
     */
    PropertyKey<Avatar, Text> NAME = PropertyKey.of("avatar.name", Avatar::name, Avatar::setName);
    /**
     * The count the badge shows, as {@link #badge()} and {@link #setBadge} say. Binds one way. A negative count
     * throws as {@code setBadge} does.
     */
    PropertyKey<Avatar, Integer> BADGE = PropertyKey.of("avatar.badge", Avatar::badge, Avatar::setBadge);

    /**
     * Returns the value shown.
     *
     * @return the value set with {@link #setValue}, or {@code null} if none is
     */
    Object value();

    /**
     * Shows a value through its {@linkplain org.inspirenxe.strata.api.appearance.Appearances appearance}: its icon
     * as the picture unless a sprite or visual is set, its name for the initials unless a name or initials are set,
     * and its tooltip, asked anew each time the pointer arrives. Setting a tooltip afterwards replaces the value's
     * until the next {@code setValue}. The icon and the name are read when the value is set. A value with no
     * appearance has no icon and no tooltip, and its name is {@link String#valueOf(Object)}, so
     * {@code setValue("Ana Lima")} shows {@code AL}.
     *
     * @param value the value, or {@code null} for none
     * @return this avatar
     */
    Avatar setValue(Object value);

    /**
     * Returns the sprite set in code.
     *
     * @return the sprite, or {@code null} while none is set
     */
    SpriteId sprite();

    /**
     * Shows a sprite as the picture, stretched over the square, replacing the sprite or visual set before.
     *
     * @param sprite the sprite, or {@code null} to clear the picture set in code
     * @return this avatar
     */
    Avatar setSprite(SpriteId sprite);

    /**
     * Returns the visual set in code.
     *
     * @return the visual, or {@code null} while none is set
     */
    Visual visual();

    /**
     * Shows an engine visual as the picture, stretched over the square, replacing the sprite or visual set before.
     * The avatar keeps its size.
     *
     * @param visual the visual, or {@code null} to clear the picture set in code
     * @return this avatar
     */
    Avatar setVisual(Visual visual);

    /**
     * Returns the name set in code.
     *
     * @return the name, or {@link Text#empty()} while none is set
     */
    Text name();

    /**
     * Sets the name the initials are worked out from, in place of the value's name. The avatar lays out again.
     *
     * @param name the name, or {@code null} or {@link Text#empty()} to go back to the value's name
     * @return this avatar
     */
    Avatar setName(Text name);

    /**
     * Sets the name with literal text, as {@link #setName(Text)} does.
     *
     * @param name the name, or {@code null} or an empty string to go back to the value's name
     * @return this avatar
     */
    default Avatar setName(final String name) {
        return this.setName(name == null ? null : Text.of(name));
    }

    /**
     * Returns the initials the avatar shows when it has no picture.
     *
     * <p>They are those set with {@link #setInitials}, or else worked out from the name set in code, or else from
     * the value's name: the first letter of the first word and the first letter of the last word, upper cased for
     * the language's locale. A word is a run of characters between spaces, and words with no letter are skipped. So
     * Ana Lima gives {@code AL}, Cleo gives {@code C}, and a name with no letters gives none. Characters are whole
     * code points. The text is read as it is now, so translated names follow the language.</p>
     *
     * @return the initials; empty when there are none
     */
    String initials();

    /**
     * Sets the initials shown when the avatar has no picture, in place of those worked out from the name. The
     * avatar lays out again.
     *
     * @param initials the initials, an empty string to show none, or {@code null} to work them out from the name
     *     again
     * @return this avatar
     */
    Avatar setInitials(String initials);

    /**
     * Returns whether the avatar stands for a group.
     *
     * @return false by default
     */
    boolean isGroup();

    /**
     * Sets whether the avatar stands for a group, which only changes the placeholder: a group draws
     * {@value #STYLE_PLACEHOLDER_GROUP}.
     *
     * @param group true for a group
     * @return this avatar
     */
    Avatar setGroup(boolean group);

    /**
     * Returns the side of the square, in UI units.
     *
     * @return the size; {@link #DEFAULT_SIZE} by default
     */
    float avatarSize();

    /**
     * Sets the side of the square, which is also the desired width and height. The avatar lays out again.
     *
     * @param size the side in UI units
     * @return this avatar
     * @throws IllegalArgumentException if the size is negative or not finite
     */
    Avatar setAvatarSize(float size);

    /**
     * Returns the count the badge shows.
     *
     * @return the count; 0 by default
     */
    int badge();

    /**
     * Sets the count the badge shows. 0 hides the badge, and a count above {@link #MAX_BADGE} shows as
     * {@code 99+}. A {@linkplain #setBadgeVisual badge visual} takes the number's place while it is set.
     *
     * @param count the count
     * @return this avatar
     * @throws IllegalArgumentException if the count is negative
     */
    Avatar setBadge(int count);

    /**
     * Returns the visual shown as the badge.
     *
     * @return the visual, or empty while none is set
     */
    Optional<Visual> badgeVisual();

    /**
     * Shows a visual as the badge, such as a status dot, in place of the number. It is drawn in a square a third of
     * the avatar's size at the bottom-right corner, whatever the count.
     *
     * @param visual the visual, or {@code null} to clear it
     * @return this avatar
     */
    Avatar setBadgeVisual(Visual visual);

    /**
     * An avatar's own state in a {@link org.inspirenxe.strata.api.component.ComponentSnapshot}.
     *
     * @param value the {@linkplain #value() value}, or null
     * @param name the {@linkplain #name() name set in code}; never null
     * @param initials the {@linkplain #initials() initials shown}; never null
     * @param group whether it {@linkplain #isGroup() stands for a group}
     * @param badge the {@linkplain #badge() badge count}
     */
    record State(Object value, Text name, String initials, boolean group, int badge) {

        /**
         * Checks the name and the initials.
         */
        public State {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(initials, "initials");
        }
    }
}
