package org.inspirenxe.strata.api.screen;

import org.inspirenxe.strata.api.ComponentFactory;
import org.inspirenxe.strata.api.component.Component;

/**
 * One full screen of UI. Implement it to describe what the screen shows; a host calls it.
 *
 * <h2>Lifecycle</h2>
 * <ol>
 *     <li>The host calls {@link #build} for the component tree and shows it, sized to the host.</li>
 *     <li>The host calls {@link #opened} with a handle the screen may keep.</li>
 *     <li>After {@link ScreenHandle#invalidate()}, the host calls {@link #build} again before the next frame and
 *     replaces the whole tree. Focus and hover start over.</li>
 *     <li>When the screen closes, the host calls {@link #closed} and then detaches the tree.</li>
 * </ol>
 * <p>A screen object may be opened again after it closes; each opening builds a new tree.</p>
 *
 * <h2>Requirements for implementors</h2>
 * <ul>
 *     <li>{@link #build} returns a new component that has no parent, made with the factory passed in. Returning a
 *     component from another factory, or one already shown elsewhere, fails with
 *     {@link IllegalArgumentException}.</li>
 *     <li>The host's theme must define every required style key of the tree's components, or opening fails with
 *     {@link org.inspirenxe.strata.api.theme.ThemeException}.</li>
 *     <li>Make the root fill the host with {@link org.inspirenxe.strata.api.layout.Anchor#FILL} if it should cover
 *     the whole area; otherwise it takes its desired size at the top left.</li>
 *     <li>All calls happen on the UI thread.</li>
 * </ul>
 *
 * <pre>{@code
 * public final class SettingsScreen implements Screen {
 *     @Override
 *     public Component<?> build(ComponentFactory factory) {
 *         Label status = factory.label("Ready");
 *         Button save = factory.button("Save").onAction(() -> status.setText("Saved"));
 *         StackPanel column = factory.stackPanel().setSpacing(4).setPadding(8).add(status).add(save);
 *         return factory.anchorPanel().add(column.setAnchor(Anchor.CENTER)).setAnchor(Anchor.FILL);
 *     }
 * }
 * }</pre>
 */
@FunctionalInterface
public interface Screen {

    /**
     * Creates the screen's component tree.
     *
     * @param factory makes the components; belongs to the host showing this screen
     * @return the root component, new and without a parent
     */
    Component<?> build(ComponentFactory factory);

    /**
     * Called once the screen is open and its first tree is built. The default does nothing.
     *
     * @param handle lets the screen ask the host for a rebuild or to close; valid until the screen closes, after
     *               which its methods do nothing
     */
    default void opened(final ScreenHandle handle) {
    }

    /**
     * Called when the screen closes, before its tree is detached. The default does nothing.
     */
    default void closed() {
    }

    /**
     * Returns whether this screen covers everything beneath it in a stack of screens. When true, a stack draws only
     * from this screen up, skipping the screens below. The default is false: screens below show through.
     *
     * @return true if screens below need not be drawn
     */
    default boolean hidesBackground() {
        return false;
    }
}
